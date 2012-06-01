package com.heroku;

import com.google.common.base.Joiner;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.herokuapp.directto.client.DirectToHerokuClient;
import com.herokuapp.directto.client.VerificationException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public abstract class AbstractArtifactDeployment extends AbstractHerokuBuildStep {

    protected final Map<String, String> artifactPaths;

    protected AbstractArtifactDeployment(String apiKey, String appName, Map<String, String> artifactPaths) {
        super(apiKey, appName);
        this.artifactPaths = artifactPaths;
    }

    @Override
    public final boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, final HerokuAPI api, final App app) {
        listener.getLogger().println("Preparing to deploy " + getDescriptor().getPipelineDisplayName() + " to " + app.getName());

        final boolean result;
        try {
            result = build.getWorkspace().act(new FilePath.FileCallable<Boolean>() {
                public Boolean invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
                    final DirectToHerokuClient client = new DirectToHerokuClient.Builder()
                            .setApiKey(getEffectiveApiKey())
                            .setConsumersUserAgent(new JenkinsUserAgentValueProvider().getLocalUserAgent())
                            .build();

                    final Map<String, File> artifacts = new HashMap<String, File>(artifactPaths.size());
                    for (Map.Entry<String, String> artifactPath : artifactPaths.entrySet()) {
                        listener.getLogger().println("Adding " + artifactPath.getKey() + " => " + artifactPath.getValue());
                        artifacts.put(artifactPath.getKey(), new File(workspace + File.separator + artifactPath.getValue()));
                    }

                    try {
                        client.verify(getDescriptor().getPipelineName(), app.getName(), artifacts);
                    } catch (VerificationException e) {
                        for (String err : e.getMessages()) {
                            listener.error(err);
                        }
                        return false;
                    }

                    listener.getLogger().println("Deploying...");
                    final Map<String, String> deployResults = client.deploy(getDescriptor().getPipelineName(), app.getName(), artifacts);
                    for (Map.Entry<String, String> result : deployResults.entrySet()) {
                        listener.getLogger().println(result.getKey() + ":" + result.getValue());
                    }

                    listener.getLogger().println("Deployment successful: " + app.getWebUrl());
                    return true;
                }
            });
        } catch (IOException e) {
            listener.error(e.getMessage());
            e.printStackTrace(listener.getLogger());
            return false;
        } catch (InterruptedException e) {
            listener.error(e.getMessage());
            e.printStackTrace(listener.getLogger());
            return false;
        }

        return result;
    }

    @Override
    public AbstractArtifactDeploymentDescriptor getDescriptor() {
        return (AbstractArtifactDeploymentDescriptor) super.getDescriptor();
    }

    public static abstract class AbstractArtifactDeploymentDescriptor extends AbstractHerokuBuildStepDescriptor {

        public abstract String getPipelineName();

        public String getDisplayName() {
            return "Heroku: Deploy " + getPipelineDisplayName() + " Artifact";
        }

        protected String getPipelineDisplayName() {
            return getPipelineName().toUpperCase();
        }

        public FormValidation checkAnyArtifactPath(AbstractProject project, String value, String... validFileEndings) throws IOException {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.validateRequired(value);
            }

            final FormValidation endingValidation = validateFileEndings(value, validFileEndings);
            if (endingValidation.kind != FormValidation.Kind.OK) {
                return endingValidation;
            }

            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        private FormValidation validateFileEndings(String value, String... validFileEndings) {
            if (validFileEndings.length == 0) {
                return FormValidation.ok();
            }

            for (String ending : validFileEndings) {
                if (value.endsWith(ending)) {
                    return FormValidation.ok();
                }
            }

            return FormValidation.error("File must end with " + Joiner.on(" or ").join(validFileEndings));
        }
    }
}


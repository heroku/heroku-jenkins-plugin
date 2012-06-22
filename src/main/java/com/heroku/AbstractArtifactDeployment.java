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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public abstract class AbstractArtifactDeployment extends AbstractHerokuBuildStep {

    protected static final String TARGZ_PIPELINE = "targz";
    protected static final String WAR_PIPELINE = "war";

    protected static final String WAR_PATH = "war";
    protected static final String TARGZ_PATH = "targz";
    protected static final String PROCFILE_PATH = "procfile";

    protected final Map<String, String> artifactPaths;

    protected AbstractArtifactDeployment(String apiKey, String appName, Map<String, String> artifactPaths) {
        super(apiKey, appName);
        this.artifactPaths = artifactPaths;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, final HerokuAPI api, final App app) {
        try {
            return build.getWorkspace().act(new RemoteCallable(
                    listener,
                    getEffectiveApiKey(),
                    app.getName(),
                    app.getWebUrl(),
                    new JenkinsUserAgentValueProvider().getLocalUserAgent(),
                    getDescriptor().getPipelineName(),
                    getDescriptor().getPipelineDisplayName(),
                    artifactPaths));
        } catch (IOException e) {
            listener.error(e.getMessage());
            e.printStackTrace(listener.getLogger());
            return false;
        } catch (InterruptedException e) {
            listener.error(e.getMessage());
            e.printStackTrace(listener.getLogger());
            return false;
        }
    }

    @Override
    public AbstractArtifactDeploymentDescriptor getDescriptor() {
        return (AbstractArtifactDeploymentDescriptor) super.getDescriptor();
    }

    /**
     * A serializable, immutable payload for the deployment task.
     * Separated from containing class and environment to allow it to be run on remote slaves without trying to serialize the world.
     */
    public static class RemoteCallable implements FilePath.FileCallable<Boolean>, Serializable {

        private final BuildListener listener;
        private final String apiKey;
        private final String appName;
        private final String appWebUrl;
        private final String userAgent;
        private final String pipelineName;
        private final String pipelineDisplayName;
        private final Map<String, String> artifactPaths;

        RemoteCallable(BuildListener listener, String apiKey, String appName, String appWebUrl, String userAgent, String pipelineName, String pipelineDisplayName, Map<String, String> artifactPaths) {
            this.listener = listener;
            this.apiKey = apiKey;
            this.appName = appName;
            this.appWebUrl = appWebUrl;
            this.pipelineName = pipelineName;
            this.userAgent = userAgent;
            this.pipelineDisplayName = pipelineDisplayName;
            this.artifactPaths = artifactPaths;
        }

        public Boolean invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
            listener.getLogger().println("Preparing to deploy " + pipelineDisplayName + " to app " + appName);

            final DirectToHerokuClient client = new DirectToHerokuClient.Builder()
                    .setApiKey(apiKey)
                    .setConsumersUserAgent(userAgent)
                    .build();

            final Map<String, File> artifacts = new HashMap<String, File>(artifactPaths.size());
            for (Map.Entry<String, String> artifactPath : artifactPaths.entrySet()) {
                artifacts.put(artifactPath.getKey(), new File(workspace + File.separator + artifactPath.getValue()));
            }

            try {
                client.verify(pipelineName, appName, artifacts);
            } catch (VerificationException e) {
                for (String err : e.getMessages()) {
                    listener.error(err);
                }
                return false;
            }

            listener.getLogger().println("Deploying...");
            final Map<String, String> deployResults = client.deploy(pipelineName, appName, artifacts);
            listener.getLogger().println("Released " + deployResults.get("release") + " to " + appWebUrl);

            return true;
        }
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


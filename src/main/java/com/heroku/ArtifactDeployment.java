package com.heroku;

import com.google.common.collect.ImmutableMap;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.herokuapp.directto.client.DirectToHerokuClient;
import com.herokuapp.directto.client.VerificationException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class ArtifactDeployment extends AbstractHerokuBuildStep {

    private final String pipelineName;
    private final Map<String, String> artifactPaths;

    @DataBoundConstructor
    public ArtifactDeployment(String apiKey, String appName, String pipelineName, String artifactPath) {
        super(apiKey, appName);
        this.pipelineName = pipelineName;
        this.artifactPaths = ImmutableMap.of("war", artifactPath);
    }

    // Overridding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getAppName() {
        return super.getAppName();
    }

    // Overridding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getApiKey() {
        return super.getApiKey();
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getArtifactPath() {
        return artifactPaths.get("war");
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, final HerokuAPI api, final App app) {
        listener.getLogger().println("Pushing " + artifactPaths + " to " + app.getName() + "...");

        final boolean result;
        try {
            result = build.getWorkspace().act(new FilePath.FileCallable<Boolean>() {
                public Boolean invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
                    final DirectToHerokuClient client = new DirectToHerokuClient(getEffectiveApiKey());

                    final Map<String, File> artifacts = new HashMap<String, File>(artifactPaths.size());
                    for (Map.Entry<String, String> artifactPath : artifactPaths.entrySet()) {
                        artifacts.put(artifactPath.getKey(), new File(workspace + File.separator + artifactPath.getValue()));
                    }

                    try {
                        client.verify(pipelineName, app.getName(), artifacts);
                    } catch (VerificationException e) {
                        for (String err : e.getMessages()) {
                            listener.error(err);
                        }
                        return false;
                    }

                    final Map<String, String> deployResults = client.deploy(pipelineName, app.getName(), artifacts);
                    for (Map.Entry<String, String> result : deployResults.entrySet()) {
                        listener.getLogger().println(result.getKey() + ":" + result.getValue());
                    }

                    listener.getLogger().println("Push successful: " + app.getWebUrl());
                    return true;
                }
            });
        } catch (IOException e) {
            listener.error(e.getMessage());
            return false;
        } catch (InterruptedException e) {
            listener.error(e.getMessage());
            return false;
        }

        return result;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends AbstractHerokuBuildStepDescriptor {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Heroku: Deploy Artifact";
        }

        public FormValidation doCheckArtifactPath(@AncestorInPath AbstractProject project, @QueryParameter String artifactPath) throws IOException {
            if (Util.fixEmptyAndTrim(artifactPath) == null) {
                return FormValidation.validateRequired(artifactPath);
            }

            if (!artifactPath.endsWith(".war")) {
                return FormValidation.error("Must be a WAR file");
            }

            return FilePath.validateFileMask(project.getSomeWorkspace(), artifactPath);
        }
    }
}


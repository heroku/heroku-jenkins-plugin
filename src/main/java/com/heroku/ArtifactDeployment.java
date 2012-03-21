package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.herokuapp.warpath.WarPusher;
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

/**
 * @author Ryan Brainard
 */
public class ArtifactDeployment extends AbstractHerokuBuildStep {

    private final String artifactPath;

    @DataBoundConstructor
    public ArtifactDeployment(String apiKey, String appName, String artifactPath) {
        super(apiKey, appName);
        this.artifactPath = artifactPath;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final String effectiveApiKey = getEffectiveApiKey();
        final HerokuAPI api = new HerokuAPI(effectiveApiKey);

        App app = getOrCreateApp(listener, api);

        listener.getLogger().println("Pushing " + artifactPath + " to " + app.getName() + "...");
        try {
            build.getWorkspace().child(artifactPath).act(new FilePath.FileCallable<Void>() {
                public Void invoke(File artifactFile, VirtualChannel channel) throws IOException, InterruptedException {
                    new WarPusher(effectiveApiKey).push(getAppName(), artifactFile);
                    return null;
                }
            });
        } catch (IOException e) {
            listener.error(e.getMessage());
            return false;
        } catch (InterruptedException e) {
            listener.error(e.getMessage());
            return false;
        }
        listener.getLogger().println("Push successful: " + app.getWebUrl());

        return true;
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
            return "Deploy to Heroku";
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


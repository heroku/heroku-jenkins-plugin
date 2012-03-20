package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
import com.herokuapp.warpath.WarPusher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public class HerokuDeployer extends Builder {

    private final String appName;
    private final String artifactPath;

    @DataBoundConstructor
    public HerokuDeployer(String appName, String artifactPath) {
        this.appName = appName;
        this.artifactPath = artifactPath;
    }

    public String getAppName() {
        return appName;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final String apiKey = getDescriptor().getApiKey(); //TODO: error handle
        final HerokuAPI api = new HerokuAPI(apiKey);

        App app;
        try {
            app = api.getApp(appName);
            listener.getLogger().println("Found existing app: " + appName);
        } catch (RequestFailedException appListingException) {
            try {
                app = api.createApp(new App().named(appName).on(Heroku.Stack.Cedar));
                listener.getLogger().println("Created new app: " + appName);
            } catch (RuntimeException appCreationException) {
                listener.error("Could not create app " + appName + "\n" +
                        appCreationException.getMessage());
                return false;
            }
        }

        listener.getLogger().println("Pushing " + artifactPath + " to " + app.getName() + "...");
        try {
            build.getWorkspace().child(artifactPath).act(new FilePath.FileCallable<Void>() {
                public Void invoke(File artifactFile, VirtualChannel channel) throws IOException, InterruptedException {
                    new WarPusher(apiKey).push(appName, artifactFile);
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
    public HerokuDeployerDescriptor getDescriptor() {
        return (HerokuDeployerDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class HerokuDeployerDescriptor extends BuildStepDescriptor<Builder> {

        private String apiKey;

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Deploy to Heroku";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            apiKey = formData.getString("apiKey");

            save();
            return super.configure(req, formData);
        }

        public String getApiKey() {
            return apiKey;
        }

        public FormValidation doCheckAppName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckArtifactPath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (value != null && !value.trim().endsWith(".war")) {
                return FormValidation.error("Artifact must be a WAR file");
            }

            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }
    }
}


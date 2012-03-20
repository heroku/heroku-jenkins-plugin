package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
import com.herokuapp.warpath.WarPusher;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
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
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
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

        final WarPusher warPusher = new WarPusher(apiKey);
        try {
            listener.getLogger().println("Pushing " + artifactPath + " to " + app.getName() + "...");
            warPusher.push(appName, new File(artifactPath));
            listener.getLogger().println("Push successful: " + app.getWebUrl());
        } catch (IOException e) {
            listener.error(e.getMessage());
            return false;
        }

        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

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
    }
}


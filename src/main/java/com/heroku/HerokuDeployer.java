package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
import com.herokuapp.warpath.WarPusher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
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

    private final String apiKey;
    private final String appName;
    private final String artifactPath;

    @DataBoundConstructor
    public HerokuDeployer(String apiKey, String appName, String artifactPath) {
        this.apiKey = apiKey;
        this.appName = appName;
        this.artifactPath = artifactPath;
    }

    public String getAppName() {
        return appName;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    private String getEffectiveApiKey() {
        if (apiKey != null && !apiKey.trim().equals("")) {
            return apiKey;
        }

        final String defaultApiKey = getDescriptor().defaultApiKey.getPlainText();
        if (defaultApiKey != null && !defaultApiKey.trim().equals("")) {
            return defaultApiKey;
        }

        throw new RuntimeException("Heroku API key not specified.");
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final String effectiveApiKey = getEffectiveApiKey();
        final HerokuAPI api = new HerokuAPI(effectiveApiKey);

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
                    new WarPusher(effectiveApiKey).push(appName, artifactFile);
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

        private Secret defaultApiKey;

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Deploy to Heroku";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            defaultApiKey = Secret.fromString(formData.getString("defaultApiKey"));

            save();
            return super.configure(req, formData);
        }

        public String getDefaultApiKey() {
            return defaultApiKey.getEncryptedValue();
        }

        public FormValidation doCheckApiKey(@QueryParameter String apiKey) {
            if (Util.fixEmptyAndTrim(apiKey) != null && Util.fixEmptyAndTrim(defaultApiKey.getPlainText()) != null) {
                return FormValidation.warning("This key will override the default key. Set to blank to use default key.");
            }

            if (Util.fixEmptyAndTrim(apiKey) != null) {
                return FormValidation.ok();
            }

            if (Util.fixEmptyAndTrim(defaultApiKey.getPlainText()) != null) {
                return FormValidation.ok("Default API key will be used.");
            }

            return FormValidation.validateRequired(apiKey);
        }

        public FormValidation doCheckAppName(@QueryParameter String appName) {
            return FormValidation.validateRequired(appName);
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


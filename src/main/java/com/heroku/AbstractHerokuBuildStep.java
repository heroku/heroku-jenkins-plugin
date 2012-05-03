package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Ryan Brainard
 */
abstract class AbstractHerokuBuildStep extends Builder {

    private final Secret apiKey;
    private final String appName;

    @DataBoundConstructor
    AbstractHerokuBuildStep(String apiKey, String appName) {
        this.apiKey = Secret.fromString(apiKey);
        this.appName = appName;
    }

    // Must override and delegate back to this method if using in config.jelly for a concrete task
    public String getAppName() {
        return appName;
    }

    /**
     * Not for use by tasks needing the API key.
     * You probably want to use {@link #getEffectiveApiKey()} instead.
     *
     * @return API key explicitly set for this build step
     */
    // Must override and delegate back to this method if using in config.jelly for a concrete task
    public String getApiKey() {
        return "".equals(Secret.toString(apiKey)) ? "" : apiKey.getEncryptedValue();
    }

    /**
     * If an API key is not explicitly defined for this specific build step,
     * the default key from {@link HerokuPlugin} is used.
     *
     * @return API key tasks should use
     */
    protected String getEffectiveApiKey() {
        final String apiKeyPlainText = apiKey.getPlainText();
        if (apiKeyPlainText != null && !apiKeyPlainText.trim().equals("")) {
            return apiKeyPlainText;
        }

        final String defaultApiKeyPlainText = HerokuPlugin.get().getDefaultApiKeyPlainText();
        if (defaultApiKeyPlainText != null && !defaultApiKeyPlainText.trim().equals("")) {
            return defaultApiKeyPlainText;
        }

        throw new RuntimeException("Heroku API key not specified.");
    }

    protected App getOrCreateApp(BuildListener listener, HerokuAPI api) {
        App app;

        try {
            app = api.getApp(appName);
            listener.getLogger().println("Found existing app: " + appName);
        } catch (RequestFailedException appListingException) {
            try {
                app = api.createApp(new App().named(appName).on(Heroku.Stack.Cedar));
                listener.getLogger().println("Created new app: " + appName);
            } catch (RuntimeException appCreationException) {
                listener.error("Could not create app " + appName + "\n" + appCreationException.getMessage());
                throw appCreationException;
            }
        }

        return app;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        final HerokuAPI api = new HerokuAPI(getEffectiveApiKey());
        final App app = getOrCreateApp(listener, api);
        return perform(build, launcher, listener, api, app);
    }

    /**
     * Subclasses should override this to get access to the Heroku API with the context of an app
     */
    protected boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        return super.perform(build, launcher, listener);
    }

    @Override
    public AbstractHerokuBuildStepDescriptor getDescriptor() {
        return (AbstractHerokuBuildStepDescriptor) super.getDescriptor();
    }

    public static abstract class AbstractHerokuBuildStepDescriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return HerokuPlugin.get().isEnabled(getClass().getEnclosingClass().getName());
        }

        public FormValidation doCheckApiKey(@QueryParameter String apiKey) {
            if (Util.fixEmptyAndTrim(apiKey) != null && Util.fixEmptyAndTrim(HerokuPlugin.get().getDefaultApiKeyPlainText()) != null) {
                return FormValidation.warning("This key will override the default key. Set to blank to use default key.");
            }

            if (Util.fixEmptyAndTrim(apiKey) != null) {
                return FormValidation.ok();
            }

            if (Util.fixEmptyAndTrim(HerokuPlugin.get().getDefaultApiKeyPlainText()) != null) {
                return FormValidation.ok("Default API key will be used.");
            }

            return FormValidation.validateRequired(apiKey);
        }

        public FormValidation doCheckAppName(@QueryParameter String appName) {
            return FormValidation.validateRequired(appName);
        }
    }
}


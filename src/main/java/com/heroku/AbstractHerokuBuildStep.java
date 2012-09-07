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
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * @author Ryan Brainard
 */
abstract class AbstractHerokuBuildStep extends Builder {

    private final boolean hasAppContext;
    private final Secret apiKey;
    private final String appName;

    AbstractHerokuBuildStep() {
        this.apiKey = null;
        this.appName = null;
        this.hasAppContext = false;
    }

    AbstractHerokuBuildStep(String apiKey, String appName) {
        this.apiKey = Secret.fromString(apiKey);
        this.appName = appName;
        this.hasAppContext = true;
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

        throw new HerokuJenkinsHandledException("Heroku API key not specified. \n" +
                "       This can be configured either with a global default or for individual build steps. \n" +
                "       To configure a global default API key, go to Manage Jenkins | Heroku | Default API Key. \n" +
                "       The global key will be used by Heroku build steps unless otherwise overridden. \n" +
                "       If no global default is specified, individual build steps must specify their own API keys under their respective Advanced settings. \n" +
                "       Your Heroku API key can be obtained from the Heroku account page at https://api.heroku.com/account.");
    }

    protected App getOrCreateApp(BuildListener listener, HerokuAPI api) {
        App app;

        try {
            app = api.getApp(appName);
        } catch (RequestFailedException appListingException) {
            if (appListingException.getStatusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new HerokuJenkinsHandledException("No access to Heroku app '" + appName + "'. Check API key, app name, and ensure you have access.");
            }

            try {
                app = api.createApp(new App().named(appName).on(Heroku.Stack.Cedar));
                listener.getLogger().println("Created new app " + appName);
            } catch (RequestFailedException appCreationException) {
                if (appCreationException.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new HerokuJenkinsHandledException("No access to create Heroku app '" + appName + "'. Check API key.");
                }

                listener.error("Unknown error creating app '" + appName + "'\n" + appCreationException.getMessage());
                throw appCreationException;
            }
        }

        if (app == null || app.getId() == null) {
            throw new HerokuJenkinsHandledException("Heroku app '" + appName + "' could not be found. Check API key, app name, and ensure you have access.");
        }

        return app;
    }

    @Override
    public final boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("\n=== Starting " + getDescriptor().getDisplayName() + " ===");
        try {
            final HerokuAPI api = hasAppContext ? new HerokuAPI(getEffectiveApiKey()) : null;
            final App app = hasAppContext ? getOrCreateApp(listener, api) : null;
            try {
                final boolean result = perform(build, launcher, listener, api, app);
                if (result) {
                    listener.getLogger().println("=== Completed " + getDescriptor().getDisplayName() + " ===");
                }
                return result;
            } catch (RequestFailedException e) {
                listener.error(e.getMessage());
                e.printStackTrace(listener.getLogger());
                return false;
            }
        } catch (HerokuJenkinsHandledException e) {
            listener.error(e.getMessage());
            return false;
        }
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
            return true;
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


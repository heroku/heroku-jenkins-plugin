package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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

    public String getAppName() {
        return appName;
    }

    public String getApiKey() {
        return apiKey.getEncryptedValue();
    }

    protected String getEffectiveApiKey() {
        final String apiKeyPlainText = apiKey.getPlainText();
        if (apiKeyPlainText != null && !apiKeyPlainText.trim().equals("")) {
            return apiKeyPlainText;
        }

        final String defaultApiKeyPlainText = getDescriptor().defaultApiKey.getPlainText();
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
    public AbstractHerokuBuildStepDescriptor getDescriptor() {
        return (AbstractHerokuBuildStepDescriptor) super.getDescriptor();
    }

    public static abstract class AbstractHerokuBuildStepDescriptor extends BuildStepDescriptor<Builder> {

        private Secret defaultApiKey = Secret.fromString("");

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            defaultApiKey = Secret.fromString(formData.getString("defaultApiKey"));

            save();
            return super.configure(req, formData);
        }

        public String getDefaultApiKey() {
            return "".equals(defaultApiKey.getPlainText()) ? "" : defaultApiKey.getEncryptedValue();
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
    }
}


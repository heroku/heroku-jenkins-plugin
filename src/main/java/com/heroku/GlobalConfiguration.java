package com.heroku;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ryan Brainard
 */
// TODO: this should not be a build step. find out where to inject myself
public class GlobalConfiguration extends Builder {

    /**
     * For plugin internal use
     *
     * @return plain text value
     */
    protected static String getDefaultKey() {
        return DescriptorImpl.defaultApiKey.get().getPlainText();
    }

    @Extension
    public static class DescriptorImpl extends AbstractHerokuBuildStep.AbstractHerokuBuildStepDescriptor {

        @SuppressWarnings("UnusedDeclaration")
        private static AtomicReference<Secret> defaultApiKey = new AtomicReference<Secret>(Secret.fromString(""));

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        /**
         * For Jenkin's UI
         *
         * @return encrypted value
         */
        public String getDefaultApiKey() {
            final Secret key = this.defaultApiKey.get();
            return "".equals(key.getPlainText()) ? "" : key.getEncryptedValue();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            defaultApiKey.set(Secret.fromString(formData.getString("defaultApiKey")));

            save();
            return super.configure(req, formData);
        }
    }
}

package com.heroku;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

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
        return DescriptorImpl.defaultApiKey.getPlainText();
    }

    @Extension
    public static class DescriptorImpl extends AbstractHerokuBuildStep.AbstractHerokuBuildStepDescriptor {

        private static Secret defaultApiKey = Secret.fromString("");

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
            return "".equals(defaultApiKey.getPlainText()) ? "" : defaultApiKey.getEncryptedValue();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            defaultApiKey = Secret.fromString(formData.getString("defaultApiKey"));

            save();
            return super.configure(req, formData);
        }
    }
}

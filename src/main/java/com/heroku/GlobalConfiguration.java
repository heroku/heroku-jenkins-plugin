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
// TODO: should this be a BuildStep??
public class GlobalConfiguration extends Builder {

    @Extension
    public static class DescriptorImpl extends AbstractHerokuBuildStep.AbstractHerokuBuildStepDescriptor {

        // TODO: probably shouldn't be static... it is global though :(
        protected static Secret defaultApiKey = Secret.fromString("");

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

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

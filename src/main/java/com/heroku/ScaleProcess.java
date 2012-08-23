package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public class ScaleProcess extends AbstractHerokuBuildStep {

    private final String processType;
    private final int quantity;

    @DataBoundConstructor
    public ScaleProcess(String apiKey, String appName, String processType, int quantity) {
        super(apiKey, appName);
        this.processType = processType;
        this.quantity = quantity;
    }

    // Overriding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getAppName() {
        return super.getAppName();
    }

    // Overriding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getApiKey() {
        return super.getApiKey();
    }

    public String getProcessType() {
        return processType;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        listener.getLogger().println("Scaling " + processType + " processes to " + quantity);
        api.scaleProcess(app.getName(), processType, quantity);
        return true;
    }


    @Override
    public ScaleProcessDescriptor getDescriptor() {
        return (ScaleProcessDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class ScaleProcessDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Scale Process";
        }

        public FormValidation doCheckProcessType(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckQuantity(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            FormValidation requiredness = FormValidation.validateRequired(value);
            if (requiredness.kind != FormValidation.Kind.OK) {
                return requiredness;
            }

            return FormValidation.validateNonNegativeInteger(value);
        }
    }
}
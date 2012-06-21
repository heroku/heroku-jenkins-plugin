package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.RequestFailedException;
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

    // Overridding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getAppName() {
        return super.getAppName();
    }

    // Overridding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getApiKey() {
        return super.getApiKey();
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        listener.getLogger().print("Scaling [" + processType + "] to " + quantity + "...");
        try {
            api.scaleProcess(app.getName(), processType, quantity);
        } catch (RequestFailedException e) {
            listener.error("\n" + e.getMessage());
            e.printStackTrace(listener.getLogger());
            return false;
        }
        listener.getLogger().print("done");
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
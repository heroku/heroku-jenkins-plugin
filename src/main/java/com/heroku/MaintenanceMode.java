package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public class MaintenanceMode extends AbstractHerokuBuildStep {

    private final boolean mode;

    @DataBoundConstructor
    public MaintenanceMode(String apiKey, String appName, boolean mode) {
        super(apiKey, appName);
        this.mode = mode;
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

    public boolean getMode() {
        return mode;
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        listener.getLogger().println((mode ? "Enabling" : "Disabling") + " maintenance mode for " + app.getName());
        api.setMaintenanceMode(app.getName(), mode);
        return true;
    }


    @Override
    public MaintenanceModeDescriptor getDescriptor() {
        return (MaintenanceModeDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class MaintenanceModeDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Maintenance Mode";
        }

    }
}
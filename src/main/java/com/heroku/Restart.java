package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Release;
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
import java.util.List;

/**
 * @author Ryan Brainard
 */
public class Restart extends AbstractHerokuBuildStep {

    @DataBoundConstructor
    public Restart(String apiKey, String appName) {
        super(apiKey, appName);
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

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        listener.getLogger().println("Restarting app " + app.getName() + " ...");
        api.restart(app.getName());
        listener.getLogger().println("Restart complete");

        return true;
    }


    @Override
    public RollbackDescriptor getDescriptor() {
        return (RollbackDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class RollbackDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Restart";
        }
    }
}

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
        listener.getLogger().println("Restarting " + app.getName());
        api.restart(app.getName());

        return true;
    }


    @Override
    public RestartDescriptor getDescriptor() {
        return (RestartDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class RestartDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Restart";
        }
    }
}

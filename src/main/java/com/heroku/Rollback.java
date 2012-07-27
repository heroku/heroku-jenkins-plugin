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
public class Rollback extends AbstractHerokuBuildStep {

    @DataBoundConstructor
    public Rollback(String apiKey, String appName) {
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
        final List<Release> releases = api.listReleases(app.getName());

        if (releases.size() < 2) {
            listener.error("Cannot rollback to before app was created.");
            return false;
        }

        final Release lastRelease = releases.get(releases.size() - 2);

        listener.getLogger().println("Rolling back to " + lastRelease.getName() + "(" + lastRelease.getCommit() + ") ...");
        api.rollback(app.getName(), lastRelease.getName());
        listener.getLogger().println("Rollback complete");

        return true;
    }


    @Override
    public RollbackDescriptor getDescriptor() {
        return (RollbackDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class RollbackDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Rollback";
        }
    }
}

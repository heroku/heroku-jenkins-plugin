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
    public Rollback(String apiKey, String appName, String command) {
        super(apiKey, appName);
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
        final List<Release> releases = api.listReleases(app.getName());
        final Release lastRelease = releases.get(releases.size() - 2);

        listener.getLogger().println("Rolling back to " + lastRelease.getName() + "(" + lastRelease.getCommit() + ") ...");
        api.rollback(app.getName(), lastRelease.getName());
        listener.getLogger().println("Rollback complete");

        return true;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends AbstractHerokuBuildStepDescriptor {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Heroku: Rollback";
        }

        public FormValidation doCheckCommand(@AncestorInPath AbstractProject project, @QueryParameter String command) throws IOException {
            return FormValidation.validateRequired(command);
        }
    }
}

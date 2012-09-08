package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.janvil.Janvil;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Ryan Brainard
 */
public class Release extends AbstractHerokuBuildStep {

    private static final String ANVIL_SLUG_PATH = ".anvil/slug";

    @DataBoundConstructor
    public Release(String apiKey, String appName) {
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
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, final App app) throws IOException, InterruptedException {
        listener.getLogger().println("Releasing slug to " + app.getName() + " ...");

        final String slugUrl = build.getWorkspace().child(ANVIL_SLUG_PATH).act(new FilePath.FileCallable<String>() {
            public String invoke(File slugUrlFile, VirtualChannel channel) throws IOException, InterruptedException {
                return Util.loadFile(slugUrlFile);
            }
        });

        if (Util.nullify(slugUrl) == null) {
            listener.error("Slug URL could not be found. Did you run a Remote Build first?");
            return false;
        }

        new Janvil(getEffectiveApiKey()).release(app.getName(), slugUrl, "Jenkins"); //TODO: what should the desc be?


        final List<com.heroku.api.Release> releases = api.listReleases(app.getName());
        final com.heroku.api.Release currentRelease = releases.get(releases.size() - 1);

        listener.getLogger().println("Release complete, " + currentRelease.getName() + " | " + app.getWebUrl());

        return true;
    }


    @Override
    public ReleaseDescriptor getDescriptor() {
        return (ReleaseDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class ReleaseDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Remote Release";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
        }
    }
}

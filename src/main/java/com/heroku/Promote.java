package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Release;
import com.heroku.janvil.Janvil;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

import static com.heroku.HerokuPlugin.Feature.ANVIL;

/**
 * @author Ryan Brainard
 */
public class Promote extends AbstractHerokuBuildStep {

    private String sourceAppName;
    private String envMask;

    @DataBoundConstructor
    public Promote(String apiKey, String sourceAppName, String targetAppName, String envMask) {
        super(apiKey, targetAppName);
        this.sourceAppName = sourceAppName;
        this.envMask = envMask;
    }

    // Overriding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getApiKey() {
        return super.getApiKey();
    }

    public String getSourceAppName() {
        return sourceAppName;
    }

    public String getTargetAppName() {
        return super.getAppName();
    }

    public String getEnvMask() {
        return envMask;
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, final App targetApp) throws IOException, InterruptedException {
        listener.getLogger().println("Promoting slug from " + sourceAppName + " to " + targetApp.getName() + " ...");

        final Release releaseBefore = currentRelease(api, targetApp);
        try {
            new Janvil(getEffectiveApiKey()).copy(getSourceAppName(), getTargetAppName(), new Janvil.ReleaseDescriptionBuilder() {
                public String buildDescription(String sourceAppName, String sourceReleaseName, String sourceCommit, String targetAppName) {
                    return "Promote " + sourceAppName + " " + sourceReleaseName + (sourceCommit != null ? " " + sourceCommit : "");
                }
            });
        } catch (Exception e) {
            api.rollback(targetApp.getName(), releaseBefore.getName());
            throw new RuntimeException(e);
        }

        listener.getLogger().println("Promotion complete, " + currentRelease(api, targetApp).getName() + " | " + targetApp.getWebUrl());

        return true;
    }

    private Release currentRelease(HerokuAPI api, App targetApp) {
        final List<Release> releases = api.listReleases(targetApp.getName());
        return releases.get(releases.size() - 1);
    }

    @Override
    public ReleaseDescriptor getDescriptor() {
        return (ReleaseDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class ReleaseDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Promote";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return HerokuPlugin.get().hasFeature(ANVIL);
        }
    }
}

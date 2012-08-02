package com.heroku;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.heroku.api.*;
import com.heroku.api.Release;
import com.herokuapp.janvil.Janvil;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            // TODO: merge into one step to make one release and avoid rollback stuff and race conditions
            // TODO: how to copy addons??
            api.addConfig(targetApp.getName(), maskConfig(api));
            final SlugInfo sourceSlug = getSourceSlug(api);

            listener.getLogger().println("Pulled slug" + sourceSlug.getName());

            try {
                new URL(sourceSlug.getSlugUrl());
            } catch (MalformedURLException e) {
                listener.error("Invalid slug url: " + sourceSlug.getSlugUrl());
                return false;
            }
            new Janvil(getEffectiveApiKey()).release(targetApp.getName(), sourceSlug.getSlugUrl());
        } catch (Exception e) {
            api.rollback(targetApp.getName(), releaseBefore.getName());
            throw new RuntimeException(e);
        }

        listener.getLogger().println("Promotion complete, " + currentRelease(api, targetApp).getName() + " | " + targetApp.getWebUrl());

        return true;
    }

    private SlugInfo getSourceSlug(HerokuAPI api) {
        return api.getConnection().execute(new ReleasesSlugInfo(sourceAppName), getEffectiveApiKey());
    }

    private Map<String, String> maskConfig(HerokuAPI api) {
        final Map<String,String> sourceConfig = api.listConfig(sourceAppName);
        final Set<String> envMaskMap = MappingConverter.convert(envMask).keySet();
        return Maps.filterKeys(sourceConfig, new Predicate<String>() {
            public boolean apply(String input) {
                return envMaskMap.contains(input);
            }
        });
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
    }
}

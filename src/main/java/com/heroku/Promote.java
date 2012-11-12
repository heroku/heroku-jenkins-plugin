package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.janvil.Config;
import com.heroku.janvil.EventSubscription;
import com.heroku.janvil.Janvil;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

import static com.heroku.HerokuPlugin.Feature.CISAURUS;
import static com.heroku.janvil.Janvil.Event;
import static com.heroku.janvil.Janvil.Event.POLLING;
import static com.heroku.janvil.Janvil.Event.PROMOTE_END;

/**
 * @author Ryan Brainard
 */
public class Promote extends AbstractHerokuBuildStep {

    private String sourceAppName;

    @DataBoundConstructor
    public Promote(String apiKey, String sourceAppName, String targetAppName) {
        super(apiKey, targetAppName);
        this.sourceAppName = sourceAppName;
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

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, final BuildListener listener, HerokuAPI api, final App targetApp) throws IOException, InterruptedException {
        final Janvil janvil = new Janvil(new Config(getEffectiveApiKey()).
            setEventSubscription(new EventSubscription<Janvil.Event>(Event.class)
                .subscribe(PROMOTE_END, new EventSubscription.Subscriber<Janvil.Event>() {
                    public void handle(Event event, Object version) {
                        listener.getLogger().println("Done, " + version + " | " + targetApp.getWebUrl());
                    }
                })));

        final List<String> downstreams = janvil.downstreams(getSourceAppName());
        if (downstreams.isEmpty()) {
            listener.getLogger().println("Adding " + targetApp + " as downstream app...");
            janvil.addDownstream(getSourceAppName(), getTargetAppName());
        } else if (!downstreams.get(0).equals(getTargetAppName())) {
            listener.error(getSourceAppName() + " already has " + downstreams.get(0) + " configured as its downstream app");
            return false;
        }

        listener.getLogger().println("Promoting " + getSourceAppName() + " to " + targetApp.getName() + " ...");
        janvil.promote(getSourceAppName());

        return true;
    }

    @Override
    public PromoteDescriptor getDescriptor() {
        return (PromoteDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class PromoteDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Promote";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return HerokuPlugin.get().hasFeature(CISAURUS);
        }
    }
}

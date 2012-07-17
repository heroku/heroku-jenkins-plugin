package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.herokuapp.janvil.Config;
import com.herokuapp.janvil.EventSubscription;
import com.herokuapp.janvil.Janvil;
import com.herokuapp.janvil.Manifest;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import static com.herokuapp.janvil.EventSubscription.Subscriber;
import static com.herokuapp.janvil.Janvil.Event;

/**
 * @author Ryan Brainard
 */
public class AnvilDeployment extends AbstractHerokuBuildStep {

    private final String globIncludes;
    private final String globExcludes;
    private final String buildpack;

    @DataBoundConstructor
    public AnvilDeployment(String apiKey, String appName, String globIncludes, String globExcludes, String buildpack) {
        super(apiKey, appName);
        this.globIncludes = globIncludes;
        this.globExcludes = globExcludes;
        this.buildpack = buildpack;
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

    public String getGlobIncludes() {
        return globIncludes;
    }

    public String getGlobExcludes() {
        return globExcludes;
    }

    public String getBuildpack() {
        return buildpack;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, final HerokuAPI api, final App app) throws IOException, InterruptedException {
        return build.getWorkspace().act(new RemoteCallable(
                listener,
                getEffectiveApiKey(),
                app.getName(),
                app.getWebUrl(),
                new JenkinsUserAgentValueProvider().getLocalUserAgent(),
                globIncludes,
                globExcludes,
                buildpack));
    }

    @Override
    public AnvilDeploymentDescriptor getDescriptor() {
        return (AnvilDeploymentDescriptor) super.getDescriptor();
    }

    /**
     * A serializable, immutable payload for the deployment task.
     * Separated from containing class and environment to allow it to be run on remote slaves without trying to serialize the world.
     */
    public static class RemoteCallable implements FilePath.FileCallable<Boolean>, Serializable {

        private final BuildListener listener;
        private final String apiKey;
        private final String appName;
        private final String appWebUrl;
        private final String userAgent;
        private final String globIncludes;
        private final String globExcludes;
        private final String buildpack;

        RemoteCallable(BuildListener listener, String apiKey, String appName, String appWebUrl, String userAgent, String globIncludes, String globExcludes, String buildpack) {
            this.listener = listener;
            this.apiKey = apiKey;
            this.appName = appName;
            this.appWebUrl = appWebUrl;
            this.userAgent = userAgent;
            this.globIncludes = globIncludes;
            this.globExcludes = globExcludes;
            this.buildpack = buildpack;
        }

        public Boolean invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
            final Janvil janvil = new Janvil(new Config(apiKey)
                    .setProtocol(Config.Protocol.HTTP)
                    .setConsumersUserAgent(userAgent)
                    .setEventSubscription(new EventSubscription<Event>(Event.class)
                            .subscribe(Event.DIFF_START, new Subscriber<Event>() {
                                public void handle(Event event, Object data) {
                                    listener.getLogger().println("Detecting new files...");
                                }
                            })
                            .subscribe(Event.UPLOADS_START, new Subscriber<Event>() {
                                public void handle(Event event, Object data) {
                                    listener.getLogger().println("Uploading " + data + " new files...");
                                }
                            })
                            .subscribe(Event.BUILD_OUTPUT_LINE, new Subscriber<Event>() {
                                public void handle(Event event, Object data) {
                                    listener.getLogger().println(data);
                                }
                            })
                            .subscribe(Event.RELEASE_START, new Subscriber<Event>() {
                                public void handle(Event event, Object data) {
                                    listener.getLogger().println("Releasing build artifact...");
                                }
                            })
                            .subscribe(Event.RELEASE_END, new Subscriber<Event>() {
                                public void handle(Event event, Object data) {
                                    listener.getLogger().println("Released " + data + " to " + appWebUrl);
                                }
                            })
                    ));

            final Manifest manifest = new Manifest(workspace);
            new DirScanner.Glob(globIncludes, globExcludes).scan(workspace, new FileVisitor() {
                @Override
                public void visit(File f, String relativePath) throws IOException {
                    manifest.add(f);
                }
            });

            janvil.build(manifest, new HashMap<String, String>(), buildpack);
            janvil.release(appName, manifest);

            return true;
        }
    }

    @Extension
    public static class AnvilDeploymentDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Anvil Deploy";
        }

        public FormValidation doCheckGlobIncludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        public FormValidation doCheckGlobExcludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

    }
}


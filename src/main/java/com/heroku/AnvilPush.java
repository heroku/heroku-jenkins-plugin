package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.janvil.*;
import hudson.*;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.heroku.HerokuPlugin.Feature.ANVIL;

/*
 * @author Ryan Brainard
 */
public class AnvilPush extends AbstractHerokuBuildStep {

    private final String buildpackUrl;
    private final String buildEnv;
    private final String releaseDesc;
    private final String baseDir;
    private final String globIncludes;
    private final String globExcludes;
    private final boolean useCache;

    @DataBoundConstructor
    public AnvilPush(String apiKey, String appName, String buildpackUrl, String buildEnv, String releaseDesc, String baseDir, String globIncludes, String globExcludes, boolean useCache) {
        super(apiKey, appName);
        this.buildpackUrl = buildpackUrl;
        this.buildEnv = buildEnv;
        this.releaseDesc = releaseDesc;
        this.baseDir = baseDir;
        this.globIncludes = globIncludes;
        this.globExcludes = globExcludes;
        this.useCache = useCache;
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

    public String getBuildpackUrl() {
        return buildpackUrl;
    }

    public String getBuildEnv() {
        return buildEnv;
    }

    public String getReleaseDesc() {
        return releaseDesc;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public String getGlobIncludes() {
        return globIncludes;
    }

    public String getGlobExcludes() {
        return globExcludes;
    }

    public boolean isUseCache() {
        return useCache;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        return build.getWorkspace().child(baseDir).act(createRemoteCallable(build, listener, api, app));
    }

    /**
     * Bridge between perform() instance method and RemoteCallable serialization, static class.
     * Resolves all non-serializable instance data to create RemoteCallable
     */
    RemoteCallable createRemoteCallable(AbstractBuild build, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        return new RemoteCallable(
                build.getEnvironment(listener),
                listener,
                app,
                getEffectiveApiKey(),
                new JenkinsUserAgentValueProvider().getLocalUserAgent(),
                api.getUserInfo().getEmail(),
                buildpackUrl,
                globIncludes,
                globExcludes,
                buildEnv,
                releaseDesc,
                useCache
        );
    }

    @Override
    public AnvilPushDescriptor getDescriptor() {
        return (AnvilPushDescriptor) super.getDescriptor();
    }

    @Extension
    public static class AnvilPushDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Push";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return HerokuPlugin.get().hasFeature(ANVIL);
        }

        public FormValidation doCheckBuildpackUrl(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.okWithMarkup(
                        "<a href='https://devcenter.heroku.com/articles/buildpacks' target='_blank'>Buildpack</a> will be auto-detected. Provide a custom URL to override.");
            } else {
                final List<String> allowedSchemes = Arrays.asList("http", "https", "git");
                try {
                    final URI buildpackUrl = new URI(value);
                    if (buildpackUrl.getScheme() == null || !allowedSchemes.contains(buildpackUrl.getScheme())) {
                        return FormValidation.error("Should be of type http:// or git://");
                    }
                    return FormValidation.ok();
                } catch (URISyntaxException e) {
                    return FormValidation.error("Invalid URL format");
                }
            }
        }

        public FormValidation doCheckBuildEnv(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            try {
                MappingConverter.convert(value);
                return FormValidation.ok();
            } catch (Exception e) {
                return FormValidation.errorWithMarkup(
                        "Error parsing environment variables. " +
                                "Syntax follows that of <a href='http://docs.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)' target='_blank'> Java Properties files</a>.");
            }
        }

        public FormValidation doCheckBaseDir(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        public FormValidation doCheckGlobIncludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }
    }

    /**
     * A serializable, immutable payload for the push task.
     * Separated from containing class and environment to allow it to be run on remote slaves without trying to serialize the world.
     */
    static class RemoteCallable implements FilePath.FileCallable<Boolean>, Serializable {
        private final EnvVars jenkinsEnv;
        private final BuildListener listener;
        private final App app;
        private final String effectiveApiKey;
        private final String userAgent;
        private final String userEmail;
        private String buildpackUrl;
        private String globIncludes;
        private String globExcludes;
        private String buildEnv;
        private String releaseDesc;
        private boolean useCache;

        RemoteCallable(EnvVars jenkinsEnv, BuildListener listener, App app, String effectiveApiKey, String userAgent, String userEmail,
                       String buildpackUrl, String globIncludes, String globExcludes, String buildEnv, String releaseDesc, boolean useCache) {
            this.jenkinsEnv = jenkinsEnv;
            this.listener = listener;
            this.app = app;
            this.effectiveApiKey = effectiveApiKey;
            this.userAgent = userAgent;
            this.userEmail = userEmail;
            this.buildpackUrl = buildpackUrl;
            this.globIncludes = globIncludes;
            this.globExcludes = globExcludes;
            this.buildEnv = buildEnv;
            this.releaseDesc = releaseDesc;
            this.useCache = useCache;
        }

        public Boolean invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
            final Janvil janvil = new Janvil(config());

            final String slugUrl;
            try {
                slugUrl = janvil.build(manifest(dir), resolveBuildEnv(), buildpackUrl);
            } catch (JanvilBuildException e) {
                listener.error("A build error occurred: " + e.getExitStatus());
                return false;
            }

            janvil.release(app.getName(), slugUrl, resolveReleaseDesc());

            return true;
        }

        Manifest manifest(File dir) throws IOException {
            final Manifest manifest = new Manifest(dir);
            new DirScanner.Glob(globIncludes, globExcludes).scan(dir, new FileVisitor() {
                @Override
                public void visit(File f, String relativePath) throws IOException {
                    if (f.isFile()) {
                        manifest.add(f);
                    }
                }
            });
            return manifest;
        }

        Map<String, String> resolveBuildEnv() throws IOException, InterruptedException {
            final Map<String, String> buildEnvMap = MappingConverter.convert(buildEnv);

            // expand with jenkins env vars
            for (Map.Entry<String, String> e : buildEnvMap.entrySet()) {
                e.setValue(jenkinsEnv.expand(e.getValue()));
            }
            return buildEnvMap;
        }

        String resolveReleaseDesc() throws IOException, InterruptedException {
            return jenkinsEnv.expand(releaseDesc);
        }

        Config config() {
            return new Config(effectiveApiKey)
                    .setConsumersUserAgent(userAgent)
                    .setReadCacheUrl(useCache)
                    .setWriteCacheUrl(true)
                    .setWriteSlugUrl(false)
                    .setHerokuApp(app.getName())
                    .setHerokuUser(userEmail)
                    .setEventSubscription(eventSubscription());
        }

        EventSubscription<Janvil.Event> eventSubscription() {
            return new EventSubscription<Janvil.Event>(Janvil.Event.class)
                    .subscribe(Janvil.Event.DIFF_START, new EventSubscription.Subscriber<Janvil.Event>() {
                        public void handle(Janvil.Event event, Object numTotalFiles) {
                            listener.getLogger().println("Workspace contains " + amt(numTotalFiles, "file"));
                        }
                    })
                    .subscribe(Janvil.Event.UPLOADS_START, new EventSubscription.Subscriber<Janvil.Event>() {
                        public void handle(Janvil.Event event, Object numDiffFiles) {
                            if (numDiffFiles == Integer.valueOf(0)) return;
                            listener.getLogger().println("Uploading " + amt(numDiffFiles, "new file") + "...");
                        }
                    })
                    .subscribe(Janvil.Event.UPLOADS_END, new EventSubscription.Subscriber<Janvil.Event>() {
                        public void handle(Janvil.Event event, Object numDiffFiles) {
                            if (numDiffFiles == Integer.valueOf(0)) return;
                            listener.getLogger().println("Upload complete");
                        }
                    })
                    .subscribe(Janvil.Event.BUILD_OUTPUT_LINE, new EventSubscription.Subscriber<Janvil.Event>() {
                        public void handle(Janvil.Event event, Object line) {
                            if (String.valueOf(line).contains("Success, slug is ")) return;
                            listener.getLogger().println(line);
                        }
                    })
                    .subscribe(Janvil.Event.RELEASE_START, new EventSubscription.Subscriber<Janvil.Event>() {
                        public void handle(Janvil.Event event, Object data) {
                            listener.getLogger().println("Releasing to " + app.getName() + "...");
                        }
                    })
                    .subscribe(Janvil.Event.RELEASE_END, new EventSubscription.Subscriber<Janvil.Event>() {
                        public void handle(Janvil.Event event, Object version) {
                            listener.getLogger().println("Push complete, " + version + " | " + app.getWebUrl());
                        }
                    });
        }

        String amt(Object qty, String counter) {
            final double num = Double.valueOf(String.valueOf(qty));
            final String s = qty + " " + counter;
            if (num == 1) {
                return s;
            } else {
                return s + "s";
            }
        }
    }
}


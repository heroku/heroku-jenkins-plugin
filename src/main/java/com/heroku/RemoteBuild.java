package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.janvil.Config;
import com.heroku.janvil.EventSubscription;
import com.heroku.janvil.Janvil;
import com.heroku.janvil.Manifest;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/*
 * @author Ryan Brainard
 */
public class RemoteBuild extends AbstractHerokuBuildStep {

    private final String buildpackUrl;
    private final String buildEnv;
    private String globIncludes;
    private String globExcludes;
    private final boolean useCache;

    @DataBoundConstructor
    public RemoteBuild(String buildpackUrl, String buildEnv, String globIncludes, String globExcludes, boolean useCache) {
        super();
        this.buildpackUrl = buildpackUrl;
        this.buildEnv = buildEnv;
        this.globIncludes = globIncludes;
        this.globExcludes = globExcludes;
        this.useCache = useCache;
    }

    public String getBuildpackUrl() {
        return buildpackUrl;
    }

    public String getBuildEnv() {
        return buildEnv;
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
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        final String userAgent = new JenkinsUserAgentValueProvider().getLocalUserAgent();

        final URL slugUrl = build.getWorkspace().act(new FilePath.FileCallable<URL>() {

            final boolean[] slugPushed = new boolean[]{false}; //TODO: use exit code

            public URL invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
                final Janvil janvil = new Janvil(
                        new Config("")
                                .setConsumersUserAgent(userAgent)
                                .setReadCacheUrl(useCache)
                                .setWriteSlugUrl(true)
                                .setWriteCacheUrl(true)
                                .setEventSubscription(new EventSubscription<Janvil.Event>(Janvil.Event.class)
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
                                            public void handle(Janvil.Event event, Object data) {
                                                listener.getLogger().println(data);
                                                slugPushed[0] |= (String.valueOf(data).contains("Success, slug is ")); //TODO: use exit code
                                            }
                                        })));

                final Manifest manifest = new Manifest(workspace);  // TODO: allow for something other than workspace root?
                new DirScanner.Glob(globIncludes, globExcludes).scan(workspace, new FileVisitor() {
                    @Override
                    public void visit(File f, String relativePath) throws IOException {
                        if (f.isFile()) {
                            manifest.add(f);
                        }
                    }
                });

                slugPushed[0] = false; //TODO: use exit code

                final Map<String, String> buildEnvMap = MappingConverter.convert(buildEnv);

                // expand with jenkins env vars TODO: make this optional?
                final EnvVars jenkinsEnv = build.getEnvironment(listener);
                for (Map.Entry<String, String> e : buildEnvMap.entrySet()) {
                    e.setValue(jenkinsEnv.expand(e.getValue()));
                }

                final String slugUrl = janvil.build(manifest, buildEnvMap, buildpackUrl);

                //TODO: use exit code
                if (!slugPushed[0]) {
                    throw new IllegalStateException("Remote Build failed."); //TODO
                }

                return new URL(slugUrl);
            }
        });

        return true;
    }

    private static String amt(Object qty, String counter) {
        final double num = Double.valueOf(String.valueOf(qty));
        final String s = qty + " " + counter;
        if (num == 1) {
            return s;
        } else {
            return s + "s";
        }
    }

    @Override
    public RemoteBuildDescriptor getDescriptor() {
        return (RemoteBuildDescriptor) super.getDescriptor();
    }

    @Extension
    public static class RemoteBuildDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Remote Build";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return false;
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

        public FormValidation doCheckGlobIncludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        public FormValidation doCheckGlobExcludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }
    }
}


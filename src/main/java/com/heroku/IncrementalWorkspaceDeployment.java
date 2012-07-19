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
import java.util.Collections;

/**
 * @author Ryan Brainard
 */
public class IncrementalWorkspaceDeployment extends AbstractHerokuBuildStep {

    private static final String NULL_BUILDPACK = "https://github.com/ryandotsmith/null-buildpack.git";

    private final String globIncludes;
    private final String globExcludes;

    @DataBoundConstructor
    public IncrementalWorkspaceDeployment(String apiKey, String appName, String globIncludes, String globExcludes) {
        super(apiKey, appName);
        this.globIncludes = globIncludes;
        this.globExcludes = globExcludes;
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

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, final HerokuAPI api, final App app) throws IOException, InterruptedException {
        final String userAgent = new JenkinsUserAgentValueProvider().getLocalUserAgent();

        return build.getWorkspace().act(new FilePath.FileCallable<Boolean>() {

            final boolean[] slugPushed = new boolean[]{false}; //TODO: use exit code

            public Boolean invoke(File baseDir, VirtualChannel channel) throws IOException, InterruptedException {
                final Janvil janvil = new Janvil(
                        new Config(getEffectiveApiKey())
                                .setConsumersUserAgent(userAgent)
                                .setReadMetadata(false)
                                .setWriteMetadata(false)
                                .setEventSubscription(new EventSubscription<Janvil.Event>(Janvil.Event.class)
                                        .subscribe(Janvil.Event.DIFF_START, new EventSubscription.Subscriber<Janvil.Event>() {
                                            public void handle(Janvil.Event event, Object numTotalFiles) {
                                                listener.getLogger().println("Workspace contains " + amt(numTotalFiles, "file") + "...");
                                            }
                                        })
                                        .subscribe(Janvil.Event.UPLOADS_START, new EventSubscription.Subscriber<Janvil.Event>() {
                                            public void handle(Janvil.Event event, Object numDiffFiles) {
                                                listener.getLogger().println("Uploading " + amt(numDiffFiles, "new file") + "...");
                                            }
                                        })
                                        .subscribe(Janvil.Event.BUILD_OUTPUT_LINE, new EventSubscription.Subscriber<Janvil.Event>() {
                                            public void handle(Janvil.Event event, Object data) {
                                                // do not output to jenkins user
                                                System.out.println(data); //TODO: REMOVE

                                                slugPushed[0] |= (String.valueOf(data).contains("Success, slug is ")); //TODO: use exit code
                                            }
                                        })
                                        .subscribe(Janvil.Event.RELEASE_START, new EventSubscription.Subscriber<Janvil.Event>() {
                                            public void handle(Janvil.Event event, Object data) {
                                                listener.getLogger().println("Deploying...");
                                            }
                                        })
                                        .subscribe(Janvil.Event.RELEASE_END, new EventSubscription.Subscriber<Janvil.Event>() {
                                            public void handle(Janvil.Event event, Object versionNumber) {
                                                listener.getLogger().println("Released " + versionNumber + " to " + app.getWebUrl());
                                            }
                                        })));

                final Manifest manifest = new Manifest(baseDir);
                new DirScanner.Glob(globIncludes, globExcludes).scan(baseDir, new FileVisitor() {
                    @Override
                    public void visit(File f, String relativePath) throws IOException {
                        if (f.isFile()) {
                            manifest.add(f);
                        }
                    }
                });

                slugPushed[0] = false; //TODO: use exit code

                final String slugUrl = janvil.build(manifest, Collections.<String, String>emptyMap(), NULL_BUILDPACK);

                //TODO: use exit code
                if (!slugPushed[0]) {
                    listener.error("Upload failed. Aborting deployment.");
                    return false;
                }

                janvil.release(app.getName(), slugUrl);

                return true;
            }

            private String amt(Object qty, String counter) {
                final double num = Double.valueOf(String.valueOf(qty));
                final String s = qty + " " + counter;
                if (num == 1) {
                    return s;
                } else {
                    return s + "s";
                }
            }
        });
    }

    @Override
    public IncrementalWorkspaceDeploymentDescriptor getDescriptor() {
        return (IncrementalWorkspaceDeploymentDescriptor) super.getDescriptor();
    }

    @Extension
    public static class IncrementalWorkspaceDeploymentDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Deploy Workspace (Incremental)";
        }

        public FormValidation doCheckGlobIncludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        public FormValidation doCheckGlobExcludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

    }
}


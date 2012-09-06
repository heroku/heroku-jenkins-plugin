package com.heroku;

import com.heroku.api.App;
import com.heroku.janvil.Config;
import com.heroku.janvil.EventSubscription;
import com.heroku.janvil.Janvil;
import com.heroku.janvil.Manifest;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * A serializable, immutable payload for the deployment task.
 * Separated from containing class and environment to allow it to be run on remote slaves without trying to serialize the world.
 */
class AnvilDeployer implements FilePath.FileCallable<Boolean>, Serializable {

    private final BuildListener listener;
    private final String apiKey;
    private final String appName;
    private final String appWebUrl;
    private final String userAgent;
    private final Map<String, String> buildEnv;
    private final DirScanner dirScanner;
    private final String buildpack;
    private final boolean[] slugPushed = new boolean[]{false};

    AnvilDeployer(BuildListener listener, String apiKey, App app, DirScanner dirScanner, String buildpack, Map<String, String> buildEnv) {
        this.listener = listener;
        this.apiKey = apiKey;
        this.appName = app.getName();
        this.appWebUrl = app.getWebUrl();
        this.userAgent = new JenkinsUserAgentValueProvider().getLocalUserAgent();
        this.dirScanner = dirScanner;
        this.buildpack = buildpack;
        this.buildEnv = buildEnv;
    }

    public Boolean invoke(File baseDir, VirtualChannel channel) throws IOException, InterruptedException {
        final Janvil janvil = new Janvil(new Config(apiKey)
                .setConsumersUserAgent(userAgent)
                .setEventSubscription(buildSubscriptions()));

        final Manifest manifest = new Manifest(baseDir);
        dirScanner.scan(baseDir, new FileVisitor() {
            @Override
            public void visit(File f, String relativePath) throws IOException {
                if (f.isFile()) {
                    manifest.add(f);
                }
            }
        });

        slugPushed[0] = false;

        final String slugUrl = janvil.build(manifest, buildEnv, buildpack);

        if (!slugPushed[0]) {
            listener.error("Remote build failed. Aborting deployment.");
            return false;
        }

        janvil.release(appName, slugUrl, "Jenkins"); // TODO: what should the description be?

        return true;
    }

    private EventSubscription<Janvil.Event> buildSubscriptions() {
        return new EventSubscription<Janvil.Event>(Janvil.Event.class)
                .subscribe(Janvil.Event.DIFF_START, new EventSubscription.Subscriber<Janvil.Event>() {
                    public void handle(Janvil.Event event, Object data) {
                        listener.getLogger().println("Detecting new files...");
                    }
                })
                .subscribe(Janvil.Event.UPLOADS_START, new EventSubscription.Subscriber<Janvil.Event>() {
                    public void handle(Janvil.Event event, Object data) {
                        listener.getLogger().println("Uploading " + data + " new files...");
                    }
                })
                .subscribe(Janvil.Event.BUILD_OUTPUT_LINE, new EventSubscription.Subscriber<Janvil.Event>() {
                    public void handle(Janvil.Event event, Object data) {
                        slugPushed[0] |= (String.valueOf(data).contains("Success, slug is "));
                        listener.getLogger().println(data);
                    }
                })
                .subscribe(Janvil.Event.RELEASE_START, new EventSubscription.Subscriber<Janvil.Event>() {
                    public void handle(Janvil.Event event, Object data) {
                        listener.getLogger().println("Releasing build artifact...");
                    }
                })
                .subscribe(Janvil.Event.RELEASE_END, new EventSubscription.Subscriber<Janvil.Event>() {
                    public void handle(Janvil.Event event, Object data) {
                        listener.getLogger().println("Released " + data + " to " + appWebUrl);
                    }
                });
    }
}

package com.heroku;

import com.herokuapp.janvil.Config;
import com.herokuapp.janvil.EventSubscription;
import com.herokuapp.janvil.Janvil;
import com.herokuapp.janvil.Manifest;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

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
    private final String globIncludes;
    private final String globExcludes;
    private final String buildpack;

    AnvilDeployer(BuildListener listener, String apiKey, String appName, String appWebUrl, String userAgent, String globIncludes, String globExcludes, String buildpack) {
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
                .setEventSubscription(buildSubscriptions()));

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

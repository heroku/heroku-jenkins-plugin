package com.heroku;

import com.google.common.collect.ImmutableMap;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.herokuapp.directto.client.DirectToHerokuClient;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.DirScanner;
import hudson.util.io.ArchiverFactory;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class WorkspaceDeployment extends AbstractHerokuBuildStep {

    private final String globIncludes;
    private final String globExcludes;
    private final String procfilePath;

    @DataBoundConstructor
    public WorkspaceDeployment(String apiKey, String appName, String globIncludes, String globExcludes, String procfilePath) {
        super(apiKey, appName);
        this.globIncludes = globIncludes;
        this.globExcludes = globExcludes;
        this.procfilePath = procfilePath;
    }

    // Overridding and delegating to parent because Jelly only looks at concrete class when rendering views
    @Override
    public String getAppName() {
        return super.getAppName();
    }

    // Overridding and delegating to parent because Jelly only looks at concrete class when rendering views
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

    public String getProcfilePath() {
        return procfilePath;
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        File tempTarFile = null;
        File tempProcfile = null;
        try {
            listener.getLogger().print("Fetching Procfile...");
            listener.getLogger().flush();
            FileOutputStream tempProcfileStream = null;
            try {
                tempProcfile = File.createTempFile(build.getProject().getName() + Integer.toString(build.getNumber()), ".procfile");
                tempProcfileStream = new FileOutputStream(tempProcfile);
                final FilePath procfile = build.getWorkspace().child(procfilePath);
                if (!procfile.exists()) {
                    listener.error("Procfile not found");
                    return false;
                }
                procfile.copyTo(tempProcfileStream);
            } finally {
                if (tempProcfileStream != null) tempProcfileStream.close();
            }
            listener.getLogger().println("done");

            listener.getLogger().print("Bundling workspace...");
            listener.getLogger().flush();
            FileOutputStream tempTarStream = null;
            try {
                tempTarFile = File.createTempFile(build.getProject().getName() + Integer.toString(build.getNumber()), ".tar.gz");
                tempTarStream = new FileOutputStream(tempTarFile);
                build.getWorkspace().archive(ArchiverFactory.TARGZ, tempTarStream, new DirScanner.Glob(globIncludes, globExcludes));
            } finally {
                if (tempTarStream != null) tempTarStream.close();
            }
            final String tempTarFileSize = new DecimalFormat("#.##").format(((double) tempTarFile.length()) / (1024 * 1024));
            listener.getLogger().println("done | " + tempTarFileSize + " MB");

            listener.getLogger().print("Deploying...");
            listener.getLogger().flush();
            final Map<String, File> payload = ImmutableMap.of("targz", tempTarFile,
                    "procfile", tempProcfile);

            new DirectToHerokuClient.Builder()
                    .setApiKey(getEffectiveApiKey())
                    .setConsumersUserAgent(new JenkinsUserAgentValueProvider().getLocalUserAgent())
                    .build().deploy("targz", app.getName(), payload);

            listener.getLogger().println("done");
            listener.getLogger().println(app.getWebUrl());
        } finally {
            if (tempTarFile != null) tempTarFile.delete();
            if (tempProcfile != null) tempProcfile.delete();
        }

        return true;
    }

    @Extension
    public static class WorkspaceDeploymentDescriptor extends AbstractHerokuBuildStepDescriptor {

        @Override
        public String getDisplayName() {
            return "Heroku: Deploy Workspace";
        }

        // TODO: validation

    }
}

package com.heroku;

import com.google.common.collect.ImmutableMap;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.util.DirScanner;
import hudson.util.FormValidation;
import hudson.util.io.Archiver;
import hudson.util.io.ArchiverFactory;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Ryan Brainard
 */
public class WorkspaceDeployment extends AbstractArtifactDeployment {

    private final String globIncludes;
    private final String globExcludes;

    @DataBoundConstructor
    public WorkspaceDeployment(String apiKey, String appName, String globIncludes, String globExcludes, String procfilePath) {
        super(apiKey, appName, ImmutableMap.of("targz", UUID.randomUUID().toString() + ".tar.gz", PROCFILE_PATH, procfilePath));
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

    public String getProcfilePath() {
        return artifactPaths.get(PROCFILE_PATH);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        try {
            listener.getLogger().println("Bundling workspace for deployment");

            if (!build.getWorkspace().child(artifactPaths.get(PROCFILE_PATH)).exists()) {
                listener.error("Profile not found in workspace. \n" +
                        "Location of Procfile can be specified in advanced configuration build step '" + getDescriptor().getDisplayName() + "'.\n" +
                        "For information on Procfile, see Heroku Dev Center: https://devcenter.heroku.com/articles/procfile\n");
                return false;
            }

            build.getWorkspace().act(RemoteWorkspaceArchiveCreation());

            return super.perform(build, launcher, listener, api, app);
        } finally {
            try {
                build.getWorkspace().child(artifactPaths.get(TARGZ_PATH)).delete();
            } catch (IOException e) {
                listener.error(e.getMessage());
                e.printStackTrace(listener.getLogger());
            } catch (InterruptedException e) {
                listener.error(e.getMessage());
                e.printStackTrace(listener.getLogger());
            }
        }
    }

    /**
     * Creates an archive of the workspace in the workspace
     */
    private FilePath.FileCallable<File> RemoteWorkspaceArchiveCreation() {
        return new FilePath.FileCallable<File>() {
            public File invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
                FileOutputStream archiveStream = null;
                try {
                    File archiveFile = new File(workspace.getAbsolutePath() + File.separator + artifactPaths.get(TARGZ_PATH));
                    archiveStream = new FileOutputStream(archiveFile);

                    Archiver archiver = null;
                    try {
                        archiver = ArchiverFactory.TARGZ.create(archiveStream);
                        final String globExcludesWithSelfExclude = globExcludes + "," + artifactPaths.get(TARGZ_PATH);
                        new DirScanner.Glob(globIncludes, globExcludesWithSelfExclude).scan(workspace, archiver);
                    } finally {
                        if (archiver != null) archiver.close();
                    }
                    return archiveFile;
                } finally {
                    if (archiveStream != null) archiveStream.close();
                }
            }
        };
    }

    @Extension
    public static class WorkspaceDeploymentDescriptor extends AbstractArtifactDeployment.AbstractArtifactDeploymentDescriptor {

        @Override
        public String getPipelineName() {
            return TARGZ_PIPELINE;
        }

        @Override
        protected String getPipelineDisplayName() {
            return "workspace";
        }

        @Override
        public String getDisplayName() {
            return "Heroku: Deploy Workspace";
        }

        public FormValidation doCheckProcfilePath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return checkAnyArtifactPath(project, value, "Procfile");
        }

        public FormValidation doCheckGlobIncludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        public FormValidation doCheckGlobExcludes(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

    }
}

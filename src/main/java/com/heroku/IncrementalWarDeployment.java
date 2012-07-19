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
import hudson.util.DirScanner;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public class IncrementalWarDeployment extends AbstractHerokuBuildStep {

    private static final String WEBAPP_RUNNER_BUILDPACK = "https://github.com/heroku/heroku-buildpack-webapp-runner.git"; //TODO: parameterize

    private final String warPath;

    @DataBoundConstructor
    public IncrementalWarDeployment(String apiKey, String appName, String warPath) {
        super(apiKey, appName);
        this.warPath = warPath;
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

    public String getWarPath() {
        return warPath;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener, final HerokuAPI api, final App app) throws IOException, InterruptedException {
        return build.getWorkspace().child(warPath).act(new AnvilDeployer(
                listener,
                getEffectiveApiKey(),
                app,
                new DirScanner.Full(),
                WEBAPP_RUNNER_BUILDPACK,
                ImmutableMap.of("WEBAPP_RUNNER_VERSION", "7.0.27.1")));
    }

    @Override
    public IncrementalWorkspaceDeploymentDescriptor getDescriptor() {
        return (IncrementalWorkspaceDeploymentDescriptor) super.getDescriptor();
    }

    @Extension
    public static class IncrementalWorkspaceDeploymentDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Deploy WAR (Incremental)";
        }

        public FormValidation doWarPath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

    }
}


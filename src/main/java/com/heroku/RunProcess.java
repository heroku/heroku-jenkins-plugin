package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.request.run.RunResponse;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ryan Brainard
 */
public class RunProcess extends AbstractHerokuBuildStep {

    private final String command;

    @DataBoundConstructor
    public RunProcess(String apiKey, String appName, String command) {
        super(apiKey, appName);
        this.command = command;
    }

    public String getCommand() {
        return command;
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

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        final RunResponse runResponse = api.runAttached(app.getName(), command);

        listener.getLogger().println(
                "Running `" + runResponse.getProc().getCommand() + "` on " + app.getName() + "... " +
                runResponse.getProc().getState() + ", " + runResponse.getProc().getProcess());

        InputStream runStream = null;
        try {
            runStream = new BufferedInputStream(runResponse.attach());
            int next;
            while ((next = runStream.read()) != -1) {
                listener.getLogger().write(next);
            }

        } finally {
            if (runStream != null) runStream.close();
        }

        return true;
    }


    @Override
    public RunProcessDescriptor getDescriptor() {
        return (RunProcessDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class RunProcessDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Run Process";
        }

        public FormValidation doCheckCommand(@AncestorInPath AbstractProject project, @QueryParameter String command) throws IOException {
            return FormValidation.validateRequired(command);
        }
    }
}

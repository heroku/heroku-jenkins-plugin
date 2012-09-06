package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

import static com.heroku.HerokuPlugin.Feature.APP_CLONE;

/**
 * @author Ryan Brainard
 */
public class AppClone extends AbstractHerokuBuildStep {

    private final String templateAppName;

    @DataBoundConstructor
    public AppClone(String apiKey, String appName, String templateAppName) {
        super(apiKey, appName);
        this.templateAppName = templateAppName;
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

    public String getTemplateAppName() {
        return templateAppName;
    }

    @Override
    protected App getOrCreateApp(BuildListener listener, HerokuAPI api) {
        return new App().named(getAppName()).on(Heroku.Stack.Cedar);
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App targetApp) throws IOException, InterruptedException {
        listener.getLogger().println("Cloning template " + templateAppName + "...");
        final App clonedApp = api.cloneApp(templateAppName, targetApp);
        listener.getLogger().println("Done, created app " + clonedApp.getName());

        return true;
    }

    @Override
    public AppCloneDescriptor getDescriptor() {
        return (AppCloneDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class AppCloneDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Create App from Template";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return HerokuPlugin.get().hasFeature(APP_CLONE);
        }

        public FormValidation doCheckTemplateAppName(@AncestorInPath AbstractProject project, @QueryParameter String templateAppName) throws IOException {
            return FormValidation.validateRequired(templateAppName);
        }
    }

}

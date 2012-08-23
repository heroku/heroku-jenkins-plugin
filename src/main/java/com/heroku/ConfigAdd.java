package com.heroku;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Release;
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
import java.util.List;

/**
 * @author Ryan Brainard
 */
public class ConfigAdd extends AbstractHerokuBuildStep {

    private String configVars;

    @DataBoundConstructor
    public ConfigAdd(String apiKey, String appName, String configVars) {
        super(apiKey, appName);
        this.configVars = configVars;
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

    public String getConfigVars() {
        return configVars;
    }

    @Override
    protected boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener, HerokuAPI api, App app) throws IOException, InterruptedException {
        listener.getLogger().println("Setting config vars and restarting " + app.getName() + "...");

        final String expandedConfigVars = build.getEnvironment(listener).expand(configVars);
        api.addConfig(app.getName(), MappingConverter.convert(expandedConfigVars));

        final List<Release> releases = api.listReleases(app.getName());
        final Release currentRelease = releases.get(releases.size() - 1);
        listener.getLogger().println("Done, " + currentRelease.getName());

        return true;
    }


    @Override
    public ConfigAddDescriptor getDescriptor() {
        return (ConfigAddDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class ConfigAddDescriptor extends AbstractHerokuBuildStepDescriptor {

        public String getDisplayName() {
            return "Heroku: Set Configuration";
        }

        public FormValidation doCheckConfigVars(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            try {
                MappingConverter.convert(value);
                return hudson.util.FormValidation.ok();
            } catch (Exception e) {
                return hudson.util.FormValidation.errorWithMarkup(
                        "Error parsing config vars. " +
                                "Syntax follows that of <a href='http://docs.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)' target='_blank'> Java Properties files</a>.");
            }
        }
    }
}

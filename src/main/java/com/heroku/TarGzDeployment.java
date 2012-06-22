package com.heroku;

import com.google.common.collect.ImmutableMap;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Ryan Brainard
 */
public class TarGzDeployment extends AbstractArtifactDeployment {

    @DataBoundConstructor
    public TarGzDeployment(String apiKey, String appName, String targzPath, String procfilePath) {
        super(apiKey, appName, ImmutableMap.of(TARGZ_PATH, targzPath, PROCFILE_PATH, procfilePath));
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

    public String getTargzPath() {
        return artifactPaths.get(TARGZ_PATH);
    }

    public String getProcfilePath() {
        return artifactPaths.get(PROCFILE_PATH);
    }

    @Extension
    public static class TarGzDeploymentDescriptor extends AbstractArtifactDeployment.AbstractArtifactDeploymentDescriptor {

        @Override
        public String getPipelineName() {
            return TARGZ_PIPELINE;
        }

        public FormValidation doCheckTargzPath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return checkAnyArtifactPath(project, value, ".targz", "tar.gz");
        }

        public FormValidation doCheckProcfilePath(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return checkAnyArtifactPath(project, value, "Procfile");
        }
    }
}

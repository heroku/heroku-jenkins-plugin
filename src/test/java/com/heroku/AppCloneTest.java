package com.heroku;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

/**
 * @author Ryan Brainard
 */
public class AppCloneTest extends BaseHerokuBuildStepTest {

    public void testAppClone() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        final String newAppName = "new-cloned-app" + String.valueOf(System.currentTimeMillis());
        project.getBuildersList().add(new AppClone(apiKey, newAppName, "template-java-spring-hibernate"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        String logs = FileUtils.readFileToString(build.getLogFile());
        assertTrue(logs.contains("Created new app: " + newAppName));
    }
}

package com.heroku;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

/**
 * @author Ryan Brainard
 */
public class RunProcessTest extends BaseHerokuBuildStepTest {

    public void testPerform() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new RunProcess(apiKey, appName, "pwd"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        String logs = FileUtils.readFileToString(build.getLogFile());
        assertTrue("Should have connected to dyno and run `pwd`", logs.contains("/app"));
    }
}

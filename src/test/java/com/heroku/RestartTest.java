package com.heroku;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

/**
 * @author Ryan Brainard
 */
public class RestartTest extends BaseHerokuBuildStepTest {

    public void testPerform() throws Exception {
        api.scaleProcess(appName, "web", 1);
        final int elapsedBefore = api.listProcesses(appName).get(0).getElapsed();

        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Restart(apiKey, appName));
        final FreeStyleBuild build = project.scheduleBuild2(0).get();

        String logs = FileUtils.readFileToString(build.getLogFile());
        assertTrue(logs.contains("Restarting " + appName));

        assertRestart(elapsedBefore);
    }

    private void assertRestart(int elapsedBefore) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            final int elapsedAfter = api.listProcesses(appName).get(0).getElapsed();
            if (elapsedAfter < elapsedBefore) return;
            Thread.sleep(5000);
        }
        fail("App should restart within reasonable time frame");
    }
}

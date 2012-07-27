package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Release;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ryan Brainard
 */
public class RestartTest extends BaseHerokuBuildStepTest {

    public void testRestart() throws Exception {
        final int elapsedBefore = api.listProcesses(appName).get(0).getElapsed();

        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Restart(apiKey, appName));
        final FreeStyleBuild build = project.scheduleBuild2(0).get();

        String logs = FileUtils.readFileToString(build.getLogFile());
        assertTrue(logs.contains("Restarting app " + appName + " ..."));
        assertTrue(logs.contains("Restart complete"));

        final int elapsedAfter = api.listProcesses(appName).get(0).getElapsed();
        assertTrue(elapsedAfter < elapsedBefore);
    }
}

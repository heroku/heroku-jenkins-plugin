package com.heroku;

import com.heroku.api.App;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

/**
 * @author Ryan Brainard
 */
public class RestartTest extends BaseHerokuBuildStepTest {

    public void testPerform() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                FreeStyleProject project = createFreeStyleProject();
                project.getBuildersList().add(new Restart(apiKey, app.getName()));
                final FreeStyleBuild build = project.scheduleBuild2(0).get();

                String logs = FileUtils.readFileToString(build.getLogFile());
                assertTrue(logs.contains("Restarting " + app.getName()));
            }
        });
    }
}

package com.heroku;

import com.heroku.api.App;
import hudson.model.FreeStyleProject;

/**
 * @author Ryan Brainard
 */
public class MaintenanceModeTest extends BaseHerokuBuildStepTest {

    public void testPerform() throws Exception {
        final FreeStyleProject project = createFreeStyleProject();

        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                String appName = app.getName();

                project.getBuildersList().add(new MaintenanceMode(apiKey, appName, true));
                project.scheduleBuild2(0).get();
                assertTrue(api.isMaintenanceModeEnabled(appName));

                project.getBuildersList().clear();
                project.getBuildersList().add(new MaintenanceMode(apiKey, appName, false));
                project.scheduleBuild2(0).get();
                assertFalse(api.isMaintenanceModeEnabled(appName));
            }
        });
    }
}

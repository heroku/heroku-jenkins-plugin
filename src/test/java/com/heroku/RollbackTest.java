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
public class RollbackTest extends BaseHerokuBuildStepTest {

    public void testRollback() throws Exception {
        // TODO: add some actual releases if people are using a new app
        final List<Release> releasesBefore = api.listReleases(appName);
        final Release releaseBefore = releasesBefore.get(releasesBefore.size() - 1);

        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Rollback(apiKey, appName));
        final FreeStyleBuild build = project.scheduleBuild2(0).get();

        final List<Release> releasesAfter = api.listReleases(appName);
        assertEquals(releasesBefore.size() + 1, releasesAfter.size());
        final Release afterRelease = releasesAfter.get(releasesAfter.size() - 1);
        final int releaseBeforeLast = releaseNameToInt(releaseBefore.getName()) - 1;
        assertEquals("Rollback to v" + releaseBeforeLast, afterRelease.getDescription());
    }

    public void testRollback_NewApp() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                assertEquals("Precondition: Should only have one release", 1, api.listReleases(app.getName()).size());

                FreeStyleProject project = createFreeStyleProject();
                project.getBuildersList().add(new Rollback(apiKey, app.getName()));
                final FreeStyleBuild build = project.scheduleBuild2(0).get();
                String logs = FileUtils.readFileToString(build.getLogFile());

                assertStringContains(logs, "Cannot rollback to before app was created");
                assertEquals("Should still only have one release", 1, api.listReleases(app.getName()).size());
            }
        });
    }

    private int releaseNameToInt(String releaseName) {
        final Matcher matcher = Pattern.compile("v(\\d+)").matcher(releaseName);
        if (!matcher.matches()) throw new IllegalArgumentException();
        return Integer.parseInt(matcher.group(1));
    }
}

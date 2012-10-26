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
        assertEquals(v(last(releasesBefore)) + 1, v(last(releasesAfter)));
        final Release afterRelease = last(releasesAfter);
        final int releaseBeforeLast = v(releaseBefore) - 1;
        assertEquals("Rollback to v" + releaseBeforeLast, afterRelease.getDescription());
    }

    private int v(Release release) {
        final Matcher matcher = Pattern.compile("v(\\d+)").matcher(release.getName());
        if (!matcher.matches()) throw new IllegalArgumentException();
        return Integer.parseInt(matcher.group(1));
    }
}

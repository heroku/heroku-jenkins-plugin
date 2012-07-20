package com.heroku;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.net.URL;

/**
 * @author Ryan Brainard
 */
public class OverwriteableUntarTest extends BaseHerokuBuildStepTest {

    public void testUntarFromUrlStream() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList();
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        final URL url = new URL("https://api.anvilworks.org/slugs/0667b9b1-d23b-11e1-bfe8-c9f796deb4b9.tgz");

        final URL src = ClassLoader.getSystemResource("sample-tar.tgz");

        // untar once
        OverwritableUntar.untar(src.openStream(), build.getWorkspace(), FilePath.TarCompression.GZIP);
        assertModes(build);

        OverwritableUntar.untar(src.openStream(), build.getWorkspace(), FilePath.TarCompression.GZIP);
        assertModes(build);
    }

    private void assertModes(FreeStyleBuild build) throws IOException, InterruptedException {
        final FilePath sampletar = build.getWorkspace().child("sample-tar");
        assertMode(sampletar.child("full"), 777);
        assertMode(sampletar.child("readonly"), 444);
        assertMode(sampletar.child("writeonly"), 200);
        assertMode(sampletar.child("execonly"), 111);
    }

    private void assertMode(FilePath path, int expected) throws IOException, InterruptedException {
        assertEquals("100" + expected, Integer.toOctalString(path.mode()));
    }
}

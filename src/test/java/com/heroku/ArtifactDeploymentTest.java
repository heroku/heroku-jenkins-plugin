package com.heroku;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import junit.framework.TestSuite;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Ryan Brainard
 */
public class ArtifactDeploymentTest extends BaseHerokuBuildStepTest {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        try {
            suite.addTest(new ArtifactDeploymentTest(
                    WarDeployment.class,
                    File.createTempFile("test", ".war")));

            suite.addTest(new ArtifactDeploymentTest(
                    TarGzDeployment.class,
                    File.createTempFile("test", ".tar.gz"),
                    new File(ClassLoader.getSystemResource("Procfile").getFile())));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return suite;
    }

    private Class<? extends AbstractArtifactDeployment> deploymentStepClass;
    private File[] artifacts;

    public ArtifactDeploymentTest(Class<? extends AbstractArtifactDeployment> deploymentStepClass, File... artifacts) {
        setName("test" + deploymentStepClass.getSimpleName());
        this.deploymentStepClass = deploymentStepClass;
        this.artifacts = artifacts;
    }

    public void runTest() throws Exception {
        assertNotNull("Jelly file should exists", ClassLoader.getSystemResource(deploymentStepClass.getName().replaceAll("\\.", File.separator) + File.separator + "config.jelly"));

        final FreeStyleProject project = createFreeStyleProject();
        project.scheduleBuild2(0).get(); // run build once get create workspace
        for (File a : artifacts) {
            new FilePath(a).copyTo(project.getSomeWorkspace().child(a.getName()));
        }

        project.getBuildersList().add(createDeploymentBuildStep());
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String logs = FileUtils.readFileToString(build.getLogFile());
        assertTrue(logs, logs.contains("Deployment successful"));
    }

    private AbstractArtifactDeployment createDeploymentBuildStep() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final int baseArgs = 2;
        final String[] args = new String[baseArgs + artifacts.length];
        args[0] = apiKey;
        args[1] = appName;
        for (int i = 0, artifactsLength = artifacts.length; i < artifactsLength; i++) {
            args[baseArgs + i] = artifacts[i].getName();
        }

        for (Constructor c : deploymentStepClass.getConstructors()) {
            if (c.getParameterTypes().length == artifacts.length + baseArgs) {
                for (Class p : c.getParameterTypes()) {
                    if (!p.equals(String.class)) break;
                }
                return (AbstractArtifactDeployment) c.newInstance(args);
            }
        }

        throw new NoSuchMethodException("No appropriate constructor could be found");
    }
}

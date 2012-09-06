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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return suite;
    }

    private Class<? extends AbstractArtifactDeployment> deploymentStepClass;
    private Object[] additionalArgs;

    public ArtifactDeploymentTest(Class<? extends AbstractArtifactDeployment> deploymentStepClass, Object... additionalArgs) {
        setName("test" + deploymentStepClass.getSimpleName());
        this.deploymentStepClass = deploymentStepClass;
        this.additionalArgs = additionalArgs;
    }

    public void runTest() throws Exception {
        assertNotNull("Jelly file should exists", ClassLoader.getSystemResource(deploymentStepClass.getName().replaceAll("\\.", File.separator) + File.separator + "config.jelly"));

        final FreeStyleProject project = createFreeStyleProject();
        project.scheduleBuild2(0).get(); // run empty build once get create workspace
        for (Object a : additionalArgs) {
            if (a instanceof File) {
                new FilePath((File) a).copyTo(project.getSomeWorkspace().child(((File) a).getName()));
            }
        }

        project.getBuildersList().add(createDeploymentBuildStep());
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String logs = FileUtils.readFileToString(build.getLogFile());
        assertTrue(logs, logs.contains("Uploading..."));
        assertTrue(logs, logs.contains("Deploying..."));
        assertTrue(logs, logs.contains("Launching... done, v"));
    }

    private AbstractArtifactDeployment createDeploymentBuildStep() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final int baseArgs = 2;
        final Object[] args = new Object[baseArgs + additionalArgs.length];
        args[0] = apiKey;
        args[1] = appName;
        for (int i = 0, artifactsLength = additionalArgs.length; i < artifactsLength; i++) {
            final Object convertedArg;
            if (additionalArgs[i] instanceof File) {
                convertedArg = ((File) additionalArgs[i]).getName();
            } else {
                convertedArg = additionalArgs[0];
            }

            args[baseArgs + i] = convertedArg;
        }

        for (Constructor c : deploymentStepClass.getConstructors()) {
            final Class[] parameterTypes = c.getParameterTypes();
            if (parameterTypes.length == args.length) {
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!parameterTypes[i].isAssignableFrom(args[i].getClass())) break;
                }
                return (AbstractArtifactDeployment) c.newInstance(args);
            }
        }

        throw new NoSuchMethodException("No appropriate constructor could be found");
    }
}

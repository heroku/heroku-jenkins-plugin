package com.heroku;

import com.heroku.api.App;
import hudson.model.*;
import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * @author Ryan Brainard
 */
public class AnvilPushTest extends BaseHerokuBuildStepTest {

    public void testPerform() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                FreeStyleProject project = createFreeStyleProject();
                project.scheduleBuild2(0).get();
                project.getSomeWorkspace().child("Procfile").copyFrom(ClassLoader.getSystemResource("Procfile"));

                project.getBuildersList().add(new AnvilPush(apiKey, app.getName(), "", "", "TEST", "", "", "", false));
                FreeStyleBuild build = project.scheduleBuild2(0).get();

                String logs = FileUtils.readFileToString(build.getLogFile());

                assertTrue(logs.contains("Workspace contains"));
                assertTrue(logs.contains("Push complete"));
            }
        });
    }

    public void testRemoteCallableSerialization() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                FreeStyleProject project = createFreeStyleProject();
                FreeStyleBuild build = project.scheduleBuild2(0).get();

                final BuildListener emptyBuildListener = new NullBuildListener();

                final AnvilPush.RemoteCallable pushRemoteCallable =
                        new AnvilPush(apiKey, app.getName(), "", "", "TEST", "", "", "", false)
                                .createRemoteCallable(build, emptyBuildListener, api, app);

                final ByteArrayOutputStream serialization = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(serialization);
                oos.writeObject(pushRemoteCallable);
                oos.close();

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialization.toByteArray()));
                final AnvilPush.RemoteCallable unserializedPushRemoteCallable = (AnvilPush.RemoteCallable) ois.readObject();
                ois.close();

                build.getWorkspace().act(unserializedPushRemoteCallable);
            }
        });
    }

}

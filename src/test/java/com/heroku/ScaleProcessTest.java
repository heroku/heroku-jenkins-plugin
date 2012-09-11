package com.heroku;

import com.heroku.api.App;
import com.heroku.api.Proc;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryan Brainard
 */
public class ScaleProcessTest extends BaseHerokuBuildStepTest {

    public void testPerform() throws Exception {
        runWithNewApp(new AppRunnable() {
            public void run(App app) throws Exception {
                FreeStyleProject project = createFreeStyleProject();

                final String processType = "web";
                final int orgQty = getProcessesByType(app).get(processType).size();
                final int scaleToZero = 0;
                assertNotSame(orgQty, scaleToZero);

                try {
                    project.getBuildersList().add(new ScaleProcess(apiKey, app.getName(), processType, scaleToZero));
                    FreeStyleBuild build = project.scheduleBuild2(0).get();
                    String logs = FileUtils.readFileToString(build.getLogFile());

                    assertNull(logs, getProcessesByType(app).get(processType));
                } finally {
                    api.scaleProcess(app.getName(), processType, orgQty);
                }
            }
        });
    }

    private Map<String, Set<Proc>> getProcessesByType(App app) {
        final Map<String, Set<Proc>> processesByType = new HashMap<String, Set<Proc>>();
        for (Proc p : api.listProcesses(app.getName())) {
            final String pType = p.getProcess().substring(0, p.getProcess().indexOf("."));

            Set<Proc> pTypes = processesByType.get(pType);
            if (pTypes == null) {
                pTypes = new HashSet<Proc>();
            }

            pTypes.add(p);
            processesByType.put(pType, pTypes);
        }
        return processesByType;
    }
}

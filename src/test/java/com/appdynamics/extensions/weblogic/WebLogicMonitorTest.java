package com.appdynamics.extensions.weblogic;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

/**
 * Created by balakrishnav on 12/10/15.
 */
public class WebLogicMonitorTest {
    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testWebLogicMonitor() throws TaskExecutionException {
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");

        WebLogicMonitor monitor = new WebLogicMonitor();
        monitor.execute(taskArgs, null);
    }
}

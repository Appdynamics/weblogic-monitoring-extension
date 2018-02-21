/**
 * Copyright 2015 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.weblogic;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.conf.MonitorConfiguration.ConfItem;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by balakrishnav on 10/9/15.
 */
public class WebLogicMonitor extends AManagedMonitor {
    public static final Logger logger = LoggerFactory.getLogger(WebLogicMonitor.class);

    private static final String METRIC_PREFIX = "Custom Metrics|WebLogic|";

    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/WebLogicMonitor/config.yml";

    private boolean initialized;
    private MonitorConfiguration configuration;

    public WebLogicMonitor() {
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logger.info(logVersion());
        if (taskArgs != null) {
            if (!initialized) {
                initialize(taskArgs);
            }

            configuration.executeTask();
            logger.info("WebLogic monitor run completed.");
            return new TaskOutput("WebLogic monitor run completed.");
        }
        throw new TaskExecutionException("WebLogic Monitor completed with failures");
    }

    private void initialize(Map<String, String> taskArgs) {
        if (!initialized) {
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            ClassLoader fileSystemLoader = Thread.currentThread().getContextClassLoader();
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(fileSystemLoader), metricWriteHelper);

            conf.setConfigYml(configFilePath);

            conf.checkIfInitialized(ConfItem.CONFIG_YML, ConfItem.EXECUTOR_SERVICE, ConfItem.METRIC_PREFIX, ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {
        ClassLoader extensionClassLoader;
        public TaskRunnable(ClassLoader extensionClassLoader){
            this.extensionClassLoader = extensionClassLoader;
        }
        public void run() {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);
            Map<String, ?> config = configuration.getConfigYml();
            if(config != null) {
                List<Map> servers = (List) config.get("servers");
                if(servers != null && !servers.isEmpty()) {
                    for(Map server : servers) {
                        WebLogicMonitorTask task = new WebLogicMonitorTask(configuration, server, extensionClassLoader);
                        configuration.getExecutorService().execute(task);
                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                if (config == null) {
                    logger.error("The config.yml is not loaded due to previous errors.The task will not run");
                }
            }
        }
    }

    private static String getImplementationVersion() {
        return WebLogicMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        return msg;
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        org.apache.log4j.Logger.getRootLogger().addAppender(ca);

        final WebLogicMonitor monitor = new WebLogicMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "src/main/resources/conf/config.yml");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 1, 10, TimeUnit.SECONDS);

    }
}

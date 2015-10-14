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

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.util.MetricUtils;
import com.appdynamics.extensions.weblogic.config.Configuration;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Created by balakrishnav on 10/9/15.
 */
public class WebLogicMonitor extends AManagedMonitor {
    public static final Logger logger = LoggerFactory.getLogger(WebLogicMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/WebLogicMonitor/config.yml";

    public WebLogicMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    private static String getImplementationVersion() {
        return WebLogicMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting " + getImplementationVersion() + " Monitoring Task");
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);

                WebLogicMonitorTask webLogicMonitorTask = new WebLogicMonitorTask();
                Map<String, Object> metrics = webLogicMonitorTask.collectMetrics(config);

                printMetrics(config, metrics);

                logger.info("Completed the WebLogic Monitoring Task successfully");
                return new TaskOutput("WebLogic Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("WebLogic Monitor completed with failures");
    }

    private void printMetrics(Configuration config, Map<String, Object> metrics) {
        String metricPath = config.getMetricPrefix();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            printAverageAverageIndividual(metricPath + entry.getKey(), entry.getValue());
        }
    }

    private void printAverageAverageIndividual(String metricName, Object metricValue) {
        if (metricValue != null) {
            MetricWriter metricWriter = getMetricWriter(metricName, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            try {
                metricWriter.printMetric(MetricUtils.toWholeNumberString(metricValue));
                if (logger.isDebugEnabled()) {
                    logger.debug(metricName + " = " + metricValue);
                }
            } catch (Exception e) {
                logger.error("Error while reporting metric {}:{} ", metricName, metricValue, e);
            }
        }
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }
}

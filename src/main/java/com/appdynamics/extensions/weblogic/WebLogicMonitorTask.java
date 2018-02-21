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
import com.appdynamics.extensions.weblogic.config.JMXConnectionConfig;
import com.appdynamics.extensions.weblogic.config.JMXConnectionUtil;
import com.appdynamics.extensions.weblogic.metrics.MetricPropertiesBuilder;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.weblogic.ConfigConstants.OBJECT_NAME;

/**
 * Created by balakrishnav on 13/10/15.
 */
public class WebLogicMonitorTask implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(WebLogicMonitorTask.class);
    public static final String METRICS_SEPARATOR = "|";
    private JMXConnectionUtil jmxConnector;

    private MonitorConfiguration configuration;
    private Map server;
    private ClassLoader extensionClassLoader;

    public WebLogicMonitorTask(MonitorConfiguration configuration, Map server, ClassLoader extensionClassLoader) {
        this.configuration = configuration;
        this.server = server;
        this.extensionClassLoader = extensionClassLoader;
    }

    public static Long format(Long value) {
        return value / 1024 / 1024;
    }

    public Map<String, Object> collectMetrics(Map server) throws Exception {
        Map<String, Object> webLogicMetrics = Maps.newHashMap();
        String username = (String) server.get("username");
        String password = (String) server.get("password");
        String jmxServiceUrl = (String) server.get("jmxServiceUrl");
        List<Map> configMBeans = (List<Map>) server.get("mbeans");
        Map environmentAttributes = populateEnvironmentAttrib(server);

        JMXConnectionConfig connectionConfig = new JMXConnectionConfig(jmxServiceUrl, username, password);
        jmxConnector = new JMXConnectionUtil(connectionConfig);
        jmxConnector.connect(environmentAttributes);

        MetricPropertiesBuilder propertyBuilder = new MetricPropertiesBuilder();

        for(Map aConfigMBean : configMBeans){
            String configObjectName = (String) aConfigMBean.get(OBJECT_NAME);
            logger.debug("Processing mbean %s from the config file",configObjectName);
            try {
                ObjectName objectName = new ObjectName(configObjectName);
                List<String> strings = (List<String>) aConfigMBean.get("metricPathFromObjectNameProperties");
                String metricPathForObjectName = buildMetricPathForObjectName((String) server.get("displayName"), objectName, strings);
                Map<String, String> metricPropsMap = propertyBuilder.build(aConfigMBean);
                for (Map.Entry<String, String> entry : metricPropsMap.entrySet()) {
                    String metricName = entry.getKey();
                    Object metricValue = jmxConnector.getAttribute(objectName, metricName);
                    webLogicMetrics.put(metricPathForObjectName + metricName, metricValue);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        }

        jmxConnector.close();
        return webLogicMetrics;
    }

    private String buildMetricPathForObjectName(String displayName, ObjectName objectName, List<String> strings) {
        StringBuilder sb = new StringBuilder(displayName).append("|");
        if (strings == null || strings.isEmpty()) {
            return sb.toString();
        }
        for (String key : strings) {
            String propertyValue = objectName.getKeyProperty(key);
            if (!Strings.isNullOrEmpty(propertyValue)) {
                sb.append(propertyValue).append("|");
            } else {
                logger.warn("The property for " + key + " in objectName " + objectName + " from metricPathFromObjectNameProperties doesn't exist");
            }
        }
        return sb.toString();
    }

    private Map populateEnvironmentAttrib(Map server) {
        Map environmentAttributes = Maps.newHashMap();
        String jmxPackage = (String) server.get("jmx.remote.protocol.provider.pkgs");
        if (!Strings.isNullOrEmpty(jmxPackage)) {
            environmentAttributes.put("jmx.remote.protocol.provider.pkgs", jmxPackage);
        }
        Integer timeout = (Integer) server.get("jmx.remote.x.request.waiting.timeout");
        if (timeout != null) {
            environmentAttributes.put("jmx.remote.x.request.waiting.timeout", timeout.longValue());
        }
        return environmentAttributes;
    }

    public void run() {

        Thread.currentThread().setContextClassLoader(extensionClassLoader);

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> metrics = collectMetrics(server);
            printMetrics(metrics);
        } catch (Exception e) {
            logger.error("WebLogic monitor has errors for " + server.get("host"), e);
            //System.out.println("WebLogic monitor has errors for " + server.get("host")+ e);
        } finally {
            long endTime = System.currentTimeMillis() - startTime;
            logger.info("WebLogic monitor thread for server " + server.get("displayName") + " ended. Time taken is " + endTime);
            //System.out.println("WebLogic monitor thread for server " + server.get("displayName") + " ended. Time taken is " + endTime);
        }
    }

    private void printMetrics(Map<String, Object> metrics) {
        String metricPath = configuration.getMetricPrefix();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            printAverageAverageIndividual(metricPath + "|" + entry.getKey(), entry.getValue());
        }
    }

    private void printAverageAverageIndividual(String metricName, Object metricValue) {
        if (metricValue != null) {
            if (metricValue instanceof Integer) {
                configuration.getMetricWriter().printMetric(metricName, new BigDecimal((Integer) metricValue), "AVG.AVG.COL");
            } else if (metricValue instanceof Long) {
                configuration.getMetricWriter().printMetric(metricName, new BigDecimal((Long) metricValue), "AVG.AVG.COL");
            }
            System.out.println(metricName + " = " + metricValue);
        }
    }
}

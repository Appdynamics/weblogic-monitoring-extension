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

import com.appdynamics.extensions.weblogic.config.Configuration;
import com.appdynamics.extensions.weblogic.config.JMXConnectionConfig;
import com.appdynamics.extensions.weblogic.config.JMXConnectionUtil;
import com.appdynamics.extensions.weblogic.config.Server;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by balakrishnav on 13/10/15.
 */
public class WebLogicMonitorTask {
    public static final Logger logger = Logger.getLogger(WebLogicMonitorTask.class);
    public static final String METRICS_SEPARATOR = "|";
    private JMXConnectionUtil jmxConnector;

    public static long format(Long value) {
        return value / 1024 / 1024;
    }

    public Map<String, Object> collectMetrics(Configuration config) throws Exception {
        Map<String, Object> webLogicMetrics = Maps.newHashMap();
        Server server = config.getServer();
        JMXConnectionConfig connectionConfig = new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(), server.getPassword(), "t3");
        jmxConnector = new JMXConnectionUtil(connectionConfig);
        jmxConnector.connect();

        fetchComponentStats(webLogicMetrics);
        fetchJdbcStats(webLogicMetrics);
        fetchJMSQueueStats(webLogicMetrics);
        fetchJMSRuntimeStats(webLogicMetrics);
        fetchJTAStats(webLogicMetrics);
        fetchJVMStats(webLogicMetrics);

        jmxConnector.close();
        return webLogicMetrics;
    }

    private void fetchJVMStats(Map<String, Object> metrics) {
        String metricPrefix = "JVM" + METRICS_SEPARATOR;
        long heapSizeMax;
        long heapSizeCurrent;
        long heapFreeCurrent;
        long heapUsedCurrent;
        try {
            ObjectName jvmRuntimeMbean = jmxConnector.getMBean("JVMRuntime");
            heapSizeMax = format((Long) jmxConnector.getAttribute(jvmRuntimeMbean, "HeapSizeMax"));
            heapSizeCurrent = format((Long) jmxConnector.getAttribute(jvmRuntimeMbean, "HeapSizeCurrent"));
            heapFreeCurrent = format((Long) jmxConnector.getAttribute(jvmRuntimeMbean, "HeapFreeCurrent"));
            heapUsedCurrent = heapSizeCurrent - heapFreeCurrent;
            metrics.put(metricPrefix + "HeapSizeMax (MB)", heapSizeMax);
            metrics.put(metricPrefix + "HeapSizeCurrent (MB)", heapSizeCurrent);
            metrics.put(metricPrefix + "HeapFreeCurrent (MB)", heapFreeCurrent);
            metrics.put(metricPrefix + "UsedMemory (MB)", heapUsedCurrent);
        } catch (Exception e) {
            logger.error("Error while fetching JVM stats", e);
        }
    }

    private void fetchJTAStats(Map<String, Object> metrics) {
        int activeTransactionsTotalCount;
        try {
            ObjectName jtaRuntimeMbean = jmxConnector.getMBean("JTARuntime");
            activeTransactionsTotalCount = (Integer) jmxConnector.getAttribute(jtaRuntimeMbean, "ActiveTransactionsTotalCount");
            metrics.put("ActiveTransactionsTotalCount", activeTransactionsTotalCount);
        } catch (Exception e) {
            logger.error("Error while fetching Transactions count", e);
        }
    }

    private void fetchJMSRuntimeStats(Map<String, Object> metrics) {
        long connectionsCurrentCount;
        try {
            ObjectName jmsRuntimeMbean = jmxConnector.getMBean("JMSRuntime");
            connectionsCurrentCount = (Long) jmxConnector.getAttribute(jmsRuntimeMbean, "ConnectionsCurrentCount");
            metrics.put("JmsRuntime" + METRICS_SEPARATOR + "ConnectionsCurrentCount", connectionsCurrentCount);
        } catch (Exception e) {
            logger.error("Error while fetching JMS stats", e);
        }
    }

    private void fetchJMSQueueStats(Map<String, Object> metrics) {
        String metricPrefix = "JmsQueue" + METRICS_SEPARATOR;
        String destinationName;
        long messagesCurrentCount;
        long messagesPendingCount;
        long consumersCurrentCount;
        try {
            ObjectName jmsRuntimeMbean = jmxConnector.getMBean("JMSRuntime");
            ObjectName[] jmsServerRuntimeMbeans = jmxConnector.getMBeans(jmsRuntimeMbean, "JMSServers");
            for (ObjectName jmsServerRuntime : jmsServerRuntimeMbeans) {
                ObjectName[] jmsDestinationRuntimeMbeans = jmxConnector.getMBeans(jmsServerRuntime, "Destinations");
                for (ObjectName jmsDestinationRuntime : jmsDestinationRuntimeMbeans) {
                    destinationName = (String) jmxConnector.getAttribute(jmsDestinationRuntime, "Name");
                    messagesCurrentCount = (Long) jmxConnector.getAttribute(jmsDestinationRuntime, "MessagesCurrentCount");
                    messagesPendingCount = (Long) jmxConnector.getAttribute(jmsDestinationRuntime, "MessagesPendingCount");
                    consumersCurrentCount = (Long) jmxConnector.getAttribute(jmsDestinationRuntime, "ConsumersCurrentCount");
                    metrics.put(metricPrefix + destinationName + METRICS_SEPARATOR + "MessagesCurrentCount", messagesCurrentCount);
                    metrics.put(metricPrefix + destinationName + METRICS_SEPARATOR + "MessagesPendingCount", messagesPendingCount);
                    metrics.put(metricPrefix + destinationName + METRICS_SEPARATOR + "ConsumersCurrentCount", consumersCurrentCount);
                }
            }
        } catch (Exception e) {
            logger.error("Error while fetching JMSQueue stats", e);
        }
    }

    private void fetchJdbcStats(Map<String, Object> metrics) {
        int capacity;
        int activeCount;
        int waitingCount;
        String metricPrefix = "JDBC" + METRICS_SEPARATOR;
        try {
            ObjectName jdbcServiceRuntimeMbean = jmxConnector.getMBean("JDBCServiceRuntime");
            ObjectName[] jdbcDataSourceRuntimeMbeans = jmxConnector.getMBeans(jdbcServiceRuntimeMbean, "JDBCDataSourceRuntimeMBeans");
            for (ObjectName datasourceRuntime : jdbcDataSourceRuntimeMbeans) {
                String datasourceName = (String) jmxConnector.getAttribute(datasourceRuntime, "Name");

                capacity = (Integer) jmxConnector.getAttribute(datasourceRuntime, "CurrCapacity");
                activeCount = (Integer) jmxConnector.getAttribute(datasourceRuntime, "ActiveConnectionsCurrentCount");
                waitingCount = (Integer) jmxConnector.getAttribute(datasourceRuntime, "WaitingForConnectionCurrentCount");
                metrics.put(metricPrefix + datasourceName + METRICS_SEPARATOR + "CurrCapacity", capacity);
                metrics.put(metricPrefix + datasourceName + METRICS_SEPARATOR + "ActiveConnectionsCurrentCount", activeCount);
                metrics.put(metricPrefix + datasourceName + METRICS_SEPARATOR + "WaitingForConnectionCurrentCount", waitingCount);
            }
        } catch (Exception e) {
            logger.error("Error while fetching JDBC stats", e);
        }
    }

    private void fetchComponentStats(Map<String, Object> metrics) {
        List<String> EXCLUSIONS = Collections.unmodifiableList(Arrays.asList(
                "_async",
                "bea_wls_deployment_internal",
                "bea_wls_cluster_internal",
                "bea_wls_diagnostics",
                "bea_wls_internal",
                "console",
                "consolehelp",
                "uddi",
                "uddiexplorer"));
        try {
            ObjectName[] applicationRuntimeMbeans = jmxConnector.getMBeans("ApplicationRuntimes");
            int openSessions;
            for (ObjectName applicationRuntime : applicationRuntimeMbeans) {
                ObjectName[] componentRuntimeMbeans = jmxConnector.getMBeans(applicationRuntime, "ComponentRuntimes");
                for (ObjectName componentRuntime : componentRuntimeMbeans) {
                    String contextRoot;
                    try {
                        contextRoot = (String) jmxConnector.getAttribute(componentRuntime, "ContextRoot");
                        // The context root may be an empty string or a single character
                        if (contextRoot.length() > 1) {
                            contextRoot = contextRoot.substring(1);
                        }
                    } catch (AttributeNotFoundException ignored) {
                        // Our component is not an instance of WebAppComponentRuntimeMBean
                        continue;
                    }
                    if (EXCLUSIONS.contains(contextRoot)) {
                        continue;
                    }
                    openSessions = (Integer) jmxConnector.getAttribute(componentRuntime, "OpenSessionsCurrentCount");
                    metrics.put("Apps" + METRICS_SEPARATOR + contextRoot + METRICS_SEPARATOR + "OpenSessionsCurrentCount", openSessions);
                }
            }
        } catch (Exception e) {
            logger.error("Error while fetching Application stats", e);
        }
    }
}

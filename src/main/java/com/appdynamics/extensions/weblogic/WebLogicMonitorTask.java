/*
 *   Copyright 2019 . AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.weblogic;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.extensions.weblogic.commons.JMXConnectionAdapter;
import com.appdynamics.extensions.weblogic.metrics.JMXMetricsProcessor;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import javax.management.JMException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.weblogic.utils.Constants.*;

/**
 * Created by bhuvnesh.kumar on 2/23/18.
 */
public class WebLogicMonitorTask implements AMonitorTaskRunnable {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(WebLogicMonitorTask.class);
    private Boolean heartBeatStatus = true;
    private String metricPrefix; // take from context
    private MetricWriteHelper metricWriter;
    private Map<String, ?> server;
    private JMXConnectionAdapter jmxConnectionAdapter; // build here instead of
    private List<Map<String, ?>> configMBeans;
    private MonitorContextConfiguration monitorContextConfiguration;
    private ClassLoader fileSystemLoader;

    private String serverName;

    public WebLogicMonitorTask(MetricWriteHelper metricWriter, Map<String, ?> server, MonitorContextConfiguration monitorContextConfiguration, ClassLoader fileSystemLoader) {
        this.metricWriter = metricWriter;
        this.server = server;
        this.monitorContextConfiguration = monitorContextConfiguration;
        metricPrefix = monitorContextConfiguration.getMetricPrefix();
        configMBeans = (List<Map<String, ?>>) monitorContextConfiguration.getConfigYml().get(MBEANS);
        this.fileSystemLoader = fileSystemLoader;
    }

    private void getJMXConnectionAdapter() throws MalformedURLException {
        String serviceUrl = (String) server.get(SERVICEURL);

        String username = (String) server.get(USERNAME);
        String password = getPassword(server);

        if (!Strings.isNullOrEmpty(serviceUrl)) {
            jmxConnectionAdapter = JMXConnectionAdapter.create(serviceUrl, username, password);
        } else {
            throw new MalformedURLException();
        }
    }

    private String getPassword(Map server) {
        if (monitorContextConfiguration.getConfigYml().get(ENCRYPTION_KEY) != null) {
            String encryptionKey = (String) monitorContextConfiguration.getConfigYml().get(ENCRYPTION_KEY);
            server.put(ENCRYPTION_KEY, encryptionKey);
        }
        return CryptoUtils.getPassword(server);
    }

    private Map populateEnvironmentAttrib(Map server) {
        Map environmentAttributes = Maps.newHashMap();
        String jmxPackage = (String) server.get(JMX_REMOTE_PROTOCOL_PROVIDER_PACKAGE);
        if (!Strings.isNullOrEmpty(jmxPackage)) {
            environmentAttributes.put(JMX_REMOTE_PROTOCOL_PROVIDER_PACKAGE, jmxPackage);
        }
        Integer timeout = (Integer) server.get(JMX_REMOTE_REQUEST_WAITING_TIMEOUT);
        if (timeout != null) {
            environmentAttributes.put(JMX_REMOTE_REQUEST_WAITING_TIMEOUT, timeout.longValue());
        }
        return environmentAttributes;
    }


    public void run() {
        Thread.currentThread().setContextClassLoader(fileSystemLoader);
        serverName = (String) server.get(DISPLAY_NAME);
        try {
            getJMXConnectionAdapter();
            logger.debug("WebLogic monitoring task initiated for server {}", serverName);
            populateAndPrintStats();
        } catch (MalformedURLException e) {
            logger.error("Cannot construct WebLogic JMX uri for " + server.get(DISPLAY_NAME).toString(), e);
            heartBeatStatus = false;
        } catch (Exception e) {
            logger.error("Error in WebLogic Monitoring Task for Server {}", serverName, e);
            heartBeatStatus = false;
        } finally {
            logger.debug("WebLogic Monitoring Task Complete for Server {}", serverName);
        }
    }

    private void populateAndPrintStats() {
        JMXConnector jmxConnector = null;
        try {
            long previousTimestamp = System.currentTimeMillis();
            jmxConnector = jmxConnectionAdapter.open(populateEnvironmentAttrib(server));
            long currentTimestamp = System.currentTimeMillis();
            logger.debug("Time to open connection for " + serverName + " in milliseconds: " + (currentTimestamp - previousTimestamp));

            for (Map<String, ?> mBean : configMBeans) {
                String configObjName = (String) mBean.get(OBJECT_NAME);
                logger.debug("Processing mBean {} from the config file", configObjName);
                try {
                    JMXMetricsProcessor jmxMetricsProcessor = new JMXMetricsProcessor(monitorContextConfiguration,
                            jmxConnectionAdapter, jmxConnector);
                    List<Metric> nodeMetrics = jmxMetricsProcessor.getJMXMetrics(mBean,
                            metricPrefix, serverName);
                    if (nodeMetrics.size() > 0) {
                        metricWriter.transformAndPrintMetrics(nodeMetrics);
                    } else {
                        logger.debug("No metrics being sent from mBean : {} and server: {}", configObjName, serverName);
                    }
                } catch (JMException e) {
                    logger.error("JMException Occurred for {} " + configObjName, e);
                    heartBeatStatus = false;
                } catch (IOException e) {
                    logger.error("IOException occurred while getting metrics for mBean : {} and server: {} ", configObjName, serverName, e);
                    heartBeatStatus = false;
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching metrics from Server : " + serverName, e);
            heartBeatStatus = false;
        } finally {
            try {
                jmxConnectionAdapter.close(jmxConnector);
                logger.debug("WebLogic JMX connection is closed for " + serverName);
            } catch (IOException e) {
                logger.error("Unable to close the WebLogic JMX connection.", e);
            }
        }
    }

    public void onTaskComplete() {
        logger.debug("Task Complete");
        String metricValue = heartBeatStatus ? "1" : "0";
        metricWriter.printMetric(metricPrefix + METRICS_SEPARATOR + server.get(DISPLAY_NAME).toString() + METRICS_SEPARATOR + HEARTBEAT, metricValue, "AVERAGE", "AVERAGE", "INDIVIDUAL");
    }
}

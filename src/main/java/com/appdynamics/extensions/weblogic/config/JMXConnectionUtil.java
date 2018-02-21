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
package com.appdynamics.extensions.weblogic.config;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by balakrishnav on 12/10/15.
 */
public class JMXConnectionUtil {
    public static final Logger logger = LoggerFactory.getLogger(JMXConnectionUtil.class);
    private JMXConnectionConfig config;
    private MBeanServerConnection connection;
    private JMXConnector connector;


    public JMXConnectionUtil(JMXConnectionConfig config) {
        this.config = config;
    }

    public void connect(Map<String, Object> env) throws IOException {
        JMXServiceURL url = new JMXServiceURL(config.getJmxServiceUrl());

        Map environment = new HashMap();
        if (!Strings.isNullOrEmpty(this.config.getUsername())) {
            environment.put("jmx.remote.credentials", new String[]{this.config.getUsername(), this.config.getPassword()});
        }
        if (!env.isEmpty()) {
            environment.putAll(env);
        }
        this.connector = JMXConnectorFactory.connect(url, environment);
        if (this.connector != null) {
            this.connection = this.connector.getMBeanServerConnection();
        }
    }

    public ObjectName getObjectNameForRootMBean(String objectName, String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException {
        final ObjectName service;
        try {
            service = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e.getMessage());
        }
        return (ObjectName) connection.getAttribute(service, attribute);
    }

    public ObjectName[] getMBeans(ObjectName mbean, String query) throws Exception {
        return (ObjectName[]) connection.getAttribute(mbean, query);
    }

    public Object getAttribute(ObjectName mbean, String attribute) throws Exception {
        return connection.getAttribute(mbean, attribute);
    }

    public ObjectName getMBean(ObjectName mbean, String query) throws Exception {
        return (ObjectName) connection.getAttribute(mbean, query);
    }

    public Set<ObjectInstance> queryMBeans(ObjectName objectName) throws IOException {
        return connection.queryMBeans(objectName, null);
    }

    public void close() throws IOException {
        if (this.connector != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Closing the connection");
            }
            this.connector.close();
        }
    }
}

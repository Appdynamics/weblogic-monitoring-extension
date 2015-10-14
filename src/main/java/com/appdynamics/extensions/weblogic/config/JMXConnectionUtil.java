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
import org.apache.log4j.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by balakrishnav on 12/10/15.
 */
public class JMXConnectionUtil {
    public static final Logger logger = Logger.getLogger(JMXConnectionUtil.class);
    private static final String JNDI_NAME = "/jndi/weblogic.management.mbeanservers.runtime";
    private JMXConnectionConfig config;
    private MBeanServerConnection connection;
    private JMXConnector connector;
    private ObjectName serverRuntimeMBean;


    public JMXConnectionUtil(JMXConnectionConfig config) {
        this.config = config;
    }

    public void connect() throws IOException {
        JMXServiceURL url = new JMXServiceURL(this.config.getProtocol(), this.config.getHost(), this.config.getPort(), JNDI_NAME);
        HashMap env = new HashMap();
        if (!Strings.isNullOrEmpty(this.config.getUsername())) {
            env.put(Context.SECURITY_PRINCIPAL, this.config.getUsername());
            env.put(Context.SECURITY_CREDENTIALS, this.config.getPassword());
            env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,
                    "weblogic.management.remote");
            this.connector = JMXConnectorFactory.connect(url, env);
        }
        if (this.connector != null) {
            this.connection = this.connector.getMBeanServerConnection();
        }
    }

    public ObjectName getServerRuntimeMBean() throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException {
        final ObjectName service;
        try {
            service = new ObjectName("com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean");
        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e.getMessage());
        }
        serverRuntimeMBean = (ObjectName) connection.getAttribute(service, "ServerRuntime");
        return serverRuntimeMBean;
    }

    public ObjectName[] getMBeans(String query) throws Exception {
        return (ObjectName[]) connection.getAttribute(getServerRuntimeMBean(), query);
    }

    public ObjectName[] getMBeans(ObjectName mbean, String query) throws Exception {
        return (ObjectName[]) connection.getAttribute(mbean, query);
    }

    public Object getAttribute(ObjectName mbean, String attribute) throws Exception {
        return connection.getAttribute(mbean, attribute);
    }

    public ObjectName getMBean(String query) throws Exception {
        return (ObjectName) connection.getAttribute(serverRuntimeMBean, query);
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

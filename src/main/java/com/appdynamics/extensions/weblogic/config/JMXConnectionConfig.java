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

/**
 * Created by balakrishnav on 12/10/15.
 */
public class JMXConnectionConfig {

    private String username;
    private String password;
    private String jmxServiceUrl;

    public JMXConnectionConfig(String jmxServiceUrl, String username, String password) {
        this.username = username;
        this.password = password;
        this.jmxServiceUrl = jmxServiceUrl;
    }

    public String getJmxServiceUrl() {
        return jmxServiceUrl;
    }

    public void setJmxServiceUrl(String jmxServiceUrl) {
        this.jmxServiceUrl = jmxServiceUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}

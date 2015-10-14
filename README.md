# AppDynamics WebLogic Monitoring Extension
AppDynamics extension to monitor Oracle WebLogic Server

This extension works only with the standalone machine agent.

##Use Case

Oracle WebLogic Server is an application server for building and deploying enterprise Java EE applications. This eXtension aims to provide JMX monitoring capabilities for WebLogic servers and reports useful statistics ts to AppDynamics Controller.

##Pre-requisites
1. This extension has a dependency on `wlclient.jar` and `wljmxclient.jar` found in `WL_HOME\lib\` where WL_HOME is the directory in which you installed WebLogic Server. Copy these jars to lib directory.
2. Enable JMX capability in your WebLogic instance. Refer Troubleshooting section for more details.

##Installation

1. To build from source, clone this repository and run 'mvn clean install'. This will produce a WebLogicMonitor-VERSION.zip in the target directory. Alternatively, download the latest release archive from [Github](https://github.com/Appdynamics/weblogic-monitoring-extension/releases).
2. Copy and unzip WebLogicMonitor.zip from 'target' directory into `<machine_agent_dir>/monitors/`
3. Configure the extension by referring to the below section.
5. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | WebLogic in case of default metric path

## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the WebLogic Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/WebLogicMonitor/`.
2. Specify the WebLogic instance host, port, username and password in the config.yml.

   For eg.
   ```
       server:
           host: ""
           port:
           username: ""
           password: ""
       metricPrefix: "Custom Metrics|WebLogic|"
   ```

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/WebLogicMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/WebLogicMonitor/config.yml" />
          ....
     </task-arguments>
    ```



##Metrics

* JVM heap monitoring: HeapSizeMax, HeapSizeCurrent, HeapFreeCurrent, UsedMemory in MB
* JTA transactions monitoring: ActiveTransactionsTotalCount
* JDBC datasources monitoring: CurrCapacity, ActiveConnectionsCurrentCount, WaitingForConnectionCurrentCount
* JMS connections monitoring: ConnectionsCurrentCount
* JMS queues monitoring: MessagesCurrentCount, MessagesPendingCount, ConsumersCurrentCount
* HTTP sessions monitoring: OpenSessionsCurrentCount

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.
```
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

##Troubleshooting

1. Check if JMX is enabled by connecting remotely to WebLogic instance. cd to JAVA_HOME/bin directory to execute the following command
```
./jconsole -J-Djava.class.path=$JAVA_HOME/lib/jconsole.jar:$WL_HOME/server/lib/wljmxclient.jar -J-Djmx.remote.protocol.provider.pkgs=weblogic.management.remote
```
In the dialog box, enter the URL with host, port(`service:jmx:t3://<host>:<port>/jndi/weblogic.management.mbeanservers.runtime`), username and password to check the JMX connectivity.


##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](http://www.appdynamics.com/community/exchange/extension/weblogic-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
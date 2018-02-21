package com.appdynamics.extensions.weblogic.metrics;


import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.weblogic.ConfigConstants.INCLUDE;
import static com.appdynamics.extensions.weblogic.ConfigConstants.METRICS;


public class MetricPropertiesBuilder {

    public Map<String,String> build(Map aConfigMBean){
        Map<String,String> metricPropsMap = Maps.newHashMap();
        if(aConfigMBean == null || aConfigMBean.isEmpty()){
            return metricPropsMap;
        }
        Map configMetrics = (Map)aConfigMBean.get(METRICS);
        List includeMetrics = (List)configMetrics.get(INCLUDE);
        if(includeMetrics != null){
            for(Object metad : includeMetrics){
                Map localMetaData = (Map)metad;
                Map.Entry entry = (Map.Entry)localMetaData.entrySet().iterator().next();
                String metricName = entry.getKey().toString();
                String alias = entry.getValue().toString();


                metricPropsMap.put(metricName,alias);
            }
        }
        return metricPropsMap;
    }

}



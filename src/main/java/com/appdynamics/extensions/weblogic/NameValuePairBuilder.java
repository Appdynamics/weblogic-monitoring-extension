package com.appdynamics.extensions.weblogic;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class NameValuePairBuilder {
    public Map<String, Object> build(List propertiesList) {
        Map<String,Object> nameValueMap = Maps.newHashMap();
        if (propertiesList == null || propertiesList.isEmpty()) {
            return nameValueMap;
        }
        for(Object metad : propertiesList){
            Map localMetaData = (Map)metad;
            Map.Entry entry = (Map.Entry)localMetaData.entrySet().iterator().next();
            String key = entry.getKey().toString();
            Object value = entry.getValue();


            nameValueMap.put(key,value);
        }
        return nameValueMap;
    }
}

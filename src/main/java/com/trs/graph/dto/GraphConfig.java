package com.trs.graph.dto;

import java.util.Map;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 15:14
 **/
public class GraphConfig {
    private String graphName;
    private Map<String,String> properties;



    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}

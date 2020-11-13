package com.trs.graph;

import com.trs.graph.dto.GraphConfig;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.11 14:16
 **/
public class GraphConfigUtil {

    private final static String JANUS_GRAPH_FACTORY="org.janusgraph.core.JanusGraphFactory";


    public static void replaceJanusGraphFactory(GraphConfig config){
        String graphFactory = config.getProperties().get("gremlin.graph");
        if (JANUS_GRAPH_FACTORY.equals(graphFactory)){
            config.getProperties().put("gremlin.graph",TrsGraphFactory.class.getName());
        }
    }

}

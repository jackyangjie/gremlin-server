package com.trs.web.server.handler;

import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trs.exception.TrsGraphServerException;
import com.trs.graph.GraphConfigUtil;
import com.trs.graph.TrsGraphFactory;
import com.trs.graph.TrsGraphManager;
import com.trs.graph.dto.GraphConfig;
import com.trs.graph.dto.Result;
import org.apache.commons.configuration.MapConfiguration;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration;
import org.janusgraph.graphdb.configuration.builder.GraphDatabaseConfigurationBuilder;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 11:12
 **/
public class TrsGraphHandler extends BaseGraphHandler {

    private static final Logger log =  LoggerFactory.getLogger(TrsGraphHandler.class);
    public static final String URL_PREFIX = "/trs/graph/";

    private static final String ADD_GRAPH = "add_graph";

    private static final String REMOVE_GRAPH = "remove_graph";

    private static final String UPDATE_GRAPH_CONFIG = "update_graph";

    @Override
    protected Result proccess(HttpExchange exchange)  {
        Result result = new Result();
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith(ADD_GRAPH)){
                InputStream requestBody = exchange.getRequestBody();
                GraphConfig graphConfig = objectMapper.readValue(requestBody, GraphConfig.class);
                result = addGraph(graphConfig);
            }else if(path.endsWith(UPDATE_GRAPH_CONFIG)) {
                //TODO 修改图库的配置信息 添加 es/hbase的节点数量
                InputStream requestBody = exchange.getRequestBody();
                GraphConfig graphConfig = objectMapper.readValue(requestBody, GraphConfig.class);
                result = updateGraphConfig(graphConfig);
            }else if (path.endsWith(REMOVE_GRAPH)){
                Map<String, String> param = queryToMap(exchange.getRequestURI().getQuery());
                String graphName = param.get("graphName");
                if (graphName != null){
                    result = removeGraph(graphName);
                }
            }else{
                result.setCode(404);
                result.setMessage("该资源不存在!只有一下链接！");
                result.setData(Lists.newArrayList(URL_PREFIX+ADD_GRAPH,
                                            URL_PREFIX+UPDATE_GRAPH_CONFIG,
                                            URL_PREFIX+REMOVE_GRAPH));
            }
        } catch (Exception e) {
            throw new TrsGraphServerException("",e);
        }
        return result;
    }


    private Result addGraph(GraphConfig config){
        try {
            String graphName = config.getGraphName();
            GraphConfigUtil.replaceJanusGraphFactory(config);
            MapConfiguration mapConfiguration = new MapConfiguration(config.getProperties());
            CommonsConfiguration graphConfig = new CommonsConfiguration(mapConfiguration);
            TrsGraphManager.getInstance().openGraph(graphName,gName -> new StandardJanusGraph(new GraphDatabaseConfigurationBuilder().build(graphConfig)));
           log.info("添加{}图到 gremlinServer",config.getGraphName());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.ErrorResult();
        }
        return Result.SuccessResult();
    }

    private Result removeGraph(String graphName){
        try {
            JanusGraph graph = (JanusGraph)TrsGraphManager.getInstance().getGraph(graphName);
            TrsGraphFactory.drop(graph);
            log.info("从 gremlinServer 删除图{}",graphName);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.ErrorResult();
        }
        return Result.SuccessResult();
    }

    private Result updateGraphConfig(GraphConfig config){
//        GraphManager graphManager = TrsGraphManager.getInstance();
        return null;
    }
}

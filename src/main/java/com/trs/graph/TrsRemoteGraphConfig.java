package com.trs.graph;

import com.google.common.collect.Maps;
import com.trs.graph.dto.GraphConfig;
import com.trs.web.server.TrsWebServer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.janusgraph.core.JanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 14:16
 **/
public class TrsRemoteGraphConfig {

    private static final Logger log =  LoggerFactory.getLogger(TrsRemoteGraphConfig.class);


    private Map<String, Map<String,String>> trsGraphConfig = Maps.newHashMap();

    private final static String JANUS_GRAPH_FACTORY="org.janusgraph.core.JanusGraphFactory";

    private TrsClientApi trsClientApi;

    protected TrsRemoteGraphConfig(Settings settings){
        trsClientApi = new TrsClientApi(settings);
        initTrsRemoteGraphConfig();
        startTrsWebServer(settings);
    }


    private void initTrsRemoteGraphConfig(){
        log.info("开始加载远程图库配置...");
        List<GraphConfig> graphConfigList =trsClientApi.fetchGraphConfig();
        if (graphConfigList != null){
//            CountDownLatch downLatch = new CountDownLatch(graphConfigList.size());
            for (GraphConfig config:graphConfigList){
                if (config.getProperties().get("graph.graphname") == null){
                    config.getProperties().put("graph.graphname",config.getGraphName());
                }
                 AsyncTimeOut.supplyAsync(() -> {
                    GraphConfigUtil.replaceJanusGraphFactory(config);
                    Configuration mapConfiguration = new MapConfiguration(config.getProperties());
                    JanusGraph graph = TrsGraphFactory.open(mapConfiguration);
                    return true;
                }, 100, TimeUnit.SECONDS, false).whenComplete((r,e) ->{
                   if (r){
                       log.info("loading graph : {} success ", config.getGraphName());
                   }else{
                       log.info("loading graph : {} failed ", config.getGraphName());
                   }
//                     downLatch.countDown();
                });
            }
//            try {
//                downLatch.await();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            log.info("加载远程图库配置完成！");
        }

    }

    /**
     * TODO 这里的地址和端口 占时选这样写，以后可以改成从 图库管理平台 获取
     * @param settings
     */
    private void startTrsWebServer(Settings settings){
        int port = settings.port;
        //web 端口默认为 图库端口 + 100
        TrsWebServer trsWebServer = new TrsWebServer(null, port+100);
        trsWebServer.start();
    }

    /**
     * 用于设置任务超时
     */
 public static class AsyncTimeOut{
     private static final ScheduledExecutorService schedulerExecutor =  Executors.newSingleThreadScheduledExecutor();
     public static < T > CompletableFuture< T > supplyAsync(
             final Supplier< T > supplier, long timeoutValue, TimeUnit timeUnit,
             T defaultValue) {
         CompletableFuture<T> completableFuture = CompletableFuture.supplyAsync(() -> supplier.get());
         //schedule watcher
         schedulerExecutor.schedule(() -> {
             if (!completableFuture.isDone()) {
                 completableFuture.complete(defaultValue);
                 completableFuture.cancel(true);
             }
         }, timeoutValue, timeUnit);
         return completableFuture;
     }
 }

}

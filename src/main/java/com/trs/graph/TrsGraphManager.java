package com.trs.graph;

import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.janusgraph.graphdb.management.utils.JanusGraphManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author yangjie
 * @Description  在 JanusGraphGraphManager 基础之上
 *  添加了，1.动态添加图库 删除图库
 *        2.初始化读取数据库 图库配置信息
 * @DATE 2020.11.9 17:02
 **/
public class TrsGraphManager implements GraphManager {

    private static final Logger log =
        LoggerFactory.getLogger(TrsGraphManager.class);
    public static final String TRS_GRAPH_MANAGER_EXPECTED_STATE_MSG
        = "Gremlin Server must be configured to use the TrsGraphManager.";

    private final Map<String, Graph> graphs = new ConcurrentHashMap<>();
    private final Map<String, TraversalSource> traversalSources = new ConcurrentHashMap<>();
    private final Object instantiateGraphLock = new Object();
    private GremlinExecutor gremlinExecutor = null;

    private static TrsGraphManager instance = null;

    protected static String TRS_GRAPH_REMOTE_CONFIG = "remoteGraphConfig";

    public TrsGraphManager(Settings settings) {
        initialize();

        // Open graphs defined at server start in settings.graphs
        for (Map.Entry<String,String> entry: settings.graphs.entrySet()) {
            //添加trs 远程的配置
            if (entry.getKey().toLowerCase().equals(TRS_GRAPH_REMOTE_CONFIG.toLowerCase())) {
                new TrsRemoteGraphConfig(settings);
            }else{ //默认图库加载方式
                final StandardJanusGraph graph = (StandardJanusGraph) TrsGraphFactory.open(entry.getValue(), entry.getKey());
                graphs.put(entry.getKey(),graph);
            }
        }

//        initGremlinExecutor(settings);
    }

    private void initGremlinExecutor(Settings settings){
        GremlinExecutor.Builder gremlinExecutorBuilder = GremlinExecutor.build()
                .globalBindings(this.getAsBindings());
//        settings.scriptEngines.forEach((k,v) ->gremlinExecutorBuilder.addPlugins(k,v.plugins));
        gremlinExecutor = gremlinExecutorBuilder.create();
        configureGremlinExecutor(gremlinExecutor);
    }

    private synchronized void initialize() {
        if (null != instance) {
            final String errMsg = "You may not instantiate a JanusGraphManager. The single instance should be handled by Tinkerpop's GremlinServer startup processes.";
            throw new JanusGraphManagerException(errMsg);
        }

        instance = this;
    }

    public static TrsGraphManager getInstance() {
        return instance;
    }

    // To be used for testing purposes only, so we can run tests in parallel
    public static TrsGraphManager getInstance(boolean forceCreate) {
        if (forceCreate) {
            return new TrsGraphManager(new Settings());
        } else {
            return instance;
        }
    }

    public void configureGremlinExecutor(GremlinExecutor gremlinExecutor) {
        this.gremlinExecutor = gremlinExecutor;
        final ScheduledExecutorService bindExecutor = Executors.newScheduledThreadPool(1);
        // Dynamically created graphs created with the ConfiguredGraphFactory are
        // bound across all nodes in the cluster and in the face of server restarts
        bindExecutor.scheduleWithFixedDelay(new GremlinExecutorGraphBinder(this, this.gremlinExecutor), 0, 20L, TimeUnit.SECONDS);
    }

    private class GremlinExecutorGraphBinder implements Runnable {
        final TrsGraphManager graphManager;
        final GremlinExecutor gremlinExecutor;

        public GremlinExecutorGraphBinder(TrsGraphManager graphManager, GremlinExecutor gremlinExecutor) {
            this.graphManager = graphManager;
            this.gremlinExecutor = gremlinExecutor;
        }

        @Override
        public void run() {
            for (Map.Entry<String,Graph> entry: graphs.entrySet()) {
                try {
                      updateTraversalSource(entry.getKey(), entry.getValue(), this.gremlinExecutor, this.graphManager);
                } catch (Exception e) {
                    // cannot open graph, do nothing
                    log.error(String.format("Failed to open graph %s with the following error:\n %s.\n" +
                            "Thus, it and its traversal will not be bound on this server.", entry.getKey(), e.toString()));
                }
            }
        }
    }

    // To be used for testing purposes
    protected static void shutdownJanusGraphManager() {
        instance = null;
    }

    @Override
    public Set<String> getGraphNames() {
        return graphs.keySet();
    }

    @Override
    public Graph getGraph(String gName) {
        return graphs.get(gName);
    }

    @Override
    public void putGraph(String gName, Graph g) {
        graphs.put(gName, g);
    }

    @Override
    public Set<String> getTraversalSourceNames() {
        return traversalSources.keySet();
    }

    @Override
    public TraversalSource getTraversalSource(String tsName) {
        return traversalSources.get(tsName);
    }

    @Override
    public void putTraversalSource(String tsName, TraversalSource ts) {
        traversalSources.put(tsName, ts);
    }

    @Override
    public TraversalSource removeTraversalSource(String tsName) {
        if (tsName == null){ return null;}
        return traversalSources.remove(tsName);
    }

    /**
     * Get the {@link Graph} and {@link TraversalSource} list as a set of bindings.
     */
    @Override
    public Bindings getAsBindings() {
        final Bindings bindings = new SimpleBindings();
        graphs.forEach(bindings::put);
        traversalSources.forEach(bindings::put);
        return bindings;
    }

    @Override
    public void rollbackAll() {
        graphs.forEach((key, graph) -> {
            if (graph.tx().isOpen()) {
                graph.tx().rollback();
            }
        });
    }

    @Override
    public void rollback(final Set<String> graphSourceNamesToCloseTxOn) {
        commitOrRollback(graphSourceNamesToCloseTxOn, false);
    }

    @Override
    public void commitAll() {
        graphs.forEach((key, graph) -> {
            if (graph.tx().isOpen()) {
                graph.tx().commit();
            }
        });
    }

    @Override
    public void commit(final Set<String> graphSourceNamesToCloseTxOn) {
        commitOrRollback(graphSourceNamesToCloseTxOn, true);
    }

    public void commitOrRollback(Set<String> graphSourceNamesToCloseTxOn, Boolean commit) {
        graphSourceNamesToCloseTxOn.forEach(e -> {
            final Graph graph = getGraph(e);
            if (null != graph) {
                closeTx(graph, commit);
            }
        });
    }

    public void closeTx(Graph graph, Boolean commit) {
        if (graph.tx().isOpen()) {
            if (commit) {
                graph.tx().commit();
            } else {
                graph.tx().rollback();
            }
        }
    }

    @Override
    public Graph openGraph(String gName, Function<String, Graph> thunk) {
        Graph graph = graphs.get(gName);
        if (graph != null && !((StandardJanusGraph) graph).isClosed()) {
            updateTraversalSource(gName, graph);
            return graph;
        } else {
            synchronized (instantiateGraphLock) {
                graph = graphs.get(gName);
                if (graph == null || ((StandardJanusGraph) graph).isClosed()) {
                    graph = thunk.apply(gName);
                    graphs.put(gName, graph);
                }
            }
            updateTraversalSource(gName, graph);
            return graph;
        }
    }

    @Override
    public Graph removeGraph(String gName) {
        if (gName == null){ return null;}
        String traversalName = gName + "_traversal";
        traversalSources.remove(traversalName);
        return graphs.remove(gName);
    }

    private void updateTraversalSource(String graphName, Graph graph){
        if (null != gremlinExecutor) {
            updateTraversalSource(graphName, graph, gremlinExecutor, this);
        }
    }

    private void updateTraversalSource(String graphName, Graph graph, GremlinExecutor gremlinExecutor,
                                       TrsGraphManager graphManager){
        gremlinExecutor.getScriptEngineManager().put(graphName, graph);
        String traversalName = graphName + "_traversal";
        TraversalSource traversalSource = graph.traversal();
        gremlinExecutor.getScriptEngineManager().put(traversalName, traversalSource);
        graphManager.putTraversalSource(traversalName, traversalSource);
    }

}
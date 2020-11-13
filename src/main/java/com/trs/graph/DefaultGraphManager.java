package com.trs.graph;

import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.11 19:03
 **/
public class DefaultGraphManager implements GraphManager {

        private static final Logger logger = LoggerFactory.getLogger(GremlinServer.class);

        private final Map<String, Graph> graphs = new ConcurrentHashMap<>();
        private final Map<String, TraversalSource> traversalSources = new ConcurrentHashMap<>();

        /**
         * Create a new instance using the {@link Settings} from Gremlin Server.
         */
    public DefaultGraphManager(final Settings settings) {
        settings.graphs.entrySet().forEach(e -> {
            try {
                final Graph newGraph = GraphFactory.open(e.getValue());
                graphs.put(e.getKey(), newGraph);
                logger.info("Graph [{}] was successfully configured via [{}].", e.getKey(), e.getValue());
            } catch (RuntimeException re) {
                logger.warn(String.format("Graph [%s] configured at [%s] could not be instantiated and will not be available in Gremlin Server.  GraphFactory message: %s",
                        e.getKey(), e.getValue(), re.getMessage()), re);
                if (re.getCause() != null) {logger.debug("GraphFactory exception", re.getCause());}
            }
        });
    }
    @Override
        public final Set<String> getGraphNames() {
        return graphs.keySet();
    }
    @Override
        public final Graph getGraph(final String graphName) {
        return graphs.get(graphName);
    }
    @Override
        public final void putGraph(final String graphName, final Graph g) {
        graphs.put(graphName, g);
    }
    @Override
        public final Set<String> getTraversalSourceNames() {
        return traversalSources.keySet();
    }

    @Override
        public final TraversalSource getTraversalSource(final String traversalSourceName) {
        return traversalSources.get(traversalSourceName);
    }
    @Override
        public final void putTraversalSource(final String tsName, final TraversalSource ts) {
        traversalSources.put(tsName, ts);
    }
    @Override
        public final TraversalSource removeTraversalSource(final String tsName) {
        return traversalSources.remove(tsName);
    }

        /**
         * Get the {@link Graph} and {@link TraversalSource} list as a set of bindings.
         */
        @Override
        public final Bindings getAsBindings() {
        final Bindings bindings = new SimpleBindings();
        graphs.forEach(bindings::put);
        traversalSources.forEach(bindings::put);
        return bindings;
    }

        /**
         * Rollback transactions across all {@link Graph} objects.
         */
        @Override
        public final void rollbackAll() {
        graphs.entrySet().forEach(e -> {
            final Graph graph = e.getValue();
            if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) {
                graph.tx().rollback();
            }
        });
    }

        /**
         * Selectively rollback transactions on the specified graphs or the graphs of traversal sources.
         */
        @Override
        public final void rollback(final Set<String> graphSourceNamesToCloseTxOn) {
        closeTx(graphSourceNamesToCloseTxOn, Transaction.Status.ROLLBACK);
    }

        /**
         * Commit transactions across all {@link Graph} objects.
         */
        @Override
        public final void commitAll() {
        graphs.entrySet().forEach(e -> {
            final Graph graph = e.getValue();
            if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) {
                graph.tx().commit();
            }
        });
    }

        /**
         * Selectively commit transactions on the specified graphs or the graphs of traversal sources.
         */
        @Override
        public final void commit(final Set<String> graphSourceNamesToCloseTxOn) {
        closeTx(graphSourceNamesToCloseTxOn, Transaction.Status.COMMIT);
    }

        /**
         * {@inheritDoc}
         */
        @Override
        public final Graph openGraph(final String graphName, final Function<String, Graph> supplier) {
        final Graph graph = graphs.get(graphName);
        if (null != graph) {
            return graph;
        }
        final Graph newGraph = supplier.apply(graphName);
        putGraph(graphName, newGraph);
        return newGraph;
    }

        /**
         * {@inheritDoc}
         */
        @Override
        public final Graph removeGraph(final String graphName) throws Exception {
        Graph graph = graphs.remove(graphName);
        graph.close();
        return graph;
    }

        /**
         * Selectively close transactions on the specified graphs or the graphs of traversal sources.
         */

        private void closeTx(final Set<String> graphSourceNamesToCloseTxOn, final Transaction.Status tx) {
        final Set<Graph> graphsToCloseTxOn = new HashSet<>();

        // by the time this method has been called, it should be validated that the source/graph is present.
        // might be possible that it could have been removed dynamically, but that i'm not sure how one would do
        // that as of right now unless they were embedded in which case they'd need to know what they were doing
        // anyway
        graphSourceNamesToCloseTxOn.forEach(r -> {
            if (graphs.containsKey(r)) {
                graphsToCloseTxOn.add(graphs.get(r));
            }else {
                graphsToCloseTxOn.add(traversalSources.get(r).getGraph());
            }
        });

        graphsToCloseTxOn.forEach(graph -> {
            if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) {
                if (tx == Transaction.Status.COMMIT) {
                    graph.tx().commit();
                }else {
                    graph.tx().rollback();
                }
            }
        });
    }

}

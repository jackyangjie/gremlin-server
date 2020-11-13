package com.trs.graph;

import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.structure.T;
import org.junit.Test;

import java.io.InputStream;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.11 10:48
 **/
public class TrsRemoteGraphConfigTest {

    @Test
    public void loadGraphConfig() throws Exception {
        InputStream resourceAsStream = TrsRemoteGraphConfigTest.class.getClassLoader().getResourceAsStream("gremlin-server.yaml");
        Settings settings = Settings.read(resourceAsStream);
        new TrsRemoteGraphConfig(settings);
    }

    public static void main(String[] args) throws Exception {
        TrsRemoteGraphConfigTest test =new TrsRemoteGraphConfigTest();
        test.loadGraphConfig();
    }
}

package com.trs.graph;

import org.apache.tinkerpop.gremlin.server.Settings;

import java.io.InputStream;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.11 20:47
 **/
public class TrsGraphManagerTest {
    public static void main(String[] args) {
        InputStream resourceAsStream = TrsRemoteGraphConfigTest.class.getClassLoader().getResourceAsStream("gremlin-server.yaml");
        Settings settings = Settings.read(resourceAsStream);
        new TrsGraphManager(settings);
    }
}

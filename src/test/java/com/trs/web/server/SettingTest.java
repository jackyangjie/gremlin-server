package com.trs.web.server;

import com.trs.graph.TrsRemoteGraphConfigTest;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.junit.Test;

import java.io.InputStream;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.11 8:46
 **/
public class SettingTest {

    @Test
    public void testRead() throws Exception {
        InputStream resourceAsStream = TrsRemoteGraphConfigTest.class.getClassLoader().getResourceAsStream("gremlin-server.yaml");
        Settings read = Settings.read(resourceAsStream);

    }
}

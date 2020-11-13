package com.trs.web.server;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 11:18
 **/
public class TrsWebServerTest {

    public static void main(String[] args) {
        TrsWebServer trsWebServer = new TrsWebServer("localhost", 8900);
        trsWebServer.start();
    }
}

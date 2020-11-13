package com.trs.web.server;

import com.sun.net.httpserver.HttpServer;
import com.trs.exception.TrsGraphServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author yangjie
 * @Description  http server 主要对外提供 gremlin Server 动态添加图库删除图库功能
 * @DATE 2020.11.10 10:40
 **/
public class TrsWebServer {
    private static final Logger log = LoggerFactory.getLogger(TrsWebServer.class);

    private HttpServer trsHttpServer ;
    private volatile static boolean RUNNING = false;

    public TrsWebServer(String address,int port){
        init(address,port);
    }


    private void init(String address,int port){
        if(port == 0){
            throw new TrsGraphServerException("端口不能为 0");
        }
        try {
            log.info("开始初始化 TrsHttpServer");
            if (address == null){
                trsHttpServer = HttpServer.create(new InetSocketAddress(port), 0);
            }else{
                trsHttpServer = HttpServer.create(new InetSocketAddress(address,port), 0);
            }
            TrsHttpContextConfig.registerHttpHandler(trsHttpServer);
        } catch (IOException e) {
            e.printStackTrace();
            throw  new TrsGraphServerException(e);
        }
    }


    public   void start(){
        if (trsHttpServer == null){
           throw new TrsGraphServerException("TrsHttpServer 还未初始化！");
        }
        synchronized(trsHttpServer){
            if (!RUNNING) {
                trsHttpServer.start();
                RUNNING = true;
                InetSocketAddress address = trsHttpServer.getAddress();
                log.info("TrsHttpServer start succeed! address= {}:{}",address.getHostString(),address.getPort());
            }
        }
    }


    public void stop(int second){
        if (RUNNING){
            trsHttpServer.stop(second);
            RUNNING = false;
        }
    }

}

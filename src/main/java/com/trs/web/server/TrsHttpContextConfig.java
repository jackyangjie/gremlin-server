package com.trs.web.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.trs.web.server.handler.DefaultHandler;
import com.trs.web.server.handler.TrsGraphHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 11:02
 **/

public class TrsHttpContextConfig {

   private static final Logger log = LoggerFactory.getLogger(TrsHttpContextConfig.class);

   public static Map<String, HttpHandler> httpHandlerMap = new ImmutableMap.Builder()
                      .put(DefaultHandler.DEFAULT_URL,new DefaultHandler())
                      .put(TrsGraphHandler.URL_PREFIX,new TrsGraphHandler())
                  .build();


   public static void registerHttpHandler(HttpServer httpServer){
      for (Map.Entry<String,HttpHandler> handlerEntry: httpHandlerMap.entrySet()){
         log.info("loading httpHandler {}",handlerEntry.getKey());
         httpServer.createContext(handlerEntry.getKey(),handlerEntry.getValue());

      }
   }
}

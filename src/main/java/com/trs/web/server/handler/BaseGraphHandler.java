package com.trs.web.server.handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trs.exception.TrsGraphServerException;
import com.trs.graph.dto.Result;
import com.trs.web.server.TrsHttpContextConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 16:13
 **/
public abstract class BaseGraphHandler implements HttpHandler {

    public static final String GET_REQUEST_METHOD = "get";

    public static final String POST_REQUEST_METHOD = "post";

    protected ObjectMapper objectMapper = new ObjectMapper();
    {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        OutputStream os = null;
        Result result = new Result();
        try {
            Headers responseHeaders = httpExchange.getResponseHeaders();
            responseHeaders.add("Content-type", "application/json;charset=UTF-8");
            String path = httpExchange.getRequestURI().getPath();
            if (!TrsHttpContextConfig.httpHandlerMap.keySet().contains(path)){
                result.setCode(404);
                result.setMessage("该URL:"+path+"不存在! 只存在一下资源");
                result.setData(TrsHttpContextConfig.httpHandlerMap.keySet());
            }
            try {
                result = proccess(httpExchange);
            } catch (TrsGraphServerException e) {
                e.printStackTrace();
                result.setCode(300);
                result.setMessage(e.getMessage());
            }
            byte[] bytes = objectMapper.writeValueAsBytes(result);
            httpExchange.sendResponseHeaders(200, bytes.length);
            os = httpExchange.getResponseBody();
            os.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            result.setCode(500);
            result.setMessage(e.getMessage());
        }finally {
            if (os != null){
                os.close();
            }
        }

    }

    protected Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

    /**
     * http请求处理类
     * @return
     */
    protected abstract Result proccess(HttpExchange httpExchange) ;
}

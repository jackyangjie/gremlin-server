package com.trs.web.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.trs.graph.TrsGraphManager;
import com.trs.graph.dto.Result;

import java.util.Date;
import java.util.Set;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 16:44
 **/
public class DefaultHandler extends BaseGraphHandler{
    public static final String DEFAULT_URL = "/";

    public static final String Traversal_URL = "/traversal";
    @Override
    protected Result proccess(HttpExchange httpExchange)  {
        Result<Set<String>> result = new Result();
        result.setCode(200);
        result.setSuccess(true);
        result.setMessage("TrsWebServer started ! "+new Date());
        if (TrsGraphManager.getInstance() != null) {
            if (httpExchange.getRequestURI().getPath().endsWith("/traversal")) {
                Set<String> traversalSourceNames = TrsGraphManager.getInstance().getTraversalSourceNames();
                result.setData(traversalSourceNames);
            } else {
                Set<String> graphNames = TrsGraphManager.getInstance().getGraphNames();
                result.setData(graphNames);
            }
        }
        return result;
    }
}

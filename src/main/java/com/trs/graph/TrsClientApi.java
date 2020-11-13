package com.trs.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trs.exception.TrsGraphServerException;
import com.trs.graph.dto.GraphConfig;
import com.trs.graph.dto.Result;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.*;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.11 9:15
 **/
public class TrsClientApi {
    private static final Logger log =  LoggerFactory.getLogger(TrsClientApi.class);


    private  String TRS_GRAPH_BASE_URL;
    private final static String LOGIN_URL="/graph/login";
    private final static String GRAPH_CONFIG_URL ="/graph/sys/accessible";
    private static ObjectMapper objectMapper = new ObjectMapper();
    private final static int SUCCESS = 200;
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }
    private HttpClient httpClient;
    private Settings settings;

    TrsClientApi(Settings settings){
        String remoteConfigUrl = settings.graphs.get(TrsGraphManager.TRS_GRAPH_REMOTE_CONFIG);
        if (StringUtils.isBlank(remoteConfigUrl)){
            throw new TrsGraphServerException("gremlin-server.yml graphs 中必须包含: "+TrsGraphManager.TRS_GRAPH_REMOTE_CONFIG);
        }
        this.TRS_GRAPH_BASE_URL = remoteConfigUrl;
        this.settings = settings;

    }


    private  Result<String> loginTrsGraph(){
        Object username = settings.authentication.config.get("username");
        Object password = settings.authentication.config.get("password");
        Object trsDbname = settings.authentication.config.get("trsDbname");
        RequestBuilder post = RequestBuilder.post(TRS_GRAPH_BASE_URL + LOGIN_URL);
        if (username!= null && password!= null && trsDbname != null){
            post.addParameter("username",username.toString())
                    .addParameter("password",password.toString())
                    .addParameter("dbname",trsDbname.toString());
        }else {
            throw new TrsGraphServerException("authentication.config.username,authentication.config.password,authentication.config.trsDbname 不能为空");
        }
        HttpUriRequest request = post.build();
        TypeReference<Result<String>> typeReference = new TypeReference<Result<String>>() {};
        Result<String> result = sendRequest(request, typeReference);
        return result;
    }


    private <T> Result<T>  sendRequest(HttpUriRequest request,TypeReference<Result<T>> type){
        try {
            httpClient = getHttpClient();
            String response = httpClient.execute(request, new BasicResponseHandler());
            System.out.println(response);
            log.info("获取图库配置信息 ：{}",request);
            Result<T> result = objectMapper.readValue(response,type);
            return result;
        } catch (HttpResponseException e) {
            log.error("error,code={},message={}", e.getStatusCode(), e.getMessage(), e);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new TrsGraphServerException("调用远程请求失败 ："+request.getURI().getPath(),e);
        }
        return new Result();
    }


    private HttpClient getHttpClient() {
        if (httpClient == null ) {
            HttpClientBuilder clientBuilder = HttpClients.custom();
            Object username = settings.authentication.config.get("username");
            Object password = settings.authentication.config.get("password");
            Object trsDbname = settings.authentication.config.get("trsDbname");
            if (username != null && password !=null){
                //表示普通 http认证
                if (trsDbname == null) {
                    CredentialsProvider provider = new BasicCredentialsProvider();
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username.toString(), password.toString());
                    provider.setCredentials(AuthScope.ANY, credentials);
                    clientBuilder.setDefaultCredentialsProvider(provider);
                }
            }
            httpClient = clientBuilder.build();
        }
        return httpClient;
    }

    public List<GraphConfig> fetchGraphConfig() {
        Result<String> loginResult = loginTrsGraph();
       if (!loginResult.getSuccess()){
           new TrsGraphServerException(loginResult.getMessage());
       }
        RequestBuilder get = RequestBuilder.get(TRS_GRAPH_BASE_URL + GRAPH_CONFIG_URL);
        HttpUriRequest request = get.build();
        TypeReference<Result<List<GraphConfig>>> typeReference = new TypeReference<Result<List<GraphConfig>>>() {};
        Result<List<GraphConfig>> result = sendRequest(request, typeReference);
        if (result.getCode() == SUCCESS || result.getSuccess()){
            return result.getData();
        }else{
            throw new TrsGraphServerException(result.getMessage()+" 请求地址："+request.getURI().getPath());
        }
    }
}

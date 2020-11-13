package com.trs.graph.dto;

import java.util.List;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 15:15
 **/
public class Result<T> {
    private Boolean success;
    private Integer code;
    private String message;
    private String exceptionClazz;
    private T data;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getExceptionClazz() {
        return exceptionClazz;
    }

    public void setExceptionClazz(String exceptionClazz) {
        this.exceptionClazz = exceptionClazz;
    }

    public static Result<String> SuccessResult(){
        Result result = new Result();
        result.setCode(200);
        result.setSuccess(true);
        result.setMessage("操作成功！");
        return result;
    }

    public static Result<String> ErrorResult(){
        Result result = new Result();
        result.setCode(500);
        result.setSuccess(false);
        result.setMessage("操作失败！");
        return result;
    }
}

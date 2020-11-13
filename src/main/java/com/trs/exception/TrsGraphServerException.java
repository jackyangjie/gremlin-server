package com.trs.exception;

/**
 * @author yangjie
 * @Description
 * @DATE 2020.11.10 10:48
 **/
public class TrsGraphServerException extends RuntimeException{

    public TrsGraphServerException(String message) {
        super(message);
    }

    public TrsGraphServerException(Throwable cause) {
        super(cause);
    }

    public TrsGraphServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

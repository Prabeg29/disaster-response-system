package com.coit20258.drs.server;

import java.io.Serializable;

public class DrsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final Object  data;
    private final String  errorMessage;

    private DrsResponse(boolean success, Object data, String errorMessage) {
        this.success      = success;
        this.data         = data;
        this.errorMessage = errorMessage;
    }

    public static DrsResponse ok(Object data) {
        return new DrsResponse(true, data, null);
    }

    public static DrsResponse error(String message) {
        return new DrsResponse(false, null, message);
    }

    public boolean isSuccess()       { return success; }
    public String  getErrorMessage() { return errorMessage; }

    @SuppressWarnings("unchecked")
    public <T> T getData()           { return (T) data; }
}

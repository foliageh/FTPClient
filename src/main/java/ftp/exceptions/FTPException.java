package ftp.exceptions;

import ftp.FTPReply;

public class FTPException extends Exception {
    private final int code;
    private String message;

    public FTPException(int code) {
        this.code = code;
    }

    public FTPException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public FTPException(FTPReply reply) {
        this.code = reply.getCode();
        this.message = reply.getMessage();
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

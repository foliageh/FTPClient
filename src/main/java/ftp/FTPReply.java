package ftp;

public class FTPReply {
    private final int code;
    private final String message;

    public FTPReply(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessCode() {
        return code >= 200 && code < 300;
    }

    public String toString() {
        return code + " " + message;
    }
}

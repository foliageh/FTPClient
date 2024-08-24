package ftp.exceptions;

public class FTPDataTransferException extends Exception {
    public FTPDataTransferException() {
        super();
    }

    public FTPDataTransferException(String message) {
        super(message);
    }

    public FTPDataTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}

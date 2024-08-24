package ftp;

import ftp.exceptions.FTPDataTransferException;

import java.io.IOException;
import java.net.Socket;

public interface FTPDataTransferChannel {
    Socket openConnection() throws IOException, FTPDataTransferException;

    default void dispose() {}
}

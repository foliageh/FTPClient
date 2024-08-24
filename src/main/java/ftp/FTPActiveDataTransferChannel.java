package ftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FTPActiveDataTransferChannel implements FTPDataTransferChannel {
    private final ServerSocket serverSocket;

    public FTPActiveDataTransferChannel() throws IOException {
        serverSocket = new ServerSocket(0);
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    public Socket openConnection() throws IOException {
        return serverSocket.accept();
    }

    @Override
    public void dispose() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }
}

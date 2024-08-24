package ftp;

import ftp.exceptions.FTPIllegalReplyException;

import java.io.*;
import java.net.Socket;

public class FTPCommunicationChannel {
    private final Socket connection;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public FTPCommunicationChannel(Socket connection) throws IOException {
        this.connection = connection;
        this.reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
    }

    public FTPReply readFTPReply() throws IOException, FTPIllegalReplyException {
        String line = reader.readLine();
        if (line == null)
            throw new IOException("FTP connection closed");
        if (line.length() < 3)
            throw new FTPIllegalReplyException();

        int code = Integer.parseInt(line.substring(0, 3));
        String message = line.substring(4);
        return new FTPReply(code, message);
    }

    public void sendFTPCommand(String command) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
}

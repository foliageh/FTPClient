package ftp;

import config.Configuration;
import ftp.exceptions.FTPDataTransferException;
import ftp.exceptions.FTPException;
import ftp.exceptions.FTPIllegalReplyException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPClient {
    private static final Pattern PASV_PATTERN = Pattern.compile("(?:\\d{1,3},){5}\\d{1,3}");
    private static final int DATA_TRANSFER_BUFFER_SIZE = Configuration.getIntProperty("ftp.dt-buffer-size", 8 * 1024);
    private static final int WELCOME_MESSAGES_COUNT = Configuration.getIntProperty("ftp.welcome-messages-count", 1);

    private String host;
    private int port = 21;
    private String username, password;
    private boolean connected;
    private boolean authenticated;
    private boolean activeMode = Configuration.getBooleanProperty("ftp.active-mode", false);
    private FTPCommunicationChannel communication;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isActiveMode() {
        return activeMode;
    }

    public void connect(String host) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        connect(host, port);
    }

    /**
     * @throws IllegalStateException If the client is already connected to a remote host.
     * @throws FTPException          If the server refuses the connection.
     */
    public void connect(String host, int port) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        if (connected)
            throw new IllegalStateException("Client already connected to " + host + " on port " + port);

        Socket connection = null;
        try {
            connection = new Socket(host, port);
            communication = new FTPCommunicationChannel(connection);

            // Returns welcome messages
            for (int i = 0; i < WELCOME_MESSAGES_COUNT; i++) {
                FTPReply r = communication.readFTPReply();
                if (!r.isSuccessCode())
                    throw new FTPException(r);
            }

            this.connected = true;
            this.host = host;
            this.port = port;
        } finally {
            if (!connected && connection != null) {
                try {
                    connection.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * @throws IllegalStateException If the client is not connected.
     * @throws FTPException          If login fails.
     */
    public void login(String username, String password) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException {
        if (!connected)
            throw new IllegalStateException("Client not connected");

        authenticated = false;

        boolean passwordRequired;

        communication.sendFTPCommand("USER " + username);
        FTPReply r = communication.readFTPReply();
        switch (r.getCode()) {
            case 230:
                passwordRequired = false;
                break;
            case 331:
                passwordRequired = true;
                break;
            default:
                throw new FTPException(r);
        }

        if (passwordRequired) {
            communication.sendFTPCommand("PASS " + password);
            r = communication.readFTPReply();
            if (!r.isSuccessCode())
                throw new FTPException(r);
        }

        this.authenticated = true;
        this.username = username;
        this.password = password;
    }

    /**
     * @throws IllegalStateException    If the client is not connected or not authenticated.
     * @throws FTPException             If the operation fails.
     * @throws FTPDataTransferException If a I/O occurs in the data transfer connection.
     */
    public void downloadTextualData(String filePath, OutputStream outputStream) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException {
        if (!connected)
            throw new IllegalStateException("Client not connected");
        if (!authenticated)
            throw new IllegalStateException("Client not authenticated");

        communication.sendFTPCommand("TYPE A");
        FTPReply r = communication.readFTPReply();
        if (!r.isSuccessCode())
            throw new FTPException(r);

        FTPDataTransferChannel dtChannel = openDataTransferChannel();
        communication.sendFTPCommand("RETR " + filePath);
        r = communication.readFTPReply();
        if (r.getCode() != 150 && r.getCode() != 125)
            throw new FTPException(r);

        try {
            Socket dtConnection;
            try {
                dtConnection = dtChannel.openConnection();
            } finally {
                dtChannel.dispose();
            }
            try (InputStreamReader reader = new InputStreamReader(dtConnection.getInputStream(), StandardCharsets.UTF_8)) {
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                char[] buffer = new char[DATA_TRANSFER_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                    writer.flush();
                }
            } catch (IOException e) {
                throw new FTPDataTransferException("I/O error in data transfer", e);
            } finally {
                try {
                    dtConnection.close();
                } catch (Throwable ignored) {
                }
            }
        } finally {
            // Consumes the result reply of the transfer
            r = communication.readFTPReply();
            if (r.getCode() != 226)
                throw new FTPException(r);
        }
    }

    /**
     * @throws IllegalStateException    If the client is not connected or not authenticated.
     * @throws FTPException             If the operation fails.
     * @throws FTPDataTransferException If a I/O occurs in the data transfer connection.
     */
    public void uploadTextualData(String filePath, InputStream inputStream) throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException {
        if (!connected)
            throw new IllegalStateException("Client not connected");
        if (!authenticated)
            throw new IllegalStateException("Client not authenticated");

        communication.sendFTPCommand("TYPE A");
        FTPReply r = communication.readFTPReply();
        if (!r.isSuccessCode())
            throw new FTPException(r);

        FTPDataTransferChannel dtChannel = openDataTransferChannel();
        communication.sendFTPCommand("STOR " + filePath);
        r = communication.readFTPReply();
        if (r.getCode() != 150 && r.getCode() != 125)
            throw new FTPException(r);

        try {
            Socket dtConnection;
            try {
                dtConnection = dtChannel.openConnection();
            } finally {
                dtChannel.dispose();
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(dtConnection.getOutputStream(), StandardCharsets.UTF_8)) {
                Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                char[] buffer = new char[DATA_TRANSFER_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                    writer.flush();
                }
            } catch (IOException e) {
                throw new FTPDataTransferException("I/O error in data transfer", e);
            } finally {
                try {
                    dtConnection.close();
                } catch (Throwable ignored) {
                }
            }
        } finally {
            // Consumes the result reply of the transfer.
            r = communication.readFTPReply();
            if (r.getCode() != 226)
                throw new FTPException(r);
        }
    }

    public void disconnect() throws IllegalStateException {
        if (!connected)
            throw new IllegalStateException("Client not connected");

        // Try sending QUIT, if it fails - whatever
        try {
            communication.sendFTPCommand("QUIT");
        } catch (IOException ignored) {
        }

        communication.close();
        communication = null;
        connected = false;
        authenticated = false;
    }

    private FTPDataTransferChannel openDataTransferChannel() throws IOException, FTPIllegalReplyException, FTPException {
        if (activeMode)
            try {
                return openActiveDataTransferChannel();
            } catch (IOException | FTPIllegalReplyException | FTPException e) {
                activeMode = false;
            }
        return openPassiveDataTransferChannel();
    }

    private FTPDataTransferChannel openActiveDataTransferChannel() throws IOException, FTPIllegalReplyException, FTPException {
        FTPActiveDataTransferChannel channel = new FTPActiveDataTransferChannel();
        int port = channel.getLocalPort();
        int[] addr = {127, 0, 0, 1}; // TODO add property for client ip in active mode

        try {
            communication.sendFTPCommand("PORT " + addr[0] + "," + addr[1] + "," + addr[2] + "," + addr[3] + "," + (port >>> 8) + "," + (port & 255));
            FTPReply r = communication.readFTPReply();
            if (!r.isSuccessCode())
                throw new FTPException(r);
        } catch (IOException | FTPIllegalReplyException e) {
            channel.dispose();
            throw e;
        }

        return channel;
    }

    private FTPDataTransferChannel openPassiveDataTransferChannel() throws IOException, FTPIllegalReplyException, FTPException {
        communication.sendFTPCommand("PASV");
        FTPReply r = communication.readFTPReply();
        if (!r.isSuccessCode())
            throw new FTPException(r);

        String hostAndPort;
        Matcher m = PASV_PATTERN.matcher(r.getMessage());
        if (m.find())
            hostAndPort = r.getMessage().substring(m.start(), m.end());
        else throw new FTPIllegalReplyException();

        // Parse the string extracted from pasv.
        String[] parts = hostAndPort.split(",");
        String pasvHost = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        int pasvPort = Integer.parseInt(parts[4]) << 8 | Integer.parseInt(parts[5]);

        return () -> {
            try {
                return new Socket(pasvHost, pasvPort);
            } catch (IOException e) {
                throw new FTPDataTransferException("Cannot connect to the remote server");
            }
        };
    }
}

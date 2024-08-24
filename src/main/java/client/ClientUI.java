package client;

import ftp.FTPClient;
import ftp.exceptions.FTPException;
import ftp.exceptions.FTPIllegalReplyException;

import java.io.IOException;


public class ClientUI {
    private final Console console = new Console();
    private FTPClient ftpClient;

    public void launch() {
        ftpClient = new FTPClient();
        if (!connectAndLogin())
            return;

        CommandHandler ch = new CommandHandler(console, ftpClient);
        ch.mainLoop();

        try {
            ftpClient.disconnect();
        } catch (IllegalStateException ignored) {
        }
    }

    public boolean connectAndLogin() {
        String host = console.input("FTP server IP address: ");
        try {
            ftpClient.connect(host);
        } catch (IOException | FTPIllegalReplyException | FTPException e) {
            console.error(e.getMessage());
            return false;
        }

        String username = console.input("Username: ");
        String password = console.inputPassword("Password: ");
        try {
            ftpClient.login(username, password);
        } catch (IOException | FTPIllegalReplyException | FTPException e) {
            console.error(e.getMessage());
            console.println("Unable to login.");
            return false;
        }

        return true;
    }
}

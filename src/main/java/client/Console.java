package client;

import java.util.Scanner;

public class Console {
    private final Scanner scanner = new Scanner(System.in);

    public void print(String s) {
        System.out.print(s);
    }

    public void println(String s) {
        System.out.println(s);
    }

    public void println(Object o) {
        System.out.println(o);
    }

    public void error(String s) {
        System.out.println("[!] " + s);
    }

    public String input() {
        return scanner.nextLine().trim();
    }

    public String input(String prompt) {
        print(prompt);
        return input();
    }

    public String inputPassword() {
        return inputPassword("");
    }

    public String inputPassword(String prompt) {
        print(prompt);
        if (System.console() != null)
            return new String(System.console().readPassword());
        else return input();
    }
}

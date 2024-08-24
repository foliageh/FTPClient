package client;

import entities.Student;
import ftp.FTPClient;
import ftp.exceptions.FTPDataTransferException;
import ftp.exceptions.FTPException;
import ftp.exceptions.FTPIllegalReplyException;
import services.StudentService;

import java.io.IOException;
import java.util.*;

public class CommandHandler {
    private final Console console;
    private final StudentService studentService;

    private final LinkedHashMap<String, Command> commands = new LinkedHashMap<String, Command>() {{
        Commands commands = new Commands();
        put("list", commands.LIST);
        put("student", commands.STUDENT);
        put("add", commands.ADD);
        put("remove", commands.REMOVE);
        put("help", commands.HELP);
        put("exit", commands.EXIT);
    }};

    public CommandHandler(Console console, FTPClient ftpClient) {
        this.console = console;
        this.studentService = new StudentService(ftpClient);
    }

    public void mainLoop() {
        console.println("\nWelcome to FTP Student Manager©!");
        executeCommand("help");

        while (true) {
            try {
                String commandName = console.input("$ ").trim();
                executeCommand(commandName);
            } catch (NoSuchElementException e) {
                // either Ctrl+C was pressed or exit command was executed
                console.println("Exiting...");
                break;
            }
        }
    }

    private void executeCommand(String commandName) {
        Command command = commands.get(commandName);
        if (command != null) {
            command.execute();
        } else {
            console.println("Unknown command. Type 'help' to see the list of available commands.");
        }
    }

    private class Commands {
        private final Command LIST = new Command("retrieve the list of all students") {
            public void execute() {
                Map<Long, Student> students = retrieveCurrentStudentData();
                if (students.isEmpty())
                    console.println("The student list is empty.");
                else students.values().stream().sorted(Comparator.comparing(Student::getName)).forEach(console::println);
            }
        };

        private final Command STUDENT = new Command("retrieve information about student by id") {
            public void execute() {
                long studentId;
                try {
                    studentId = Long.parseLong(console.input("Student ID: "));
                } catch (NumberFormatException e) {
                    console.println("Invalid student id.");
                    return;
                }

                Map<Long, Student> students = retrieveCurrentStudentData();
                if (students.containsKey(studentId)) {
                    console.println(students.get(studentId));
                } else {
                    console.println("Student with id=" + studentId + " not found.");
                }
            }
        };

        private final Command ADD = new Command("add new student") {
            public void execute() {
                String studentName = console.input("Name: ");
                if (studentName.contains("\"")) {
                    console.println("Quotes are banned, don't break the system! :)");
                    return;
                }

                retrieveCurrentStudentData();
                try {
                    Student student = studentService.createStudent(studentName);
                    console.println("Student created: " + student);
                } catch (IOException | FTPIllegalReplyException | FTPDataTransferException | FTPException e) {
                    console.error(e.getMessage());
                    console.println("Cannot create student!");
                }
            }
        };

        private final Command REMOVE = new Command("remove student by id") {
            public void execute() {
                long studentId;
                try {
                    studentId = Long.parseLong(console.input("Student ID: "));
                } catch (NumberFormatException e) {
                    console.println("Invalid student id.");
                    return;
                }

                Map<Long, Student> students = retrieveCurrentStudentData();
                if (!students.containsKey(studentId)) {
                    console.println("Student with id=" + studentId + " not found.");
                    return;
                }
                try {
                    studentService.removeStudent(studentId);
                    console.println("Student removed.");
                } catch (IOException | FTPIllegalReplyException | FTPDataTransferException | FTPException e) {
                    console.error(e.getMessage());
                    console.println("Cannot remove student!");
                }
            }
        };

        private final Command HELP = new Command("show the list of available commands") {
            public void execute() {
                StringBuilder sb = new StringBuilder();
                sb.append("Available commands:\n");
                commands.forEach((name, command) -> sb.append(name).append(" : ").append(command.description()).append("\n"));
                sb.append("Note that any additional arguments will be ignored.");
                console.println(sb);
            }
        };

        private final Command EXIT = new Command("disconnect from server and exit the app") {
            public void execute() {
                throw new NoSuchElementException();
            }
        };

        private Map<Long, Student> retrieveCurrentStudentData() {
            try {
                studentService.downloadStudentData();
            } catch (IOException | FTPIllegalReplyException | FTPDataTransferException | FTPException e) {
                console.error(e.getMessage());
                console.println("Unable to retrieve information from server, using data from local copy...");
            }
            return studentService.getLocalStudentData();
        }
    }
}
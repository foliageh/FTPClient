package services;

import config.Configuration;
import entities.Student;
import ftp.FTPClient;
import ftp.exceptions.FTPDataTransferException;
import ftp.exceptions.FTPException;
import ftp.exceptions.FTPIllegalReplyException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StudentService {
    private static final String STUDENT_DATA_FTP_FILEPATH = Configuration.getProperty("ftp.student-data-filepath", "students.json");
    private static final Pattern STUDENT_INFO_JSON_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)\\s*,\\s*\"name\"\\s*:\\s*\"([^\"]+)\"");
    private final FTPClient ftpClient;
    private Map<Long, Student> students = new HashMap<>();

    public StudentService(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }

    private static Map<Long, Student> parseJsonToStudentMap(String json) {
        Map<Long, Student> students = new HashMap<>();
        Matcher m = STUDENT_INFO_JSON_PATTERN.matcher(json);
        while (m.find()) {
            long id = Long.parseLong(m.group(1));
            String name = m.group(2);
            students.put(id, new Student(id, name));
        }
        return students;
    }

    private static String serializeStudentMapToJson(Map<Long, Student> students) {
        return students.values().stream()
                .map(student -> String.format("{\"id\":%d,\"name\":\"%s\"}", student.getId(), student.getName()))
                .collect(Collectors.joining(",", "{\"students\":[", "]}"));
    }

    public void downloadStudentData() throws IOException, FTPIllegalReplyException, FTPDataTransferException, FTPException {
        String json;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ftpClient.downloadTextualData(STUDENT_DATA_FTP_FILEPATH, outputStream);
            json = outputStream.toString("UTF8");
        }
        students = parseJsonToStudentMap(json);
    }

    private void uploadStudentData(Runnable onFail) throws FTPIllegalReplyException, FTPDataTransferException, IOException, FTPException {
        String studentDataJson = serializeStudentMapToJson(students);
        try (InputStream inputStream = new ByteArrayInputStream(studentDataJson.getBytes(StandardCharsets.UTF_8))) {
            ftpClient.uploadTextualData(STUDENT_DATA_FTP_FILEPATH, inputStream);
        } catch (FTPIllegalReplyException | IOException | FTPException | FTPDataTransferException e) {
            onFail.run();
            throw e;
        }
    }

    public Map<Long, Student> getLocalStudentData() {
        return students;
    }

    public Student createStudent(String studentName) throws FTPIllegalReplyException, FTPDataTransferException, IOException, FTPException {
        long studentId = generateStudentID();
        Student student = new Student(studentId, studentName);
        students.put(studentId, student);

        uploadStudentData(() -> students.remove(studentId));
        return student;
    }

    public void removeStudent(long studentId) throws FTPIllegalReplyException, FTPDataTransferException, IOException, FTPException {
        Student student = students.get(studentId);
        students.remove(studentId);

        uploadStudentData(() -> students.put(studentId, student));
    }

    private long generateStudentID() {
        if (students.isEmpty())
            return 0;

        long _id = students.keySet().stream()
                .filter(id -> id > 0 && !students.containsKey(id - 1) || id < Long.MAX_VALUE && !students.containsKey(id + 1))
                .findAny().get();
        return _id > 0 && !students.containsKey(_id - 1) ? _id - 1 : _id + 1;
    }
}

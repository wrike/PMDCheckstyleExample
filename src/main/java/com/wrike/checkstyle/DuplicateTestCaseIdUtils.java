package com.wrike.checkstyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public final class DuplicateTestCaseIdUtils {

    private static final String TESTS_PATH = System.getProperty("user.dir");

    private static final List<File> TEST_CLASSES = new ArrayList<>();
    private static Map<Integer, Integer> allIdsWithAmount;

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    private DuplicateTestCaseIdUtils() {
    }

    public static synchronized Map<Integer, Integer> getAllIdsWithAmount(String path) {
        performFindAllIds(path);
        return allIdsWithAmount;
    }

    private static void performFindAllIds(String path) {
        if (allIdsWithAmount == null) {
            allIdsWithAmount = new HashMap<>();
            findTestClasses(new File(path));
            findAllIds(TEST_CLASSES);
        }
    }

    private static void findTestClasses(File folder) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File fileEntry : files) {
                if (fileEntry.isDirectory()) {
                    findTestClasses(fileEntry);
                } else {
                    if (fileEntry.getPath().endsWith(".java")) {
                        TEST_CLASSES.add(fileEntry);
                    }
                }
            }
        }
    }

    public static Map<Integer, Integer> findAllIds() {
        return getAllIdsWithAmount(TESTS_PATH);
    }

    private static void findAllIds(List<File> files) {
        for (File file : files) {
            try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getPath()))) {
                String line = br.readLine();
                while (line != null) {
                    if (line.contains("@TestCaseId")) {
                        parseAndAddId(buildLines(br, line));
                    }
                    line = br.readLine();
                }
            } catch (IOException e) {
                throw new Error(format("Can't operate with file %s", file), e);
            }
        }
    }

    private static void parseAndAddId(String testCaseId) {
        Matcher match = DIGIT_PATTERN.matcher(testCaseId);
        while (match.find()) {
            Integer id = Integer.parseInt(match.group());
            allIdsWithAmount.put(id, allIdsWithAmount.getOrDefault(id, 0) + 1);
        }
    }

    private static String buildLines(BufferedReader br, String line) throws IOException {
        String lineWithoutComment;
        if (line.contains("/")) {
            lineWithoutComment = line.substring(0, line.indexOf('/'));
        } else {
            lineWithoutComment = line;
        }

        if (lineWithoutComment.contains(")")) {
            return lineWithoutComment;
        } else {
            return lineWithoutComment + buildLines(br, br.readLine());
        }
    }

}

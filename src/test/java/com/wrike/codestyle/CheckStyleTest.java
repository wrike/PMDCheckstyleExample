package com.wrike.codestyle;

import com.puppycrawl.tools.checkstyle.Main;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.wrike.codestyle.CheckCodeStyleUtils.CodeStyleGitCommand.DIFF_NAMES_LOCAL;
import static com.wrike.codestyle.CheckCodeStyleUtils.CodeStyleGitCommand.DIFF_NAMES_MASTER;
import static com.wrike.codestyle.CheckCodeStyleUtils.PROJECT_ROOT;
import static com.wrike.codestyle.CheckCodeStyleUtils.getFileInStringList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.SystemUtils.USER_DIR;

public class CheckStyleTest {

    private static final Logger LOG = Logger.getLogger(CheckStyleTest.class);
    private static final String ERROR_LABEL = "[ERROR] ";

    private static final String CONFIGURATION_FILE = USER_DIR + "/src/main/resources/checkstyle/checksWrike.xml";
    private static final String OUTPUT_REPORT_FILE = USER_DIR + "/target/checkStyleOutputReport.txt";

    @Test
    void checkChangedFilesWithCheckStyle() {
        CheckCodeStyleUtils.fetchChanges();
        List<String> changedFileNameList = CheckCodeStyleUtils.runGitDiffCommandAndGetChanges(DIFF_NAMES_LOCAL, DIFF_NAMES_MASTER);
        Set<File> changedFileSet = CheckCodeStyleUtils.getChangedFileSet(changedFileNameList);
        checkCodeStyle(changedFileSet);
    }

    public void checkCodeStyleForFiles(Set<File> fileSet) {
        checkCodeStyle(fileSet);
    }

    private void checkCodeStyle(Set<File> fileSet) {
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                /* Allow everything.*/
            }

            @Override
            public void checkExit(int status) {
                /* Don't allow exit with failed status code.*/
                if (status > 0) {
                    throw new CodeStyleViolationException();
                }
            }
        });

        try {
            checkFilesWithCheckStyle(fileSet);
        } catch (CodeStyleViolationException e) {
            printViolation();
        }
    }

    private void printViolation() {
        List<CheckStyleViolation> checkStyleViolationList = createAndGetViolationList();
        LOG.error(checkStyleViolationList);
        printCodeStyleViolationInTeamCity(checkStyleViolationList);
        throw new CodeStyleViolationException(
                format("Your code have [%s] violation(s)! Please fix issue. Check logs for more information...",
                        checkStyleViolationList.size()));
    }

    private void printCodeStyleViolationInTeamCity(List<CheckStyleViolation> checkStyleViolationList) {
        String buildFailureMessageWithIssueCount = format("Problems with code style, found [%s] violation(s)",
                checkStyleViolationList.size());
        LOG.info(format("##teamcity[buildStatus status='FAILURE' text='%s']",
                CheckCodeStyleUtils.replaceEscapeCharacters(buildFailureMessageWithIssueCount)));
        checkStyleViolationList.forEach(checkStyleViolation ->
                LOG.info(format("##teamcity[buildStatus status='FAILURE' text= '%s']",
                        CheckCodeStyleUtils.replaceEscapeCharacters(checkStyleViolation.toString()))));
    }

    private void checkFilesWithCheckStyle(Set<File> filesForCheckList) {
        if (filesForCheckList.isEmpty()) {
            return;
        }

        try {
            CommandLine commandLine = new CommandLine();
            commandLine.addArg("-c");
            commandLine.addArg(CONFIGURATION_FILE);
            filesForCheckList.forEach(file -> commandLine.addArg(file.getAbsolutePath()));
            commandLine.addArg("-o");
            commandLine.addArg(OUTPUT_REPORT_FILE);

            Main.main(commandLine.getArgs());
        } catch (IOException e) {
            throw new Error("Error while executing CodeStyle checks!", e);
        }
    }

    List<CheckStyleViolation> createAndGetViolationList() {
        List<String> listOfViolations = getFileInStringList(OUTPUT_REPORT_FILE);
        return listOfViolations.stream()
                .filter(line -> line.contains(ERROR_LABEL))
                .map(this::parseViolation)
                .collect(Collectors.toList());
    }

    private CheckStyleViolation parseViolation(String violationInString) {
        String[] splittedViolation = violationInString.split(": ");
        String fileLocation = remove(splittedViolation[0], ERROR_LABEL + PROJECT_ROOT);
        String fileLink = fileLocation.substring(lastIndexOf(fileLocation, File.separator) + 1);

        return new CheckStyleViolation(fileLocation, createLinkFromFileName(fileLink), splittedViolation[1]);
    }

    private String createLinkFromFileName(String fileName) {
        String preparedFileName = fileName.split(":").length > 2
                ? fileName.substring(0, fileName.lastIndexOf(':')) : fileName;
        return format(".(%s)", preparedFileName);
    }

    public static class CheckStyleViolation {

        private final String pathToFile;
        private final String linkToViolation;
        private final String description;

        CheckStyleViolation(String pathToFile, String linkToViolation, String description) {
            this.pathToFile = pathToFile;
            this.linkToViolation = linkToViolation;
            this.description = description;
        }

        @Override
        public String toString() {
            return String.join("\n", "\n", pathToFile, linkToViolation, description, repeat("-", 40));
        }
    }

    private static class CommandLine {
        private final List<String> args = new LinkedList<>();

        void addArg(String arg) {
            args.add(arg);
        }

        String[] getArgs() {
            String[] result = new String[args.size()];
            args.toArray(result);
            return result;
        }

        @Override
        public String toString() {
            return "CommandLine{" + "args=" + args + '}';
        }
    }

    private static class CodeStyleViolationException extends RuntimeException {
        static final long serialVersionUID = -7034877170745766737L;

        CodeStyleViolationException() {
            super();
        }

        CodeStyleViolationException(String errorMessage) {
            super(errorMessage);
        }
    }

}

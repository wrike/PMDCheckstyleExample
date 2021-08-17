package com.wrike.codestyle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.SystemUtils.USER_DIR;

public final class CheckCodeStyleUtils {

    private static final Logger LOG = Logger.getLogger(CheckCodeStyleUtils.class);
    private static String branchName;
    private static boolean isAlreadyFetched;
    private static final List<String> DIFF_WITH_MASTER_LIST = new ArrayList<>();

    public static final Path PROJECT_ROOT = Path.of(USER_DIR);

    private CheckCodeStyleUtils() {
    }

    public static List<String> runGitDiffCommandAndGetChanges(CodeStyleGitCommand localDiffCommand, CodeStyleGitCommand remoteDiffCommand) {
        if (DIFF_WITH_MASTER_LIST.isEmpty()) {
            LOG.info(format("Getting diff between `origin/master` and `%s` branches", getCurrentBranchName()));
            DIFF_WITH_MASTER_LIST.addAll(executeCommandAndGetResultList(new ProcessBuilder(localDiffCommand.getCommands())));
            DIFF_WITH_MASTER_LIST.addAll(executeCommandAndGetResultList(new ProcessBuilder(remoteDiffCommand.getCommands())));
        }
        return DIFF_WITH_MASTER_LIST;
    }

    private static List<String> executeCommandAndGetResultList(ProcessBuilder processBuilder) {
        Process process;
        StringWriter writer = new StringWriter();
        try {
            process = processBuilder.start();
            process.waitFor(20, TimeUnit.SECONDS);
            IOUtils.copy(process.getInputStream(), writer, StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException e) {
            throw new Error(String.format("Can not execute command [%s], trace: %s", processBuilder.command(), e));
        }
        return writer.toString().isEmpty() ? List.of() : Arrays.asList(writer.toString().split("\n"));
    }

    public static synchronized Set<File> getChangedFileSet(List<String> fileNameList) {
        Set<File> fileSet;
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(fileNameList);
        fileSet = cowList.stream()
                .filter(StringUtils::isNotEmpty)
                .map(path -> PROJECT_ROOT + "/" + path)
                .map(File::new)
                .filter(File::exists)
                .collect(Collectors.toSet());
        LOG.info("\nList of " + fileSet.size() + " files for checking is:\n "
                + fileSet.stream().map(file -> "\t" + StringUtils.remove(file.getAbsolutePath(), PROJECT_ROOT.toString()) + "\n")
                .collect(Collectors.joining()));
        return fileSet;
    }

    public static void fetchChanges() {
        if (!isAlreadyFetched) {
            try {
                LOG.info("Fetching changes...");
                new ProcessBuilder("git", "fetch")
                        .start()
                        .waitFor(60, TimeUnit.SECONDS);
                isAlreadyFetched = true;
            } catch (InterruptedException | IOException e) {
                throw new Error("Error while fetching changes", e);
            }
        }
    }

    private static String findCurrentBranchName() {
        return executeCommandAndGetResultList(
                new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")).get(0).trim();
    }

    private static String getCurrentBranchName() {
        if (branchName == null) {
            LOG.info("getting branch name...");
            branchName = findCurrentBranchName();
            LOG.info(branchName);
        }
        return branchName;
    }

    @SuppressWarnings("Duplicates")
    public static String replaceEscapeCharacters(String string) {
        String tmp = string.replace("|", "||");
        tmp = tmp.replace("'", "|'");
        tmp = tmp.replace("\"", "|'");
        tmp = tmp.replaceAll("\\n", "|n");
        tmp = tmp.replaceAll("\\r", "|r");
        tmp = tmp.replace("[", "|[");
        tmp = tmp.replace("]", "|]");
        return tmp;
    }

    public static List<String> getFileInStringList(String filePath) {
        File codeStyleReportFile = new File(filePath);
        List<String> fileInList;
        try {
            fileInList = FileUtils.readLines(codeStyleReportFile, Charset.defaultCharset());
        } catch (IOException e) {
            throw new Error(format("Can't operate with file %s", filePath), e);
        }
        return fileInList;
    }

    public enum CodeStyleGitCommand {
        DIFF_NAMES_LOCAL(List.of("git", "diff", "--name-only", "HEAD")),
        DIFF_NAMES_MASTER(List.of("git", "diff", "--name-only", "origin/master..."));

        private List<String> commands;

        CodeStyleGitCommand(List<String> commands) {
            this.commands = commands;
        }

        public List<String> getCommands() {
            return commands;
        }

        @Override
        public String toString() {
            return commands.toString();
        }
    }
}

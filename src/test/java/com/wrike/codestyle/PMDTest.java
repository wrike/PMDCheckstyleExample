package com.wrike.codestyle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.wrike.codestyle.CheckCodeStyleUtils.CodeStyleGitCommand.DIFF_NAMES_LOCAL;
import static com.wrike.codestyle.CheckCodeStyleUtils.CodeStyleGitCommand.DIFF_NAMES_MASTER;
import static java.lang.String.format;

public class PMDTest {

    private static final Logger LOG = Logger.getLogger(PMDTest.class);
    private static final String HORIZONTAL_LINE = StringUtils.repeat('-', 77);
    private int countPMDIssue;

    @Test
    void runPMD() {
        CheckCodeStyleUtils.fetchChanges();
        List<String> changedFileNameList = CheckCodeStyleUtils.runGitDiffCommandAndGetChanges(DIFF_NAMES_LOCAL, DIFF_NAMES_MASTER);
        List<String> onlyJavaFiles = changedFileNameList.stream().filter(file -> file.endsWith(".java")).collect(Collectors.toList());
        Set<File> changedFileSet = CheckCodeStyleUtils.getChangedFileSet(onlyJavaFiles);
        if (changedFileSet.isEmpty()) {
            return;
        }
        checkPMDForFileSet(changedFileSet);
    }

    public void runPMD(Set<File> fileSet) {
        checkPMDForFileSet(fileSet);
    }

    private void checkPMDForFileSet(Set<File> fileSet) {
        List<PMDWrapper.PMDResult> pmdResults = checkFileListAndGetResult(fileSet);
        List<String> violationInfoList = issuePmdResults(pmdResults).stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());

        if (!violationInfoList.isEmpty()) {
            String buildFailureMessageWithIssueCount = format("\nProblems with PMD, found [%s] violation(s)\n", countPMDIssue);

            LOG.error(format("\n %s", buildFailureMessageWithIssueCount));
            LOG.error(violationInfoList);
            throw new PMDViolationException();
        }
    }

    private List<String> issuePmdResults(List<PMDWrapper.PMDResult> results) {
        List<String> violationInfoList = new ArrayList<>();
        for (PMDWrapper.PMDResult pmdResult : results.stream().filter(result -> !result.getViolationList().isEmpty()).collect(Collectors.toList())) {
            violationInfoList.add(createViolationInfoString(pmdResult));
        }
        for (PMDWrapper.PMDResult pmdResult : results.stream().filter(result -> !result.getInternalViolationList().isEmpty()).collect(Collectors.toList())) {
            violationInfoList.add(createInternalViolationInfoString(pmdResult));
        }
        return violationInfoList;
    }

    private String createInternalViolationInfoString(PMDWrapper.PMDResult pmdResult) {
        StringBuilder sb = new StringBuilder();
        pmdResult.getInternalViolationList().forEach(violation -> {
            countPMDIssue++;

            sb.append('\n');
            sb.append(violation);
            sb.append(HORIZONTAL_LINE).append('\n');
        });

        return sb.toString();
    }

    private String createViolationInfoString(PMDWrapper.PMDResult pmdResult) {
        StringBuilder sb = new StringBuilder();
        pmdResult.getViolationList().forEach(violation -> {
            countPMDIssue++;

            sb.append('\n')
                    .append(violation.getFilename().replace(CheckCodeStyleUtils.PROJECT_ROOT.toString(), ""))
                    .append('\n');
            sb.append(".(").append(violation.getClassName()).append(".java").append(':')
                    .append(violation.getBeginLine()).append(')').append('\n');
            sb.append(" at line `");
            sb.append(violation.getBeginLine());
            sb.append("` and column `");
            sb.append(violation.getBeginColumn()).append("`\n");
            sb.append(violation.getDescription().trim().replaceAll("\\\\n", "\n")).append('\n');
            sb.append(violation.getRule().getName().trim());
            sb.append(violation.getRule().getExamples().size() > 0 ? violation.getRule().getExamples() : "");
            sb.append(violation.getRule().getExternalInfoUrl().trim().length() > 0
                    ? "`\ncheck in " + violation.getRule().getExternalInfoUrl() + '\n' : '\n');
            sb.append(HORIZONTAL_LINE).append('\n');
        });

        return sb.toString();
    }

    private List<PMDWrapper.PMDResult> checkFileListAndGetResult(Set<File> filesForCheckInPmd) {
        return PMDWrapper.checkFilesWithPMD(filesForCheckInPmd);
    }

    static class PMDViolationException extends RuntimeException {
        private static final long serialVersionUID = 4156531284772167254L;
    }

}

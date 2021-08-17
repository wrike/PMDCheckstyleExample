package com.wrike.codestyle;

import com.google.gson.Gson;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetLoader;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static net.sourceforge.pmd.PMD.getApplicableFiles;

class PMDWrapper {

    public static final Gson GSON = new Gson();
    private static final Logger LOG = Logger.getLogger(PMDWrapper.class);
    private static final String PMD_REPORT_PATH = "./target/pmd_report.LOG";
    private static final String PMD_RULESETS_PATH = "./src/main/resources/pmd";
    private static Writer writer;

    public static class PMDResult {
        private final List<RuleViolation> violationList;
        private List<String> internalViolationList;

        @Override
        public String toString() {
            return "PMDResult{" +
                    ", violationList=" + violationList +
                    ", internalViolationList=" + internalViolationList +
                    '}';
        }

        PMDResult(final List<RuleViolation> violationList) {
            this.violationList = checkNotNull(violationList, "violationList required");
        }

        List<RuleViolation> getViolationList() {
            return violationList;
        }

        public void setInternalViolationList(List<String> internalViolationList) {
            this.internalViolationList = internalViolationList;
        }

        public List<String> getInternalViolationList() {
            return internalViolationList;
        }
    }

    private static String getPmdRulesets() throws IOException {
        File[] pmdRulesets = new File(PMD_RULESETS_PATH).listFiles();
        if (pmdRulesets == null) {
            throw new FileNotFoundException(String.format("Directory with path %s doesn't exist!", PMD_RULESETS_PATH));
        }
        return Arrays.stream(pmdRulesets)
                .map(File::getName)
                .map(filename -> "pmd/" + filename)
                .collect(Collectors.joining(","));
    }

    static List<PMDResult> checkFilesWithPMD(Set<File> fileSet) {
        List<PMDResult> pmdResults = new ArrayList<>();
        try {
            writer = Files.newBufferedWriter(new File(PMD_REPORT_PATH).toPath(), Charset.defaultCharset());
            pmdResults.addAll(Collections.singletonList(checkFilesWithPMD(fileSet.toArray(File[]::new))));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }

        checkPMDReportLogsFile(pmdResults);

        return pmdResults;
    }

    private static PMDResult checkFilesWithPMD(File... srcDir) throws IOException {
        checkNotNull(srcDir, "srcDir required");

        final File tempFile = createTempFileWithChangedFiles(srcDir);

        PMDConfiguration pmdConfiguration = new PMDConfiguration();
        Language javaLanguage = LanguageRegistry.getLanguage("Java");

        pmdConfiguration.setDefaultLanguageVersion(javaLanguage.getVersion("11"));
        pmdConfiguration.setThreads(Integer.parseInt(System.getProperty("pmd.threads", "8")));
        pmdConfiguration.setRuleSets(getPmdRulesets());
        pmdConfiguration.setInputFilePath(tempFile.getAbsolutePath());
        pmdConfiguration.setReportFormat("json");
        pmdConfiguration.setReportFile(PMD_REPORT_PATH);
        pmdConfiguration.setAnalysisCache(null);
        pmdConfiguration.setDebug(false);

        final List<RuleViolation> violationList = new ArrayList<>();

        doPMD(pmdConfiguration, violationList);

        return new PMDResult(violationList);
    }

    private static int doPMD(final PMDConfiguration configuration, List<RuleViolation> violationList) {

        // Load the RuleSets
        RuleSetLoader ruleSetLoader = RuleSetLoader.fromPmdConfig(configuration);
        List<RuleSet> ruleSetList = ruleSetLoader.loadFromResources(Arrays.asList(configuration.getRuleSets().split(",")));
        if (ruleSetList.isEmpty()) {
            return 0;
        } else {
            if (configuration.isDebug()) {
                LOG.info("Enabled rules:");
                for (final RuleSet rule : ruleSetList) {
                    LOG.info("  - " + rule.getName());
                }
                LOG.info("total rules (" + ruleSetList.size() + ")");
            }
        }

        final Language languages = getApplicableLanguages(configuration);
        final List<DataSource> files = getApplicableFiles(configuration, Set.of(languages));

        try {
            final Renderer renderer = configuration.createRenderer();
            final List<Renderer> renderers = Collections.singletonList(renderer);

            renderer.setWriter(writer);
            renderer.start();
            Report report = PMD.processFiles(configuration, ruleSetList, files, renderers);
            violationList.addAll(report.getViolations());
            renderer.end();
            renderer.flush();

            return report.getViolations().size();
        } catch (final Exception e) {
            final String message = e.getMessage();
            if (message == null) {
                LOG.warn("Exception during processing", e);
            } else {
                LOG.error(message);
            }

            LOG.debug("Exception during processing", e);
            //print PMD usage
            LOG.info(PMD.run(new String[]{""}));

            return -1;
        }
    }

    private static Language getApplicableLanguages(final PMDConfiguration configuration) {
        Language language = LanguageRegistry.getLanguage("Java");
        LOG.info("Using " + language.getShortName() + " version: " + configuration.getLanguageVersionDiscoverer().getDefaultLanguageVersion(language).getShortName());
        return language;
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static void checkPMDReportLogsFile(List<PMDResult> pmdResult) {
        try {
            PmdLogFileBean pmdLogFileBean = GSON.fromJson(Files.newBufferedReader(Paths.get(PMD_REPORT_PATH)), PmdLogFileBean.class);
            pmdResult.get(0).setInternalViolationList(
                    pmdLogFileBean.getProcessingErrors().stream()
                            .map(PmdLogFileBean.ProcessingErrors::getDetail)
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new Error(format("Can't operate with file %s", PMD_REPORT_PATH), e);
        }
    }

    private static File createTempFileWithChangedFiles(File... srcDir) throws IOException {
        File tempFile = File.createTempFile("pmd_files", ".tmp");
        tempFile.deleteOnExit();

        final String commaSeparatedFilePaths = Stream.of(srcDir)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(","));

        Files.writeString(tempFile.toPath(), commaSeparatedFilePaths);
        return tempFile;
    }
}

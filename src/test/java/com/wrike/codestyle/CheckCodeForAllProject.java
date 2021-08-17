package com.wrike.codestyle;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.wrike.codestyle.CheckCodeStyleUtils.PROJECT_ROOT;

class CheckCodeForAllProject {

    private static final Logger LOG = Logger.getLogger(CheckCodeForAllProject.class);

    @Test
    void runCheckStyle() {
        new CheckStyleTest().checkCodeStyleForFiles(getAllJavaFiles());
    }

    @Test
    void runPMD() {
        new PMDTest().runPMD(getAllJavaFiles());
    }

    private static Set<File> getJavaFileSetForFolder(File folder) {
        Set<File> javaFileSet = new HashSet<>();
        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.exists() && fileEntry.isDirectory()) {
                try {
                    if (!fileEntry.getCanonicalPath().contains("target")) {
                        javaFileSet.addAll(getJavaFileSetForFolder(fileEntry));
                    }
                } catch (IOException e) {
                    LOG.error(e);
                }
            } else {
                if (fileEntry.getName().endsWith(".java")) {
                    javaFileSet.add(fileEntry);
                }
            }
        }
        return javaFileSet;
    }

    private static Set<File> getAllJavaFiles() {
        return getJavaFileSetForFolder(PROJECT_ROOT.toFile());
    }
}

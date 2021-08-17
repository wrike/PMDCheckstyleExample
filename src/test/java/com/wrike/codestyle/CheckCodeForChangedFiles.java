package com.wrike.codestyle;

import org.junit.jupiter.api.Test;

class CheckCodeForChangedFiles {

    @Test
    void runPmd() {
        new PMDTest().runPMD();
    }

    @Test
    void runCheckStyle() {
        new CheckStyleTest().checkChangedFilesWithCheckStyle();
    }
}

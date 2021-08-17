package com.wrike.tests;

import com.wrike.annotation.Epic;
import com.wrike.annotation.TestCaseId;
import com.wrike.steps.ScreenshotSteps;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static com.wrike.steps.ScreenshotSteps.SCREENSHOT_TEST;

//change this file to add it to checkstyle and PMD tests
@Epic("ScreenshotTest")
public class ScreenshotTest {

    private static final ScreenshotSteps SCREENSHOT_STEPS = new ScreenshotSteps();

    private void doNothing() {
        //do some important stuff
    }

    private BufferedImage makeScreenshotOfPage() {
        //do some important stuff
        return SCREENSHOT_STEPS.makeScreenshot();
    }

    @Test
    @TestCaseId(10)
    public void nonScreenshotMethod() {
        doNothing();
    }

    @Test
    @TestCaseId(11)
    public void screenshotMethodWithoutTag() {
        makeScreenshotOfPage();
    }

    @Test
    @Tag(SCREENSHOT_TEST)
    @TestCaseId(12)
    public void screenshotMethodWithTag() {
        makeScreenshotOfPage();
    }

    @Test
    @Tag(SCREENSHOT_TEST)
    @TestCaseId(13)
    public void screenshotMethodWithTag2() {
        SCREENSHOT_STEPS.makeScreenshot();
    }

}

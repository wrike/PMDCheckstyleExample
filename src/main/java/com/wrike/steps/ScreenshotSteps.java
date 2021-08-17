package com.wrike.steps;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class ScreenshotSteps {

    public static final String SCREENSHOT_TEST = "SCREENSHOT_TEST";

    public BufferedImage makeScreenshot() {
        return new BufferedImage(1, 1, Image.SCALE_DEFAULT);
    }

}

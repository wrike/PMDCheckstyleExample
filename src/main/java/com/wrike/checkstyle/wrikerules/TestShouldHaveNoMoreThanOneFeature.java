package com.wrike.checkstyle.wrikerules;

import com.wrike.annotation.Feature;
import com.wrike.annotation.Features;

public class TestShouldHaveNoMoreThanOneFeature extends TestShouldHaveOneEntity {

    public TestShouldHaveNoMoreThanOneFeature() {
        super(Feature.class.getSimpleName(), Features.class.getSimpleName(), false);
    }

}

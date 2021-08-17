package com.wrike.checkstyle.wrikerules;

import com.wrike.annotation.Stories;
import com.wrike.annotation.Story;

public class TestShouldHaveNoMoreThanOneStory extends TestShouldHaveOneEntity {

    public TestShouldHaveNoMoreThanOneStory() {
        super(Story.class.getSimpleName(), Stories.class.getSimpleName(), false);
    }

}

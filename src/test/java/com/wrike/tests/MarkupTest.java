package com.wrike.tests;

import com.wrike.annotation.Epic;
import com.wrike.annotation.Epics;
import com.wrike.annotation.Feature;
import com.wrike.annotation.Features;
import com.wrike.annotation.Story;
import com.wrike.annotation.TestCaseId;
import org.junit.jupiter.api.Test;

//change this file to add it to checkstyle and PMD tests
public class MarkupTest {

    @Test
    @TestCaseId(14)
    @Epic("MarkupTest")
    @Features({@Feature("feature1"), @Feature("feature2")})
    public void testTwoFeatures() {

    }

    @Test
    @TestCaseId(15)
    @Epic("MarkupTest")
    public void testNoFeature() {

    }

    @Test
    @TestCaseId(16)
    @Epic("MarkupTest")
    @Story("story1")
    public void testStory() {

    }

    @Test
    @TestCaseId(17)
    public void testNoEpic() {

    }

    @Test
    @TestCaseId(18)
    @Epics({@Epic("MarkupTest"), @Epic("epic1")})
    public void testTwoEpics() {

    }

}

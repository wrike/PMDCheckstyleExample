package com.wrike.checkstyle.wrikerules;

import com.wrike.annotation.Epic;
import com.wrike.annotation.Epics;

public class TestShouldHaveOnlyOneEpic extends TestShouldHaveOneEntity {

    public TestShouldHaveOnlyOneEpic() {
        super(Epic.class.getSimpleName(), Epics.class.getSimpleName(), true);
    }

}

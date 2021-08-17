package com.wrike.tests;

import com.wrike.annotation.Epic;
import org.junit.jupiter.api.RepeatedTest;

//change this file to add it to checkstyle and PMD tests
@Epic("RepeatedAnnotationTest")
public class RepeatedAnnotationTest {

    @RepeatedTest(8)
    public void repeatedTest() {
        //nothing interesting
    }

}

package com.wrike.tests;

import com.wrike.annotation.Epic;
import com.wrike.annotation.TestCaseId;
import org.junit.jupiter.api.Test;

//change this file to add it to checkstyle and PMD tests
@Epic("DuplicateIdsTest")
public class DuplicateIdsTest {

    @Test
    @TestCaseId(1)
    public void test() {

    }

    @Test
    @TestCaseId(1)
    public void testDuplicate() {

    }

}

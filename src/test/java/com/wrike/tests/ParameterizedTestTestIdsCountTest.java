package com.wrike.tests;

import com.wrike.annotation.Epic;
import com.wrike.annotation.TestCaseId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//change this file to add it to checkstyle and PMD tests
@Epic("ParameterizedTestTestIdsCountTest")
public class ParameterizedTestTestIdsCountTest {

    @ParameterizedTest
    @ValueSource(strings = {"apple", "banana"})
    @TestCaseId(2)
    public void testValueSource(String fruit) {
        assertNotEquals("pineapple", fruit);
    }

    @ParameterizedTest
    @CsvSource({
            "apple, 1",
            "banana, 2"
    })
    @TestCaseId({3, 4, 5})
    public void testCSVSource(String fruit, int count) {
        assertNotEquals("pineapple", fruit);
        assertTrue(count < 3);
    }

    static List<String> sourceProvider() {
        return List.of(
                "apple",
                "banana"
        );
    }

    @ParameterizedTest
    @MethodSource("sourceProvider")
    @TestCaseId({6, 7})
    public void testMethodSource(String fruit) {
        assertNotEquals("pineapple", fruit);
    }

    private enum TestParameter {
        APPLE, BANANA
    }

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    @TestCaseId({8, 9})
    public void testMethodSource(TestParameter fruit) {
        assertNotEquals("PINEAPPLE", fruit.name());
    }

}

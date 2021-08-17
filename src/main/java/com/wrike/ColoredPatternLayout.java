package com.wrike;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class ColoredPatternLayout extends PatternLayout {

    private static final char SEPARATOR = ';';
    private static final String PREFIX = "\u001b[";
    private static final String ATTR_DIM = "2";
    private static final String FG_RED = "31";
    private static final String SUFFIX = "m";
    private static final String END_COLOR = PREFIX + SUFFIX;

    private static final String ERROR_COLOR = PREFIX + ATTR_DIM + SEPARATOR + FG_RED + SUFFIX;

    @Override
    public String format(LoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return ERROR_COLOR + super.format(event) + END_COLOR;
        } else {
            return super.format(event);
        }
    }
}

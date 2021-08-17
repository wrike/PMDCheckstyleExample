package com.wrike.codestyle;

import lombok.Data;

import java.util.List;

@Data
public class PmdLogFileBean {

    private List<ProcessingErrors> processingErrors;

    @Data
    public static class ProcessingErrors {
        private String detail;
    }

}

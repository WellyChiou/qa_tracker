package com.example.helloworld.dto.church.frontend;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AboutResult {

    private List<Section> sections;

    @Data
    @AllArgsConstructor
    public static class Section {
        private String key;
        private String title;
        private String content;
        private int order;
    }
}
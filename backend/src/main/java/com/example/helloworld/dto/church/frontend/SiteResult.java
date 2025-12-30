package com.example.helloworld.dto.church.frontend;

import lombok.Data;

import java.util.Map;

@Data
public class SiteResult {

    private Contact contact = new Contact();
    private OfficeHours officeHours = new OfficeHours();
    private Home home = new Home();

    // 預留擴充
    private Map<String, String> social;
    private Seo seo;

    @Data
    public static class Contact {
        private String address;
        private String phone;
        private String email;
    }

    @Data
    public static class OfficeHours {
        private String weekday;
        private String weekend;
    }

    @Data
    public static class Home {
        private String welcomeTitle;
        private String welcomeSubtitle;
        private Service mainService = new Service();
        private Service saturdayService = new Service();

        @Data
        public static class Service {
            private String time;
            private String location;
        }
    }

    @Data
    public static class Seo {
        private String title;
        private String description;
        private String ogImageUrl;
    }
}
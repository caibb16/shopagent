package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class Logistics {
    private String trackingNumber;
    private String carrier;
    private String currentLocation;
    private List<Event> events;

    @Data @Builder
    public static class Event {
        private Instant timestamp;
        private String location;
        private String description;
    }
}

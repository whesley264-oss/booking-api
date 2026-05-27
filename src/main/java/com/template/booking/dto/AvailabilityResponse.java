package com.template.booking.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
    private String reason;
}

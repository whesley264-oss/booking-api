package com.template.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    private Integer capacity;
    private String location;
}

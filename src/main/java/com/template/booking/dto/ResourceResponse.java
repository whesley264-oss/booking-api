package com.template.booking.dto;

import com.template.booking.model.Resource;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceResponse {
    private Long id;
    private String name;
    private String description;
    private Integer capacity;
    private String location;
    private String status;

    public static ResourceResponse fromEntity(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .capacity(resource.getCapacity())
                .location(resource.getLocation())
                .status(resource.getStatus().name())
                .build();
    }
}

package com.template.booking.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PageRequestDto {
    @Min(value = 0, message = "Page must be >= 0")
    @Builder.Default
    private int page = 0;
    
    @Min(value = 1, message = "Size must be >= 1")
    @Max(value = 100, message = "Size must be <= 100")
    @Builder.Default
    private int size = 20;
    
    @Builder.Default
    private String sortBy = "createdAt";
    
    @Builder.Default
    private String sortDirection = "DESC";

    public org.springframework.data.domain.PageRequest toSpringPageRequest() {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}

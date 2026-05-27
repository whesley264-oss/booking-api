package com.template.booking.controller;

import com.template.booking.dto.ResourceRequest;
import com.template.booking.dto.ResourceResponse;
import com.template.booking.dto.common.PageRequestDto;
import com.template.booking.dto.common.PageResponse;
import com.template.booking.model.Resource;
import com.template.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Resource management endpoints")
public class ResourceController {
    private final ResourceService resourceService;

    @PostMapping
    @Operation(summary = "Create a new resource")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createResource(request));
    }

    @GetMapping
    @Operation(summary = "Get all resources with pagination")
    public ResponseEntity<PageResponse<ResourceResponse>> getAllResources(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDirection) {
        PageRequestDto pageRequest = PageRequestDto.builder()
                .page(page).size(size).sortBy(sortBy).sortDirection(sortDirection).build();
        return ResponseEntity.ok(resourceService.getAllResources(pageRequest));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active resources with pagination")
    public ResponseEntity<PageResponse<ResourceResponse>> getActiveResources(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)") @RequestParam(defaultValue = "20") int size) {
        PageRequestDto pageRequest = PageRequestDto.builder().page(page).size(size).build();
        return ResponseEntity.ok(resourceService.getActiveResources(pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<ResourceResponse> getResource(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResource(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update resource status")
    public ResponseEntity<ResourceResponse> updateResourceStatus(
            @PathVariable Long id,
            @RequestParam Resource.ResourceStatus status) {
        return ResponseEntity.ok(resourceService.updateResourceStatus(id, status));
    }
}

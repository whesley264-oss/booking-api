package com.template.booking.controller;

import com.template.booking.dto.ResourceRequest;
import com.template.booking.dto.ResourceResponse;
import com.template.booking.model.Resource;
import com.template.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    @Operation(summary = "Get all resources")
    public ResponseEntity<List<ResourceResponse>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active resources")
    public ResponseEntity<List<ResourceResponse>> getActiveResources() {
        return ResponseEntity.ok(resourceService.getActiveResources());
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

package com.template.booking.service;

import com.template.booking.dto.ResourceRequest;
import com.template.booking.dto.ResourceResponse;
import com.template.booking.exception.ResourceNotFoundException;
import com.template.booking.model.Resource;
import com.template.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .status(Resource.ResourceStatus.ACTIVE)
                .build();
        return ResourceResponse.fromEntity(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllResources() {
        return resourceRepository.findAll().stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getActiveResources() {
        return resourceRepository.findByStatus(Resource.ResourceStatus.ACTIVE).stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long id) {
        return resourceRepository.findById(id)
                .map(ResourceResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    @Transactional
    public ResourceResponse updateResourceStatus(Long id, Resource.ResourceStatus status) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        resource.setStatus(status);
        return ResourceResponse.fromEntity(resourceRepository.save(resource));
    }
}

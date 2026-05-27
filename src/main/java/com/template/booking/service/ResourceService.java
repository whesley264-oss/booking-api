package com.template.booking.service;

import com.template.booking.dto.ResourceRequest;
import com.template.booking.dto.ResourceResponse;
import com.template.booking.dto.common.PageRequestDto;
import com.template.booking.dto.common.PageResponse;
import com.template.booking.exception.ResourceNotFoundException;
import com.template.booking.model.Resource;
import com.template.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public PageResponse<ResourceResponse> getAllResources(PageRequestDto pageRequest) {
        Page<Resource> page = resourceRepository.findAll(pageRequest.toSpringPageRequest());
        List<ResourceResponse> content = page.getContent().stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getActiveResources(PageRequestDto pageRequest) {
        Page<Resource> page = resourceRepository.findByStatus(
                Resource.ResourceStatus.ACTIVE, pageRequest.toSpringPageRequest());
        List<ResourceResponse> content = page.getContent().stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
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

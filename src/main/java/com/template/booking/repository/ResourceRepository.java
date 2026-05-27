package com.template.booking.repository;

import com.template.booking.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByStatus(Resource.ResourceStatus status);
    Page<Resource> findByStatus(Resource.ResourceStatus status, Pageable pageable);
}

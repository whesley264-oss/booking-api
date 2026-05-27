package com.template.booking.config;

import com.template.booking.model.Booking;
import com.template.booking.model.Resource;
import com.template.booking.repository.BookingRepository;
import com.template.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {
    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;

    @Override
    public void run(String... args) {
        if (resourceRepository.count() > 0) {
            log.info("Data already initialized, skipping seed data");
            return;
        }
        
        log.info("Initializing seed data...");
        
        Resource room1 = resourceRepository.save(Resource.builder()
                .name("Meeting Room Alpha")
                .description("Modern meeting room with video conferencing")
                .capacity(10)
                .location("Building A, Floor 3, Room 301")
                .status(Resource.ResourceStatus.ACTIVE)
                .build());
        
        Resource room2 = resourceRepository.save(Resource.builder()
                .name("Conference Room Beta")
                .description("Large conference room for presentations")
                .capacity(30)
                .location("Building A, Floor 2, Room 201")
                .status(Resource.ResourceStatus.ACTIVE)
                .build());
        
        Resource room3 = resourceRepository.save(Resource.builder()
                .name("Training Room Gamma")
                .description("Training room with projectors")
                .capacity(20)
                .location("Building B, Floor 1, Room 101")
                .status(Resource.ResourceStatus.ACTIVE)
                .build());
        
        Resource room4 = resourceRepository.save(Resource.builder()
                .name("Executive Suite Delta")
                .description("Private executive meeting room")
                .capacity(6)
                .location("Building A, Floor 5, Room 501")
                .status(Resource.ResourceStatus.ACTIVE)
                .build());
        
        resourceRepository.save(Resource.builder()
                .name("Portable Projector A")
                .description("HD portable projector with HDMI")
                .capacity(1)
                .location("Equipment Room")
                .status(Resource.ResourceStatus.ACTIVE)
                .build());
        
        resourceRepository.save(Resource.builder()
                .name("Video Camera Kit")
                .description("Professional video recording kit")
                .capacity(1)
                .location("Equipment Room")
                .status(Resource.ResourceStatus.INACTIVE)
                .build());
        
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        if (tomorrow.getDayOfWeek().getValue() < 6) {
            bookingRepository.save(Booking.builder()
                    .userId("admin")
                    .resource(room1)
                    .startTime(tomorrow.atTime(LocalTime.of(9, 0)))
                    .endTime(tomorrow.atTime(LocalTime.of(10, 0)))
                    .status(Booking.BookingStatus.CONFIRMED)
                    .reason("Morning standup meeting")
                    .build());
            
            bookingRepository.save(Booking.builder()
                    .userId("admin")
                    .resource(room2)
                    .startTime(tomorrow.atTime(LocalTime.of(14, 0)))
                    .endTime(tomorrow.atTime(LocalTime.of(16, 0)))
                    .status(Booking.BookingStatus.CONFIRMED)
                    .reason("Quarterly review presentation")
                    .build());
            
            bookingRepository.save(Booking.builder()
                    .userId("user")
                    .resource(room3)
                    .startTime(tomorrow.atTime(LocalTime.of(10, 0)))
                    .endTime(tomorrow.atTime(LocalTime.of(12, 0)))
                    .status(Booking.BookingStatus.CONFIRMED)
                    .reason("New employee training session")
                    .build());
        }
        
        LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);
        if (dayAfterTomorrow.getDayOfWeek().getValue() < 6) {
            bookingRepository.save(Booking.builder()
                    .userId("user")
                    .resource(room4)
                    .startTime(dayAfterTomorrow.atTime(LocalTime.of(11, 0)))
                    .endTime(dayAfterTomorrow.atTime(LocalTime.of(12, 0)))
                    .status(Booking.BookingStatus.CONFIRMED)
                    .reason("Client meeting")
                    .build());
        }
        
        log.info("Seed data initialized: {} resources, {} bookings", 
                resourceRepository.count(), bookingRepository.count());
    }
}
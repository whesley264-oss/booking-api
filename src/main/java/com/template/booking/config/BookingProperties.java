package com.template.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.time.LocalTime;

@Configuration
@ConfigurationProperties(prefix = "booking")
@Data
public class BookingProperties {
    
    private Business business = new Business();
    private Booking booking = new Booking();
    private Cancellation cancellation = new Cancellation();
    
    @Data
    public static class Business {
        private String hourStart = "08:00";
        private String hourEnd = "18:00";
        
        public LocalTime getStartTime() {
            return LocalTime.parse(hourStart);
        }
        
        public LocalTime getEndTime() {
            return LocalTime.parse(hourEnd);
        }
    }
    
    @Data
    public static class Booking {
        private int minAdvanceHours = 1;
        private int minDurationMinutes = 30;
        private int maxDurationHours = 8;
        private boolean allowWeekends = false;
    }
    
    @Data
    public static class Cancellation {
        private int minHours = 1;
    }
}
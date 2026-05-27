package com.template.booking.event;

import com.template.booking.model.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class BookingEvent extends ApplicationEvent {
    private final Booking booking;

    protected BookingEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }
}

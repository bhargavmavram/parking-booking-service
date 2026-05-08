package com.parking.booking.repo;

import com.parking.booking.domain.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUsername(String username);
    List<Booking> findByLocationId(Long locationId);
}

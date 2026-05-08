package com.parking.booking.repo;

import com.parking.booking.domain.ParkingSession;
import com.parking.booking.domain.ParkingSessionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    List<ParkingSession> findByUsername(String username);
    List<ParkingSession> findByStatus(ParkingSessionStatus status);
    List<ParkingSession> findByVehicleRegistrationNumberIgnoreCase(String vehicleRegistrationNumber);
}

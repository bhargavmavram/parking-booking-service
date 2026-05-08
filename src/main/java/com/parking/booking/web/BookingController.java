package com.parking.booking.web;

import com.parking.booking.domain.*;
import com.parking.booking.repo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class BookingController {
    private final BookingRepository bookings;
    private final ParkingSessionRepository sessions;

    public BookingController(BookingRepository bookings, ParkingSessionRepository sessions) {
        this.bookings = bookings;
        this.sessions = sessions;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return new StatusResponse("parking-booking-service", "UP");
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public Booking createBooking(@Valid @RequestBody BookingRequest request, Authentication authentication) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        Booking booking = new Booking();
        booking.setUsername(authentication.getName());
        booking.setLocationId(request.locationId());
        booking.setSpaceId(request.spaceId());
        booking.setVehicleRegistrationNumber(request.vehicleRegistrationNumber());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.REQUESTED);
        return bookings.save(booking);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public List<Booking> allBookings(@RequestParam(required = false) Long locationId) {
        return locationId == null ? bookings.findAll() : bookings.findByLocationId(locationId);
    }

    @GetMapping("/bookings/me")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public List<Booking> myBookings(Authentication authentication) {
        return bookings.findByUsername(authentication.getName());
    }

    @GetMapping("/bookings/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public Booking booking(@PathVariable Long bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> notFound("Booking not found"));
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public Booking confirmBooking(@PathVariable Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> notFound("Booking not found"));
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookings.save(booking);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public Booking cancelBooking(@PathVariable Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> notFound("Booking not found"));
        booking.setStatus(BookingStatus.CANCELLED);
        return bookings.save(booking);
    }

    @PostMapping("/sessions/start")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public ParkingSession startSession(@Valid @RequestBody SessionStartRequest request, Authentication authentication) {
        ParkingSession session = new ParkingSession();
        session.setUsername(authentication.getName());
        session.setLocationId(request.locationId());
        session.setSpaceId(request.spaceId());
        session.setVehicleRegistrationNumber(request.vehicleRegistrationNumber());
        session.setStartedAt(Instant.now());
        session.setStatus(ParkingSessionStatus.ACTIVE);
        return sessions.save(session);
    }

    @PostMapping("/sessions/start-with-booking/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public ParkingSession startSessionWithBooking(@PathVariable Long bookingId, Authentication authentication) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> notFound("Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking must be confirmed before session start");
        }
        ParkingSession session = new ParkingSession();
        session.setBookingId(booking.getId());
        session.setUsername(authentication.getName());
        session.setLocationId(booking.getLocationId());
        session.setSpaceId(booking.getSpaceId());
        session.setVehicleRegistrationNumber(booking.getVehicleRegistrationNumber());
        session.setStartedAt(Instant.now());
        session.setStatus(ParkingSessionStatus.ACTIVE);
        return sessions.save(session);
    }

    @PostMapping("/sessions/start-by-plate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER')")
    public ParkingSession startSessionByPlate(@Valid @RequestBody PlateSessionStartRequest request) {
        ParkingSession session = new ParkingSession();
        session.setUsername("camera:" + request.vehicleRegistrationNumber());
        session.setLocationId(request.locationId());
        session.setSpaceId(request.spaceId());
        session.setVehicleRegistrationNumber(request.vehicleRegistrationNumber());
        session.setStartedAt(Instant.now());
        session.setStatus(ParkingSessionStatus.ACTIVE);
        return sessions.save(session);
    }

    @PostMapping("/sessions/{sessionId}/end")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public ParkingSession endSession(@PathVariable Long sessionId) {
        ParkingSession session = sessions.findById(sessionId).orElseThrow(() -> notFound("Session not found"));
        session.setEndedAt(Instant.now());
        session.setStatus(ParkingSessionStatus.ENDED);
        return sessions.save(session);
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public List<ParkingSession> activeSessions() {
        return sessions.findByStatus(ParkingSessionStatus.ACTIVE);
    }

    @GetMapping("/sessions/by-vehicle/{registrationNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','PARKING_OWNER','SUPPORT')")
    public List<ParkingSession> sessionsByVehicle(@PathVariable String registrationNumber) {
        return sessions.findByVehicleRegistrationNumberIgnoreCase(registrationNumber);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public record StatusResponse(String service, String status) {}
    public record BookingRequest(@NotNull Long locationId, Long spaceId, @NotBlank String vehicleRegistrationNumber, @NotNull @Future Instant startTime, @NotNull @Future Instant endTime) {}
    public record SessionStartRequest(@NotNull Long locationId, Long spaceId, @NotBlank String vehicleRegistrationNumber) {}
    public record PlateSessionStartRequest(@NotNull Long locationId, Long spaceId, @NotBlank String vehicleRegistrationNumber) {}
}

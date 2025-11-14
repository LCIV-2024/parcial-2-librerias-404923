package com.example.libreria.repository;

import com.example.libreria.model.Reservation;
import com.example.libreria.model.Reservation.ReservationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(Long userId);
    
    List<Reservation> findByStatus(ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.status = ReservationStatus.ACTIVE")
    List<Reservation> findOverdueReservations();
}


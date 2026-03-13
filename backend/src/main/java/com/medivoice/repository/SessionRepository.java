package com.medivoice.repository;

import com.medivoice.model.SymptomSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SymptomSession, String> {

    List<SymptomSession> findByStatusOrderByCreatedAtDesc(SymptomSession.SessionStatus status);

    List<SymptomSession> findAllByOrderByCreatedAtDesc();
}

package com.example.pifagor.repository;

import com.example.pifagor.model.RegistrationRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
    @Transactional
    List<RegistrationRequest> findByApprovedFalse(); // всі заявки, які чекають підтвердження
    @Transactional
    List<RegistrationRequest> findByGroupIdAndApprovedTrue(Long groupId);
}


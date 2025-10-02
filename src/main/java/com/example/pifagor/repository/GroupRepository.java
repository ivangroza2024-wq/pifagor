package com.example.pifagor.repository;

import com.example.pifagor.model.Group;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Transactional
    Group findByName(String name);
}


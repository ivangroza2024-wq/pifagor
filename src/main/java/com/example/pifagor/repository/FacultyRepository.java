package com.example.pifagor.repository;

import com.example.pifagor.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    // Знайти факультет за назвою
    Optional<Faculty> findByName(String name);

    // Можна додати інші методи за потреби, наприклад, за факультетом певного учня
    // List<Faculty> findByStudentsContaining(User student);
}

package com.example.pifagor.repository;

import com.example.pifagor.model.Faculty;
import com.example.pifagor.model.Group;
import com.example.pifagor.model.Role;
import com.example.pifagor.model.User;
import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Transactional
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    User findByTelegramId(Long telegramId);

    // Знайти всіх користувачів певного факультету
    List<User> findByFaculty(Faculty faculty);

    // Знайти всіх користувачів певної групи
    List<User> findByGroupId(Long groupId);
    int countByFaculty(Faculty faculty);
    List<User> findByGroupAndRole(Group group, Role role);

    List<User> findByName(String name);
}

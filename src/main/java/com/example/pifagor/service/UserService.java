package com.example.pifagor.service;



import com.example.pifagor.model.Faculty;
import lombok.RequiredArgsConstructor;
import com.example.pifagor.model.User;
import com.example.pifagor.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final Map<Long, String> pendingHomeworkDates = new HashMap<>();
    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId); // повертає null, якщо не знайдено
    }
    // ✅ додати бали учню і його факультету
    public void addFacultyPoints(User user, int points) {
        if (user == null || user.getFaculty() == null) return;

        // додаємо учню
        user.addFacultyPoints(points);
        userRepository.save(user);

        // додаємо факультету
        Faculty faculty = user.getFaculty();
        faculty.setPoints(faculty.getPoints() + points);
        // треба через FacultyRepository зберігати, але краще це робити у FacultyService
    }

    // ✅ скинути все (для адміна)
    public void resetFacultiesAndPoints(List<User> allUsers, List<Faculty> allFaculties) {
        for (User u : allUsers) {
            u.setFaculty(null);
            u.setFacultyPoints(0);
        }
        for (Faculty f : allFaculties) {
            f.setPoints(0);
        }
        userRepository.saveAll(allUsers);
    }

    public void setPendingHomeworkDate(Long userId, String dateTime) {
        pendingHomeworkDates.put(userId, dateTime);
    }
    // Можна тримати в пам’яті або зберігати у БД
    private final Map<Long, Set<String>> submittedHomework = new HashMap<>();

    public boolean hasSubmittedHomework(User user, String date) {
        return submittedHomework
                .getOrDefault(user.getTelegramId(), new HashSet<>())
                .contains(date);
    }

    public void markHomeworkSubmitted(User user, String date) {
        submittedHomework
                .computeIfAbsent(user.getTelegramId(), k -> new HashSet<>())
                .add(date);
    }

    public String getPendingHomeworkDate(Long userId) {
        return pendingHomeworkDates.get(userId);
    }

    public void clearPendingHomeworkDate(Long userId) {
        pendingHomeworkDates.remove(userId);
    }
}

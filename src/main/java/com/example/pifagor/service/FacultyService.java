package com.example.pifagor.service;

import com.example.pifagor.model.Faculty;
import com.example.pifagor.model.User;
import com.example.pifagor.repository.FacultyRepository;
import com.example.pifagor.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class FacultyService {
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;

    public FacultyService(FacultyRepository facultyRepository, UserRepository userRepository) {
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
    }

    public Faculty assignRandomFaculty(User user) {
        List<Faculty> faculties = facultyRepository.findAll();
        if (faculties.isEmpty()) throw new RuntimeException("Факультети ще не створені!");

        Faculty chosen = faculties.get(new Random().nextInt(faculties.size()));
        user.setFaculty(chosen);
        user.setFacultyPoints(0);
        userRepository.save(user);
        return chosen;
    }

    public List<User> getTopStudents(Faculty faculty, int limit) {
        return userRepository.findAll().stream()
                .filter(u -> faculty.equals(u.getFaculty()))
                .sorted((a, b) -> Integer.compare(b.getFacultyPoints(), a.getFacultyPoints()))
                .limit(limit)
                .toList();
    }

    public List<Faculty> getAllFaculties() {
        return facultyRepository.findAll();
    }
}


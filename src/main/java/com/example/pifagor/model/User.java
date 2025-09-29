package com.example.pifagor.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "telegram_id")
    private Long telegramId;
    private String name;
    @Enumerated(EnumType.STRING)
    private Role role;
    @ManyToOne
    private Group group;
    @ManyToOne
    private Faculty faculty;
    private Long parentTelegramId; // для прив'язки дитини до батька
    private boolean approved = false;
    private int facultyPoints = 0; // ✅ бали учня у битві факультетів
    // ✅ зв’язок з дитиною (для батьків)
    @ManyToOne
    @JoinColumn(name = "child_id")
    private User child;

    public int getFacultyPoints() {
        return facultyPoints;
    }

    public void setFacultyPoints(int facultyPoints) {
        this.facultyPoints = facultyPoints;
    }

    public void addFacultyPoints(int points) {
        this.facultyPoints += points;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Long getParentTelegramId() {
        return parentTelegramId;
    }

    public void setParentTelegramId(Long parentTelegramId) {
        this.parentTelegramId = parentTelegramId;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

}


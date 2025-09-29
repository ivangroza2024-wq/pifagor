package com.example.pifagor.model;

import jakarta.persistence.*;

@Entity
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;      // Назва факультету (українською)
    private String color;     // Колір (наприклад, HEX-код #FF0000)
    private String symbol;    // Символ або емодзі факультету 🦁
    private int points;       // Бали факультету

    private String imageUrl;  // Посилання або шлях до фото факультету

    public Faculty() {}

    public Faculty(String name, String color, String symbol, String imageUrl) {
        this.name = name;
        this.color = color;
        this.symbol = symbol;
        this.imageUrl = imageUrl;
        this.points = 0;
    }

    // --- getters & setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

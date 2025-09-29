package com.example.pifagor.util;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Утиліти для нормалізації дат, повертають рядок у форматі dd.MM.yyyy
 */
public class DateUtil {

    private static final DateTimeFormatter SHEETS_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DMY_DOT = DateTimeFormatter.ofPattern("d.M.yyyy");
    private static final DateTimeFormatter DMY_DASH = DateTimeFormatter.ofPattern("d-M-yyyy");
    // для випадків з часом (ми просто беремо дату частину)
    private static final DateTimeFormatter DMY_DOT_WITH_TIME = DateTimeFormatter.ofPattern("d.M.yyyy H:mm");
    private static final DateTimeFormatter DMY_DASH_WITH_TIME = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

    /**
     * Парсить вхідну дату в LocalDate. Підтримує:
     * - "dd.MM.yyyy" або "d.M.yyyy"
     * - "dd-MM-yyyy" або "d-M-yyyy"
     * - "dd.MM" або "dd-MM" (додає поточний рік)
     * - рядки з часом "dd.MM.yyyy HH:mm" або "dd-MM-yyyy HH:mm" (ігнорує час)
     *
     * @param input будь-який рядок дати
     * @return LocalDate
     */
    public static LocalDate parseDate(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Порожній рядок дати");
        }

        String raw = input.trim();
        // прибираємо зайві пробіли
        raw = raw.replaceAll("\\s+", " ");

        // візьмемо першу частину до пробілу (щоб відсікати час): "11-09-2025 16:00" -> "11-09-2025"
        String datePart = raw.split(" ")[0];

        // уніфікуємо роздільники: замінимо '/' на '.' або '-' ? Ми краще приведемо '-' -> '.'
        // але збережемо обробку обох варіантів нижче
        if (datePart.contains("/")) {
            datePart = datePart.replace('/', '.');
        }

        // якщо маємо дефіс, спробуємо парсити як dash або замінити на крапку і парсити
        // спробуємо декілька стратегій по черзі — найбільш прості/ймовірні першими
        // 1) вже є рік і формат з точками (dd.MM.yyyy або d.M.yyyy)
        try {
            if (datePart.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")) {
                return LocalDate.parse(datePart, DMY_DOT);
            }
        } catch (DateTimeParseException ignored) { }

        // 2) dash with year
        try {
            if (datePart.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
                return LocalDate.parse(datePart, DMY_DASH);
            }
        } catch (DateTimeParseException ignored) { }

        // 3) has time and dot or dash: "dd.MM.yyyy HH:mm" or "dd-MM-yyyy HH:mm"
        // (we already split by space so time is removed; but just in case)
        try {
            if (datePart.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")) {
                return LocalDate.parse(datePart, DMY_DOT);
            }
        } catch (DateTimeParseException ignored) { }

        // 4) no year: dd.MM  -> add current year
        if (datePart.matches("\\d{1,2}\\.\\d{1,2}")) {
            String withYear = datePart + "." + Year.now().getValue();
            try {
                return LocalDate.parse(withYear, DMY_DOT);
            } catch (DateTimeParseException ignored) { }
        }

        // 5) no year with dash: dd-MM -> add current year
        if (datePart.matches("\\d{1,2}-\\d{1,2}")) {
            String withYear = datePart.replace('-', '.') + "." + Year.now().getValue();
            try {
                return LocalDate.parse(withYear, DMY_DOT);
            } catch (DateTimeParseException ignored) { }
        }

        // 6) last resort: try several flexible patterns
        String[] tryPatterns = {
                "d.M.yyyy",
                "d-M-yyyy",
                "d.M",
                "d-M"
        };
        for (String p : tryPatterns) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
                if (p.contains("yyyy")) {
                    return LocalDate.parse(datePart, fmt);
                } else {
                    // без року: додаємо рік
                    String withYear = datePart + "." + Year.now().getValue();
                    return LocalDate.parse(withYear, DMY_DOT);
                }
            } catch (DateTimeParseException ignored) { }
        }

        throw new IllegalArgumentException("Невідомий формат дати: " + input);
    }

    /**
     * Повертає рядок у форматі, який використовується в Google Sheets: dd.MM.yyyy
     */
    public static String toSheetsFormat(String input) {
        Objects.requireNonNull(input, "input date is null");
        LocalDate d = parseDate(input);
        return d.format(SHEETS_FORMAT);
    }
}

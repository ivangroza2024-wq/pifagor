package com.example.pifagor.service;

        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Service;

        import javax.sql.DataSource;
        import java.sql.*;
        import java.time.LocalDate;

@Service
public class PhraseOfDayService {

    private final DataSource dataSource;

    @Autowired
    public PhraseOfDayService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getPhraseOfDay() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            LocalDate today = LocalDate.now();

            PreparedStatement ps = connection.prepareStatement(
                    "SELECT last_phrase_id, last_update_date FROM app_state WHERE id = 1"
            );
            ResultSet rs = ps.executeQuery();

            int lastPhraseId = 0;
            LocalDate lastUpdateDate = null;

            if (rs.next()) {
                lastPhraseId = rs.getInt("last_phrase_id");
                Date date = rs.getDate("last_update_date");
                if (date != null) lastUpdateDate = date.toLocalDate();
            }

            if (lastUpdateDate == null || !lastUpdateDate.equals(today)) {
                int nextId = getNextPhraseId(connection, lastPhraseId);

                PreparedStatement update = connection.prepareStatement(
                        "UPDATE app_state SET last_phrase_id = ?, last_update_date = ? WHERE id = 1"
                );
                update.setInt(1, nextId);
                update.setDate(2, Date.valueOf(today));
                update.executeUpdate();

                return getPhraseById(connection, nextId);
            }

            return getPhraseById(connection, lastPhraseId);
        }
    }

    private int getNextPhraseId(Connection connection, int currentId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM phrases_of_day");
        ResultSet rs = ps.executeQuery();
        rs.next();
        int total = rs.getInt(1);

        int next = currentId + 1;
        if (next > total) next = 1;
        return next;
    }

    private String getPhraseById(Connection connection, int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT phrase_text FROM phrases_of_day WHERE id = ?"
        );
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("phrase_text");
        }
        return "💭 Фраза дня ще не додана!";
    }
}

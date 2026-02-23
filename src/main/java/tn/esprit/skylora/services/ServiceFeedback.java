package tn.esprit.skylora.services;

import tn.esprit.skylora.entities.Feedback;
import tn.esprit.skylora.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceFeedback {
    private Connection cnx;

    public ServiceFeedback() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    public void ajouter(Feedback f) throws SQLException {
        String sql = "INSERT INTO feedback(ticket_id, rating, comment, date_creation) VALUES(?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, f.getTicketId());
        ps.setInt(2, f.getRating());
        ps.setString(3, f.getComment());
        ps.setTimestamp(4, Timestamp.valueOf(f.getDateCreation()));
        ps.executeUpdate();
    }

    public void modifier(Feedback f) throws SQLException {
        String sql = "UPDATE feedback SET rating = ?, comment = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, f.getRating());
        ps.setString(2, f.getComment());
        ps.setInt(3, f.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM feedback WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Feedback> afficher() throws SQLException {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT * FROM feedback";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Feedback f = new Feedback();
            f.setId(rs.getInt("id"));
            f.setTicketId(rs.getInt("ticket_id"));
            f.setRating(rs.getInt("rating"));
            f.setComment(rs.getString("comment"));
            Timestamp ts = rs.getTimestamp("date_creation");
            f.setDateCreation(ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now());
            list.add(f);
        }
        return list;
    }

    public Feedback getFeedbackByTicketId(int ticketId) throws SQLException {
        String sql = "SELECT * FROM feedback WHERE ticket_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, ticketId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Feedback f = new Feedback();
            f.setId(rs.getInt("id"));
            f.setTicketId(rs.getInt("ticket_id"));
            f.setRating(rs.getInt("rating"));
            f.setComment(rs.getString("comment"));
            Timestamp ts = rs.getTimestamp("date_creation");
            f.setDateCreation(ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now());
            return f;
        }
        return null;
    }

    // --- Statistics using Java Streams ---

    public double getAverageRating() throws SQLException {
        return afficher().stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
    }

    public long getCountByRating(int rating) throws SQLException {
        return afficher().stream()
                .filter(f -> f.getRating() == rating)
                .count();
    }
}

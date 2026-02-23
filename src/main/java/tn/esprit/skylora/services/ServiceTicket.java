package tn.esprit.skylora.services;

import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceTicket {

    Connection cnx;

    public ServiceTicket() {
        cnx = MyConnection.getInstance().getConnection();
    }

    public void ajouter(Ticket t) throws SQLException {

        String sql = "INSERT INTO ticket(utilisateur_id, subject, categorie, priorite, statut, description, date_creation) VALUES(?,?,?,?,?,?,?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, t.getUtilisateurId());
        ps.setString(2, t.getSubject());
        ps.setString(3, t.getCategorie());
        ps.setString(4, t.getPriorite());
        ps.setString(5, t.getStatut());
        ps.setString(6, t.getDescription());
        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));

        ps.executeUpdate();
    }

    public void modifier(Ticket t) throws SQLException {

        String sql = "UPDATE ticket SET subject=?, categorie=?, priorite=?, statut=?, description=?, date_resolution=?, agent_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getSubject());
        ps.setString(2, t.getCategorie());
        ps.setString(3, t.getPriorite());
        ps.setString(4, t.getStatut());
        ps.setString(5, t.getDescription());
        ps.setTimestamp(6, t.getDateResolution() != null ? Timestamp.valueOf(t.getDateResolution()) : null);
        if (t.getAgentId() != null)
            ps.setInt(7, t.getAgentId());
        else
            ps.setNull(7, Types.INTEGER);
        ps.setInt(8, t.getId());

        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM ticket WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Ticket> afficher() throws SQLException {

        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(mapResultSetToTicket(rs));
        }
        return list;
    }

    public List<Ticket> getTicketsByUserId(int userId) throws SQLException {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket WHERE utilisateur_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToTicket(rs));
        }
        return list;
    }

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE ticket SET statut = ?, date_resolution = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ps.setTimestamp(2,
                (status.equals("RESOLVED") || status.equals("CLOSED")) ? Timestamp.valueOf(LocalDateTime.now()) : null);
        ps.setInt(3, id);
        ps.executeUpdate();
    }

    public void updatePriority(int id, String priority) throws SQLException {
        String sql = "UPDATE ticket SET priorite = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, priority);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public void assignTicket(int id, int agentId) throws SQLException {
        String sql = "UPDATE ticket SET agent_id = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, agentId);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setId(rs.getInt("id"));
        t.setUtilisateurId(rs.getInt("utilisateur_id"));
        t.setSubject(rs.getString("subject"));
        t.setCategorie(rs.getString("categorie"));
        t.setPriorite(rs.getString("priorite"));
        t.setStatut(rs.getString("statut"));
        t.setDescription(rs.getString("description"));

        Timestamp creation = rs.getTimestamp("date_creation");
        if (creation != null) {
            t.setDateCreation(creation.toLocalDateTime());
        }

        Timestamp resolution = rs.getTimestamp("date_resolution");
        if (resolution != null) {
            t.setDateResolution(resolution.toLocalDateTime());
        }

        int agentId = rs.getInt("agent_id");
        if (!rs.wasNull())
            t.setAgentId(agentId);
        return t;
    }

    // --- Statistiques avec Java Streams ---

    public long getTotalTickets() throws SQLException {
        return afficher().stream().count();
    }

    public long getCountByStatus(String status) throws SQLException {
        return afficher().stream()
                .filter(t -> t.getStatut().equalsIgnoreCase(status))
                .count();
    }

    public long getCountByPriority(String priority) throws SQLException {
        return afficher().stream()
                .filter(t -> t.getPriorite().equalsIgnoreCase(priority))
                .count();
    }

    public java.util.Map<String, Long> getCountByCategory() throws SQLException {
        return afficher().stream()
                .collect(java.util.stream.Collectors.groupingBy(Ticket::getCategorie,
                        java.util.stream.Collectors.counting()));
    }
}

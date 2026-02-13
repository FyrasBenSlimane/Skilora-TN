package tn.esprit.skylora.services;

import tn.esprit.skylora.entities.MessageTicket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageTicketService {

    private Connection conn;

    public MessageTicketService(Connection conn) {
        this.conn = conn;
    }

    // Ajouter un message
    public boolean ajouterMessage(MessageTicket message) {
        String sql = "INSERT INTO ticket_messages(ticket_id, utilisateur_id, contenu, is_internal, attachments_json) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, message.getTicketId());
            ps.setInt(2, message.getUtilisateurId());
            ps.setString(3, message.getContenu());
            ps.setBoolean(4, message.isInternal());
            ps.setString(5, message.getAttachmentsJson());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Modifier un message
    public boolean modifierMessage(MessageTicket message) {
        String sql = "UPDATE ticket_messages SET contenu=?, is_internal=?, attachments_json=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message.getContenu());
            ps.setBoolean(2, message.isInternal());
            ps.setString(3, message.getAttachmentsJson());
            ps.setInt(4, message.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Supprimer un message
    public boolean supprimerMessage(int id) {
        String sql = "DELETE FROM ticket_messages WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Lister tous les messages
    public List<MessageTicket> getAllMessages() {
        List<MessageTicket> messages = new ArrayList<>();
        String sql = "SELECT * FROM ticket_messages";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapResultSetToMessageTicket(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<MessageTicket> getMessagesByTicketId(int ticketId) {
        List<MessageTicket> messages = new ArrayList<>();
        String sql = "SELECT * FROM ticket_messages WHERE ticket_id = ? ORDER BY date_envoi ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessageTicket(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    private MessageTicket mapResultSetToMessageTicket(ResultSet rs) throws SQLException {
        MessageTicket m = new MessageTicket();
        m.setId(rs.getInt("id"));
        m.setTicketId(rs.getInt("ticket_id"));
        m.setUtilisateurId(rs.getInt("utilisateur_id"));
        m.setContenu(rs.getString("contenu"));
        m.setInternal(rs.getBoolean("is_internal"));
        m.setAttachmentsJson(rs.getString("attachments_json"));
        Timestamp ts = rs.getTimestamp("date_envoi");
        if (ts != null)
            m.setDateEnvoi(ts.toLocalDateTime());
        return m;
    }
}

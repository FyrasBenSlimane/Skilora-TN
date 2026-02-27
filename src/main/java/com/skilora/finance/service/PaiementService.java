package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.model.Paiement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PaiementService (CRUD MySQL)
 *
 * IMPORTANT :
 * - Utilise la même logique de connexion que FinanceService (DatabaseConfig + injection ConnectionFactory).
 * - Table attendue :
 *   CREATE TABLE paiement (
 *     id INT AUTO_INCREMENT PRIMARY KEY,
 *     montant DOUBLE NOT NULL,
 *     date_heure DATETIME NOT NULL,
 *     statut VARCHAR(20) NOT NULL,
 *     stripe_payment_id VARCHAR(100),
 *     reference_projet VARCHAR(100),
 *     nom_beneficiaire VARCHAR(100)
 *   );
 */
public class PaiementService {

    private static PaiementService instance;
    private ConnectionFactory connectionFactory;

    public interface ConnectionFactory {
        Connection getConnection() throws SQLException;
    }

    private PaiementService() {
        this.connectionFactory = () -> DatabaseConfig.getInstance().getConnection();
    }

    public PaiementService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public static synchronized PaiementService getInstance() {
        if (instance == null) {
            instance = new PaiementService();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return connectionFactory.getConnection();
    }

    /**
     * INSERT dans table paiement.
     * @return true si insertion OK
     */
    public boolean ajouterPaiement(Paiement p) throws SQLException {
        if (p == null) return false;
        String sql = "INSERT INTO paiement (montant, date_heure, statut, stripe_payment_id, reference_projet, nom_beneficiaire) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) {
                throw new SQLException("Connexion DB indisponible (OFFLINE MODE).");
            }
            stmt.setDouble(1, p.getMontant());
            LocalDateTime dt = p.getDateHeure() != null ? p.getDateHeure() : LocalDateTime.now();
            stmt.setTimestamp(2, Timestamp.valueOf(dt));
            stmt.setString(3, p.getStatut() != null ? p.getStatut().name() : Paiement.Statut.PENDING.name());
            stmt.setString(4, p.getStripePaymentId());
            stmt.setString(5, p.getReferenceProjet());
            stmt.setString(6, p.getNomBeneficiaire());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * SELECT paiements d'un projet.
     */
    public List<Paiement> getPaiementsByProjet(String referenceProjet) throws SQLException {
        List<Paiement> list = new ArrayList<>();
        String sql = "SELECT id, montant, date_heure, statut, stripe_payment_id, reference_projet, nom_beneficiaire " +
                "FROM paiement WHERE reference_projet = ? ORDER BY date_heure DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (conn == null) {
                throw new SQLException("Connexion DB indisponible (OFFLINE MODE).");
            }
            stmt.setString(1, referenceProjet);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Paiement p = new Paiement();
                p.setId(rs.getInt("id"));
                p.setMontant(rs.getDouble("montant"));
                Timestamp ts = rs.getTimestamp("date_heure");
                p.setDateHeure(ts != null ? ts.toLocalDateTime() : null);
                String st = rs.getString("statut");
                if (st != null) {
                    try {
                        p.setStatut(Paiement.Statut.valueOf(st));
                    } catch (IllegalArgumentException ex) {
                        p.setStatut(Paiement.Statut.PENDING);
                    }
                } else {
                    p.setStatut(Paiement.Statut.PENDING);
                }
                p.setStripePaymentId(rs.getString("stripe_payment_id"));
                p.setReferenceProjet(rs.getString("reference_projet"));
                p.setNomBeneficiaire(rs.getString("nom_beneficiaire"));
                list.add(p);
            }
        }
        return list;
    }
}


package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    private static volatile CompanyService instance;

    private CompanyService() {}

    public static CompanyService getInstance() {
        if (instance == null) {
            synchronized (CompanyService.class) {
                if (instance == null) {
                    instance = new CompanyService();
                }
            }
        }
        return instance;
    }

    public int create(Company company) {
        if (company == null) throw new IllegalArgumentException("Company cannot be null");
        if (company.getName() == null || company.getName().isBlank()) throw new IllegalArgumentException("Company name is required");
        if (company.getOwnerId() <= 0) throw new IllegalArgumentException("Valid owner ID is required");
        String sql = """
            INSERT INTO companies (owner_id, name, country, industry,
                website, logo_url, verified, size)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, company.getOwnerId());
            stmt.setString(2, company.getName());
            stmt.setString(3, company.getCountry());
            stmt.setString(4, company.getIndustry());
            stmt.setString(5, company.getWebsite());
            stmt.setString(6, company.getLogoUrl());
            stmt.setBoolean(7, company.isVerified());
            stmt.setString(8, company.getSize());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Company created: id={}, name={}", id, company.getName());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating company: {}", e.getMessage(), e);
        }
        return -1;
    }

    public Company findById(int id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapCompany(rs);
        } catch (SQLException e) {
            logger.error("Error finding company {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    public Company findByOwner(int ownerId) {
        String sql = "SELECT * FROM companies WHERE owner_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ownerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapCompany(rs);
        } catch (SQLException e) {
            logger.error("Error finding company for owner {}: {}", ownerId, e.getMessage(), e);
        }
        return null;
    }

    public List<Company> findAll() {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM companies ORDER BY name";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapCompany(rs));
        } catch (SQLException e) {
            logger.error("Error finding all companies: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<Company> findVerified() {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM companies WHERE verified = TRUE ORDER BY name";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapCompany(rs));
        } catch (SQLException e) {
            logger.error("Error finding verified companies: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<Company> searchByName(String query) {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM companies WHERE name LIKE ? ORDER BY name";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapCompany(rs));
        } catch (SQLException e) {
            logger.error("Error searching companies: {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean update(Company company) {
        String sql = """
            UPDATE companies SET name = ?, country = ?, industry = ?,
                website = ?, logo_url = ?, size = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, company.getName());
            stmt.setString(2, company.getCountry());
            stmt.setString(3, company.getIndustry());
            stmt.setString(4, company.getWebsite());
            stmt.setString(5, company.getLogoUrl());
            stmt.setString(6, company.getSize());
            stmt.setInt(7, company.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating company: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean verify(int id) {
        String sql = "UPDATE companies SET verified = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error verifying company: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM companies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting company: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── Private helpers ──

    private Company mapCompany(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setId(rs.getInt("id"));
        c.setOwnerId(rs.getInt("owner_id"));
        c.setName(rs.getString("name"));
        c.setCountry(rs.getString("country"));
        c.setIndustry(rs.getString("industry"));
        c.setWebsite(rs.getString("website"));
        c.setLogoUrl(rs.getString("logo_url"));
        c.setVerified(rs.getBoolean("verified"));
        c.setSize(rs.getString("size"));
        return c;
    }
}

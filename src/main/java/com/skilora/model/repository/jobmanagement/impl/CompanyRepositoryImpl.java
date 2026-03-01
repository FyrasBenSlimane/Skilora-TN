package com.skilora.model.repository.jobmanagement.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.jobmanagement.Company;
import com.skilora.model.repository.jobmanagement.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompanyRepositoryImpl implements CompanyRepository {
    private static final Logger logger = LoggerFactory.getLogger(CompanyRepositoryImpl.class);

    @Override
    public Optional<Company> findById(int id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCompany(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding company by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Company> findAll() {
        List<Company> companies = new ArrayList<>();
        String sql = "SELECT * FROM companies";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                companies.add(mapResultSetToCompany(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all companies", e);
        }
        return companies;
    }

    @Override
    public List<Company> findByOwnerId(int ownerId) {
        List<Company> companies = new ArrayList<>();
        String sql = "SELECT * FROM companies WHERE owner_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    companies.add(mapResultSetToCompany(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding companies by owner_id: {}", ownerId, e);
        }
        return companies;
    }

    @Override
    public void save(Company company) {
        String sql = "INSERT INTO companies (owner_id, name, country, industry, website, logo_url, is_verified, size) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (company.getOwnerId() != null) pstmt.setInt(1, company.getOwnerId()); else pstmt.setNull(1, Types.INTEGER);
            pstmt.setString(2, company.getName());
            pstmt.setString(3, company.getCountry());
            pstmt.setString(4, company.getIndustry());
            pstmt.setString(5, company.getWebsite());
            pstmt.setString(6, company.getLogoUrl());
            pstmt.setBoolean(7, company.isVerified());
            pstmt.setString(8, company.getSize());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        company.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving company", e);
        }
    }

    @Override
    public void update(Company company) {
        String sql = "UPDATE companies SET owner_id = ?, name = ?, country = ?, industry = ?, website = ?, logo_url = ?, is_verified = ?, size = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (company.getOwnerId() != null) pstmt.setInt(1, company.getOwnerId()); else pstmt.setNull(1, Types.INTEGER);
            pstmt.setString(2, company.getName());
            pstmt.setString(3, company.getCountry());
            pstmt.setString(4, company.getIndustry());
            pstmt.setString(5, company.getWebsite());
            pstmt.setString(6, company.getLogoUrl());
            pstmt.setBoolean(7, company.isVerified());
            pstmt.setString(8, company.getSize());
            pstmt.setInt(9, company.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating company", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM companies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting company", e);
        }
    }

    private Company mapResultSetToCompany(ResultSet rs) throws SQLException {
        Company company = new Company();
        company.setId(rs.getInt("id"));
        int ownerId = rs.getInt("owner_id");
        company.setOwnerId(rs.wasNull() ? null : ownerId);
        company.setName(rs.getString("name"));
        company.setCountry(rs.getString("country"));
        company.setIndustry(rs.getString("industry"));
        company.setWebsite(rs.getString("website"));
        company.setLogoUrl(rs.getString("logo_url"));
        company.setVerified(rs.getBoolean("is_verified"));
        company.setSize(rs.getString("size"));
        return company;
    }
}

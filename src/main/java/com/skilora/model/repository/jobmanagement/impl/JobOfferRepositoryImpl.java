package com.skilora.model.repository.jobmanagement.impl;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.jobmanagement.JobOffer;
import com.skilora.model.enums.JobStatus;
import com.skilora.model.enums.WorkType;
import com.skilora.model.repository.jobmanagement.JobOfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobOfferRepositoryImpl implements JobOfferRepository {
    private static final Logger logger = LoggerFactory.getLogger(JobOfferRepositoryImpl.class);

    @Override
    public Optional<JobOffer> findById(int id) {
        String sql = "SELECT * FROM job_offers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToJobOffer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding job offer by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<JobOffer> findAll() {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT * FROM job_offers ORDER BY posted_date DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all job offers", e);
        }
        return jobOffers;
    }

    @Override
    public List<JobOffer> findByCompanyId(int companyId) {
        List<JobOffer> jobOffers = new ArrayList<>();
        String sql = "SELECT * FROM job_offers WHERE company_id = ? ORDER BY posted_date DESC";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, companyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    jobOffers.add(mapResultSetToJobOffer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding job offers by company_id: {}", companyId, e);
        }
        return jobOffers;
    }

    @Override
    public void save(JobOffer jobOffer) {
        String sql = "INSERT INTO job_offers (company_id, title, description, requirements, min_salary, max_salary, currency, location, work_type, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, jobOffer.getCompanyId());
            pstmt.setString(2, jobOffer.getTitle());
            pstmt.setString(3, jobOffer.getDescription());
            pstmt.setString(4, jobOffer.getRequirements());
            pstmt.setBigDecimal(5, jobOffer.getMinSalary());
            pstmt.setBigDecimal(6, jobOffer.getMaxSalary());
            pstmt.setString(7, jobOffer.getCurrency());
            pstmt.setString(8, jobOffer.getLocation());
            pstmt.setString(9, jobOffer.getWorkType() != null ? jobOffer.getWorkType().name() : null);
            pstmt.setDate(10, jobOffer.getDeadline() != null ? Date.valueOf(jobOffer.getDeadline()) : null);
            pstmt.setString(11, jobOffer.getStatus() != null ? jobOffer.getStatus().name() : JobStatus.OPEN.name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        jobOffer.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving job offer", e);
        }
    }

    @Override
    public void update(JobOffer jobOffer) {
        String sql = "UPDATE job_offers SET company_id = ?, title = ?, description = ?, requirements = ?, min_salary = ?, max_salary = ?, currency = ?, location = ?, work_type = ?, deadline = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, jobOffer.getCompanyId());
            pstmt.setString(2, jobOffer.getTitle());
            pstmt.setString(3, jobOffer.getDescription());
            pstmt.setString(4, jobOffer.getRequirements());
            pstmt.setBigDecimal(5, jobOffer.getMinSalary());
            pstmt.setBigDecimal(6, jobOffer.getMaxSalary());
            pstmt.setString(7, jobOffer.getCurrency());
            pstmt.setString(8, jobOffer.getLocation());
            pstmt.setString(9, jobOffer.getWorkType() != null ? jobOffer.getWorkType().name() : null);
            pstmt.setDate(10, jobOffer.getDeadline() != null ? Date.valueOf(jobOffer.getDeadline()) : null);
            pstmt.setString(11, jobOffer.getStatus() != null ? jobOffer.getStatus().name() : JobStatus.OPEN.name());
            pstmt.setInt(12, jobOffer.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating job offer", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM job_offers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting job offer", e);
        }
    }

    private JobOffer mapResultSetToJobOffer(ResultSet rs) throws SQLException {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(rs.getInt("id"));
        jobOffer.setCompanyId(rs.getInt("company_id"));
        jobOffer.setTitle(rs.getString("title"));
        jobOffer.setDescription(rs.getString("description"));
        jobOffer.setRequirements(rs.getString("requirements"));
        jobOffer.setMinSalary(rs.getBigDecimal("min_salary"));
        jobOffer.setMaxSalary(rs.getBigDecimal("max_salary"));
        jobOffer.setCurrency(rs.getString("currency"));
        jobOffer.setLocation(rs.getString("location"));
        
        String workTypeStr = rs.getString("work_type");
        if (workTypeStr != null) {
            try {
                jobOffer.setWorkType(WorkType.valueOf(workTypeStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown work type: {}", workTypeStr);
            }
        }

        Timestamp postedDateTs = rs.getTimestamp("posted_date");
        if (postedDateTs != null) {
            jobOffer.setPostedDate(postedDateTs.toLocalDateTime());
        }

        Date deadlineDate = rs.getDate("deadline");
        if (deadlineDate != null) {
            jobOffer.setDeadline(deadlineDate.toLocalDate());
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                jobOffer.setStatus(JobStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown job status: {}", statusStr);
                jobOffer.setStatus(JobStatus.OPEN);
            }
        }
        
        return jobOffer;
    }
}

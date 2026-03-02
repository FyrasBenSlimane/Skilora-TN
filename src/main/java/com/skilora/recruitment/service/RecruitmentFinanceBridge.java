package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.EmploymentContract;
import com.skilora.finance.service.ContractService;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.HireOffer;
import com.skilora.recruitment.enums.HireOfferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Bridges the recruitment and finance modules.
 * When a HireOffer is accepted, auto-generates an EmploymentContract
 * with PENDING_SIGNATURE status for the employee to sign.
 */
public class RecruitmentFinanceBridge {

    private static final Logger logger = LoggerFactory.getLogger(RecruitmentFinanceBridge.class);
    private static volatile RecruitmentFinanceBridge instance;

    private final HireOfferService hireOfferService = HireOfferService.getInstance();
    private final ApplicationService applicationService = ApplicationService.getInstance();
    private final ContractService contractService = ContractService.getInstance();

    private RecruitmentFinanceBridge() {}

    public static RecruitmentFinanceBridge getInstance() {
        if (instance == null) {
            synchronized (RecruitmentFinanceBridge.class) {
                if (instance == null) {
                    instance = new RecruitmentFinanceBridge();
                }
            }
        }
        return instance;
    }

    /**
     * Accepts a hire offer and auto-generates an employment contract.
     * Flow: HireOffer ACCEPTED → Application ACCEPTED → EmploymentContract PENDING_SIGNATURE
     *
     * @return the generated contract ID, or -1 on failure
     */
    public int acceptOfferAndGenerateContract(int hireOfferId) {
        try {
            HireOffer offer = hireOfferService.findById(hireOfferId);
            if (offer == null) {
                logger.error("Hire offer {} not found", hireOfferId);
                return -1;
            }

            if (!HireOfferStatus.PENDING.name().equals(offer.getStatus())) {
                logger.warn("Hire offer {} is not in PENDING status: {}", hireOfferId, offer.getStatus());
                return -1;
            }

            boolean accepted = hireOfferService.accept(hireOfferId);
            if (!accepted) {
                logger.error("Failed to accept hire offer {}", hireOfferId);
                return -1;
            }

            applicationService.updateStatus(offer.getApplicationId(), Application.Status.ACCEPTED);

            Application app = applicationService.getById(offer.getApplicationId()).orElse(null);
            if (app == null) {
                logger.error("Application {} not found for hire offer {}", offer.getApplicationId(), hireOfferId);
                return -1;
            }

            int employerUserId = resolveEmployerUserId(app.getJobOfferId());

            EmploymentContract contract = new EmploymentContract();
            contract.setUserId(resolveUserIdFromProfile(app.getCandidateProfileId()));
            contract.setEmployerId(employerUserId > 0 ? employerUserId : null);
            contract.setJobOfferId(app.getJobOfferId());
            contract.setSalaryBase(BigDecimal.valueOf(offer.getSalaryOffered()));
            contract.setCurrency(offer.getCurrency());
            contract.setStartDate(offer.getStartDate() != null
                ? offer.getStartDate()
                : java.time.LocalDate.now().plusMonths(1));
            contract.setContractType(offer.getContractType());
            contract.setStatus("PENDING_SIGNATURE");

            if ("CDD".equals(offer.getContractType()) || "STAGE".equals(offer.getContractType())) {
                contract.setEndDate(contract.getStartDate().plusMonths(
                    "STAGE".equals(offer.getContractType()) ? 6 : 12));
            }

            int contractId = contractService.create(contract);
            logger.info("Auto-generated contract {} from hire offer {} for user {}",
                contractId, hireOfferId, contract.getUserId());
            return contractId;

        } catch (Exception e) {
            logger.error("Failed to accept offer and generate contract for offer {}: {}",
                hireOfferId, e.getMessage(), e);
            return -1;
        }
    }

    private int resolveEmployerUserId(int jobOfferId) {
        String sql = """
            SELECT c.owner_id FROM job_offers j
            JOIN companies c ON j.company_id = c.id
            WHERE j.id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobOfferId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("owner_id");
        } catch (SQLException e) {
            logger.error("Error resolving employer for job {}: {}", jobOfferId, e.getMessage());
        }
        return -1;
    }

    private int resolveUserIdFromProfile(int profileId) {
        String sql = "SELECT user_id FROM profiles WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, profileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        } catch (SQLException e) {
            logger.error("Error resolving user from profile {}: {}", profileId, e.getMessage());
        }
        return profileId;
    }
}

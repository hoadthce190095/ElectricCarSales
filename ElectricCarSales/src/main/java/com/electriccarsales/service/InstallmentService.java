package com.electriccarsales.service;

import com.electriccarsales.dao.FinancialCompanyDAO;
import com.electriccarsales.dao.InstallmentContractDAO;
import com.electriccarsales.dao.InstallmentPaymentDAO;
import com.electriccarsales.dao.InstallmentPlanDAO;
import com.electriccarsales.dao.InstallmentScheduleDAO;
import com.electriccarsales.model.FinancialCompany;
import com.electriccarsales.model.InstallmentContract;
import com.electriccarsales.model.InstallmentPayment;
import com.electriccarsales.model.InstallmentPlan;
import com.electriccarsales.model.InstallmentSchedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Installment plan lookup and server-side finance calculations. */
public class InstallmentService {
    private static final BigDecimal DOWN_PAYMENT_RATE = new BigDecimal("0.30");
    private static final List<Integer> ALLOWED_TERMS = List.of(12, 24, 36, 48, 60);

    private final InstallmentPlanDAO planDAO;
    private final FinancialCompanyDAO companyDAO;
    private final InstallmentContractDAO contractDAO;
    private final InstallmentScheduleDAO scheduleDAO;
    private final InstallmentPaymentDAO installmentPaymentDAO;

    public InstallmentService() {
        this(new InstallmentPlanDAO(), new FinancialCompanyDAO(), new InstallmentContractDAO(),
                new InstallmentScheduleDAO(), new InstallmentPaymentDAO());
    }

    public InstallmentService(InstallmentPlanDAO planDAO, FinancialCompanyDAO companyDAO,
                              InstallmentContractDAO contractDAO) {
        this(planDAO, companyDAO, contractDAO, new InstallmentScheduleDAO(), new InstallmentPaymentDAO());
    }

    public InstallmentService(InstallmentPlanDAO planDAO, FinancialCompanyDAO companyDAO,
                              InstallmentContractDAO contractDAO, InstallmentScheduleDAO scheduleDAO,
                              InstallmentPaymentDAO installmentPaymentDAO) {
        this.planDAO = Objects.requireNonNull(planDAO, "planDAO");
        this.companyDAO = Objects.requireNonNull(companyDAO, "companyDAO");
        this.contractDAO = Objects.requireNonNull(contractDAO, "contractDAO");
        this.scheduleDAO = Objects.requireNonNull(scheduleDAO, "scheduleDAO");
        this.installmentPaymentDAO = Objects.requireNonNull(installmentPaymentDAO, "installmentPaymentDAO");
    }

    public List<InstallmentPlan> getPlans() { return planDAO.findAll(); }
    public InstallmentPlan getPlan(int planId) { return planDAO.findById(requirePositiveId(planId, "planId")); }
    public List<FinancialCompany> getFinancialCompanies() { return companyDAO.findAll(); }
    public FinancialCompany getFinancialCompany(int companyId) {
        return companyDAO.findById(requirePositiveId(companyId, "companyId"));
    }
    public InstallmentContract getContract(int contractId) {
        return contractDAO.findById(requirePositiveId(contractId, "contractId"));
    }
    public InstallmentContract getContractDetails(int contractId) {
        return contractDAO.findDetailsById(requirePositiveId(contractId, "contractId"));
    }
    public InstallmentContract getContractForOrder(int orderId) {
        return contractDAO.findByOrderId(requirePositiveId(orderId, "orderId"));
    }
    public boolean orderBelongsToCustomer(int orderId, int customerId) {
        return contractDAO.orderBelongsToCustomer(requirePositiveId(orderId, "orderId"),
                requirePositiveId(customerId, "customerId"));
    }
    public boolean contractBelongsToCustomer(int contractId, int customerId) {
        return contractDAO.contractBelongsToCustomer(requirePositiveId(contractId, "contractId"),
                requirePositiveId(customerId, "customerId"));
    }
    public List<InstallmentSchedule> getSchedule(int contractId) {
        return scheduleDAO.findByContractId(requirePositiveId(contractId, "contractId"));
    }
    public List<InstallmentPayment> getPaymentHistory(int contractId) {
        return installmentPaymentDAO.findByContractId(requirePositiveId(contractId, "contractId"));
    }
    public List<InstallmentSchedule> getUpcomingSchedules(java.sql.Date fromDate) {
        return scheduleDAO.findUpcoming(Objects.requireNonNull(fromDate, "fromDate"));
    }
    public List<InstallmentSchedule> getOverdueSchedules(java.sql.Date asOfDate) {
        return scheduleDAO.findOverdue(Objects.requireNonNull(asOfDate, "asOfDate"));
    }
    public BigDecimal getOutstandingDebt(int contractId) {
        requirePositiveId(contractId, "contractId");
        InstallmentContract contract = contractDAO.findById(contractId);
        if (contract == null) throw new IllegalArgumentException("Installment contract does not exist");
        BigDecimal paid = installmentPaymentDAO.sumSuccessfulByContractId(contractId);
        BigDecimal debt = contract.getTotalInstallmentAmount().subtract(paid).setScale(2, RoundingMode.HALF_UP);
        if (debt.signum() < 0) throw new IllegalStateException("Successful installment payments exceed the contract amount");
        return debt;
    }

    public InstallmentQuote calculateQuote(BigDecimal vehiclePrice, int termMonths,
                                           BigDecimal storedMonthlyRate, RateRepresentation rateRepresentation) {
        requirePositiveMoney(vehiclePrice, "vehiclePrice");
        if (!ALLOWED_TERMS.contains(termMonths)) throw new IllegalArgumentException("Unsupported installment term");
        if (storedMonthlyRate == null || storedMonthlyRate.signum() < 0) {
            throw new IllegalArgumentException("storedMonthlyRate must be zero or greater");
        }
        Objects.requireNonNull(rateRepresentation, "rateRepresentation");

        BigDecimal downPayment = vehiclePrice.multiply(DOWN_PAYMENT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal loanAmount = vehiclePrice.subtract(downPayment).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = rateRepresentation == RateRepresentation.PERCENT
                ? storedMonthlyRate.movePointLeft(2) : storedMonthlyRate;
        BigDecimal totalInterest = loanAmount.multiply(monthlyRate)
                .multiply(BigDecimal.valueOf(termMonths)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInstallmentAmount = loanAmount.add(totalInterest).setScale(2, RoundingMode.HALF_UP);
        BigDecimal regularPayment = totalInstallmentAmount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.DOWN);

        List<BigDecimal> scheduleAmounts = new ArrayList<>(termMonths);
        BigDecimal allocated = BigDecimal.ZERO.setScale(2);
        for (int period = 1; period < termMonths; period++) {
            scheduleAmounts.add(regularPayment);
            allocated = allocated.add(regularPayment);
        }
        scheduleAmounts.add(totalInstallmentAmount.subtract(allocated).setScale(2, RoundingMode.UNNECESSARY));
        return new InstallmentQuote(vehiclePrice, downPayment, loanAmount, termMonths,
                storedMonthlyRate, rateRepresentation, totalInterest, totalInstallmentAmount,
                regularPayment, List.copyOf(scheduleAmounts));
    }

    private static int requirePositiveId(int id, String name) {
        if (id <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
        return id;
    }

    private static void requirePositiveMoney(BigDecimal amount, String name) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
    }

    public enum RateRepresentation {
        /** Database value 0.12 means 12% per month. */
        PERCENT,
        /** Database value 0.12 means a 0.12 fraction per month. */
        DECIMAL_FRACTION
    }

    public record InstallmentQuote(BigDecimal vehiclePrice, BigDecimal downPayment, BigDecimal loanAmount,
                                   int termMonths, BigDecimal storedMonthlyRate,
                                   RateRepresentation rateRepresentation, BigDecimal totalInterest,
                                   BigDecimal totalInstallmentAmount, BigDecimal regularPayment,
                                   List<BigDecimal> scheduleAmounts) { }
}

package com.electriccarsales.service;

import com.electriccarsales.dao.InstallmentContractDAO;
import com.electriccarsales.dao.InstallmentSettlementDAO;
import com.electriccarsales.model.InstallmentContract;
import com.electriccarsales.model.InstallmentSettlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Settlement quote arithmetic and contract lookup. Persists no settlement before payment verification. */
public class SettlementService {
    private final InstallmentContractDAO contractDAO;
    private final InstallmentSettlementDAO settlementDAO;
    private final SettlementBalanceProvider balanceProvider;

    public SettlementService(InstallmentContractDAO contractDAO, InstallmentSettlementDAO settlementDAO,
                             SettlementBalanceProvider balanceProvider) {
        this.contractDAO = Objects.requireNonNull(contractDAO, "contractDAO");
        this.settlementDAO = Objects.requireNonNull(settlementDAO, "settlementDAO");
        this.balanceProvider = Objects.requireNonNull(balanceProvider, "balanceProvider");
    }

    public InstallmentContract getContract(int contractId) {
        if (contractId <= 0) throw new IllegalArgumentException("contractId must be greater than zero");
        return contractDAO.findById(contractId);
    }

    public InstallmentSettlement getSettlement(int contractId) {
        if (contractId <= 0) throw new IllegalArgumentException("contractId must be greater than zero");
        return settlementDAO.findByContractId(contractId);
    }

    /** Loads settlement amounts through a server-side ledger provider; client-supplied balances are not accepted. */
    public SettlementQuote calculateQuote(int contractId) {
        InstallmentContract contract = getContract(contractId);
        if (contract == null) throw new IllegalArgumentException("Installment contract does not exist");
        if (settlementDAO.existsByContractId(contractId)) {
            throw new IllegalStateException("Installment contract is already settled");
        }
        SettlementBalance balance = Objects.requireNonNull(balanceProvider.load(contractId),
                "Settlement balance provider returned no balance");
        if (balance.alreadySettled()) throw new IllegalStateException("Installment contract is already settled");
        BigDecimal principalRemaining = balance.principalRemaining();
        BigDecimal accruedInterest = balance.accruedInterest();
        BigDecimal settlementFee = balance.settlementFee();
        requireNonNegative(principalRemaining, "principalRemaining");
        requireNonNegative(accruedInterest, "accruedInterest");
        requireNonNegative(settlementFee, "settlementFee");
        if (principalRemaining.signum() == 0) {
            throw new IllegalArgumentException("There is no remaining principal to settle");
        }
        BigDecimal amount = principalRemaining.add(accruedInterest).add(settlementFee)
                .setScale(2, RoundingMode.UNNECESSARY);
        return new SettlementQuote(principalRemaining, accruedInterest, settlementFee, amount);
    }

    private static void requireNonNegative(BigDecimal amount, String name) {
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException(name + " must be zero or greater");
    }

    @FunctionalInterface
    public interface SettlementBalanceProvider {
        /** Implemented with trusted server-side schedule, successful-payment, and prior-settlement data. */
        SettlementBalance load(int installmentContractId);
    }

    public record SettlementBalance(BigDecimal principalRemaining, BigDecimal accruedInterest,
                                    BigDecimal settlementFee, boolean alreadySettled) { }

    public record SettlementQuote(BigDecimal principalRemaining, BigDecimal accruedInterest,
                                  BigDecimal settlementFee, BigDecimal settlementAmount) { }
}

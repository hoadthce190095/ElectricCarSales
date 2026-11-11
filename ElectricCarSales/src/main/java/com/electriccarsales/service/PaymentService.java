package com.electriccarsales.service;

import com.electriccarsales.dao.PaymentDAO;
import com.electriccarsales.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Currency;

/** Payment business calculations and gateway amount preparation. */
public class PaymentService {
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");
    private static final BigDecimal TEST_DIVISOR = new BigDecimal("1000");

    private final PaymentDAO paymentDAO;
    private final boolean testMode;
    private final OrderPaymentEligibility orderPaymentEligibility;

    public PaymentService() {
        this(new PaymentDAO(), Boolean.parseBoolean(System.getenv().getOrDefault("PAYMENT_TEST_MODE", "false")));
    }

    public PaymentService(PaymentDAO paymentDAO) {
        this(paymentDAO, Boolean.parseBoolean(System.getenv().getOrDefault("PAYMENT_TEST_MODE", "false")));
    }

    /** Explicit mode injection keeps amount conversion deterministic in callers and tests. */
    public PaymentService(PaymentDAO paymentDAO, boolean testMode) {
        this(paymentDAO, testMode, null);
    }

    public PaymentService(PaymentDAO paymentDAO, boolean testMode,
                          OrderPaymentEligibility orderPaymentEligibility) {
        this.paymentDAO = Objects.requireNonNull(paymentDAO, "paymentDAO");
        this.testMode = testMode;
        this.orderPaymentEligibility = orderPaymentEligibility;
    }

    public List<Payment> getOrderHistory(int orderId) {
        requirePositiveId(orderId, "orderId");
        return paymentDAO.findByOrderId(orderId);
    }

    public Payment getPayment(int paymentId) {
        requirePositiveId(paymentId, "paymentId");
        return paymentDAO.findById(paymentId);
    }

    public List<Payment> getCustomerPaymentHistory(int customerId) {
        requirePositiveId(customerId, "customerId");
        return paymentDAO.findDetailsByCustomerId(customerId);
    }

    public boolean orderBelongsToCustomer(int orderId, int customerId) {
        requirePositiveId(orderId, "orderId");
        requirePositiveId(customerId, "customerId");
        return paymentDAO.orderBelongsToCustomer(orderId, customerId);
    }

    public PaymentDAO.OrderBalance getOrderBalance(int orderId) {
        requirePositiveId(orderId, "orderId");
        PaymentDAO.OrderBalance balance = paymentDAO.getOrderBalance(orderId);
        if (balance == null) throw new IllegalArgumentException("Order does not exist");
        return balance;
    }

    public BigDecimal getOutstandingDebt(int orderId) {
        PaymentDAO.OrderBalance balance = getOrderBalance(orderId);
        BigDecimal debt = balance.getTotalAmount().subtract(balance.getSuccessfulPayments())
                .setScale(2, RoundingMode.HALF_UP);
        if (debt.signum() < 0) {
            throw new IllegalStateException("Successful payments exceed the order total.");
        }
        return debt;
    }

    public BigDecimal calculateDeposit(BigDecimal vehiclePrice) {
        requirePositiveMoney(vehiclePrice, "vehiclePrice");
        return vehiclePrice.multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRemaining(BigDecimal vehiclePrice) {
        requirePositiveMoney(vehiclePrice, "vehiclePrice");
        return vehiclePrice.subtract(calculateDeposit(vehiclePrice)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Creates a pending database payment using an amount derived from the order balance, then
     * returns a separate gateway amount. The request amount is never accepted from the client.
     * The application must inject an eligibility checker backed by its order and security modules.
     */
    public PreparedPayment createRequest(int orderId, String methodName, String typeName,
                                         String currency, BigDecimal providerMinimum, int providerScale) {
        requirePositiveId(orderId, "orderId");
        if (testMode) throw new IllegalStateException("Scaled test gateway amounts cannot create real order payments.");
        if (methodName == null || methodName.isBlank()) throw new IllegalArgumentException("methodName is required");
        if (typeName == null || typeName.isBlank()) throw new IllegalArgumentException("typeName is required");
        if (orderPaymentEligibility == null) {
            throw new IllegalStateException("Order payment eligibility integration is not configured");
        }
        orderPaymentEligibility.assertCustomerMayPayOpenOrder(orderId);

        PaymentDAO.OrderBalance balance = getOrderBalance(orderId);
        BigDecimal originalAmount;
        String normalizedType = typeName.trim().toUpperCase(Locale.ROOT);
        switch (normalizedType) {
            case "DEPOSIT" -> {
                BigDecimal dueDeposit = calculateDeposit(balance.getTotalAmount());
                originalAmount = dueDeposit.subtract(balance.getSuccessfulPayments());
                if (originalAmount.signum() <= 0) throw new IllegalStateException("Deposit is already paid");
            }
            case "REMAINING" -> {
                originalAmount = balance.getTotalAmount().subtract(balance.getSuccessfulPayments())
                        .setScale(2, RoundingMode.HALF_UP);
                if (originalAmount.signum() <= 0) throw new IllegalStateException("Order is already fully paid");
            }
            default -> throw new IllegalArgumentException("Unsupported direct payment type: " + normalizedType);
        }

        GatewayPaymentAmount gatewayAmount = prepareGatewayAmount(originalAmount, currency,
                providerMinimum, providerScale);
        int methodId = paymentDAO.getPaymentMethodId(methodName.trim());
        String reference = "PAY-" + UUID.randomUUID();
        Payment payment = paymentDAO.createPending(orderId, methodId, normalizedType,
                originalAmount, false, reference);
        return new PreparedPayment(payment, gatewayAmount);
    }

    /**
     * Builds the gateway-facing amount pair. The original business amount is never modified;
     * provider minimum and amount scale must come from the selected provider's configuration.
     */
    public GatewayPaymentAmount prepareGatewayAmount(BigDecimal originalAmount, String currency,
                                                     BigDecimal providerMinimum, int providerScale) {
        requirePositiveMoney(originalAmount, "originalAmount");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalizedCurrency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("currency must be a supported ISO 4217 code", e);
        }
        if (providerMinimum == null || providerMinimum.signum() < 0) {
            throw new IllegalArgumentException("providerMinimum must be zero or greater");
        }
        if (providerScale < 0) throw new IllegalArgumentException("providerScale must be zero or greater");

        BigDecimal gatewayAmount = testMode
                ? originalAmount.divide(TEST_DIVISOR)
                : originalAmount;
        try {
            gatewayAmount = gatewayAmount.setScale(providerScale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount cannot be represented exactly at the provider's configured scale", e);
        }
        if (gatewayAmount.signum() <= 0) throw new IllegalArgumentException("Gateway amount must remain greater than zero");
        if (gatewayAmount.compareTo(providerMinimum) < 0) {
            throw new IllegalArgumentException("Gateway amount is below the configured provider minimum");
        }
        return new GatewayPaymentAmount(originalAmount, gatewayAmount, normalizedCurrency, testMode);
    }

    public boolean isTestMode() {
        return testMode;
    }

    private static void requirePositiveMoney(BigDecimal amount, String name) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
    }

    private static void requirePositiveId(int id, String name) {
        if (id <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
    }

    public record GatewayPaymentAmount(BigDecimal originalAmount, BigDecimal gatewayAmount,
                                       String currency, boolean testMode) {
        public GatewayPaymentAmount {
            Objects.requireNonNull(originalAmount, "originalAmount");
            Objects.requireNonNull(gatewayAmount, "gatewayAmount");
            Objects.requireNonNull(currency, "currency");
        }
    }

    public record PreparedPayment(Payment payment, GatewayPaymentAmount gatewayAmount) {
        public PreparedPayment {
            Objects.requireNonNull(payment, "payment");
            Objects.requireNonNull(gatewayAmount, "gatewayAmount");
        }
    }

    @FunctionalInterface
    public interface OrderPaymentEligibility {
        /** Validates the authenticated customer owns the order and the order is open for payment. */
        void assertCustomerMayPayOpenOrder(int orderId);
    }
}

package com.electriccarsales.service;

import com.electriccarsales.dao.PaymentDAO;
import com.electriccarsales.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Payment history, order balances, and basic amount calculations. */
public class PaymentService {
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");

    private final PaymentDAO paymentDAO;

    public PaymentService() {
        this(new PaymentDAO());
    }

    public PaymentService(PaymentDAO paymentDAO) {
        this.paymentDAO = Objects.requireNonNull(paymentDAO, "paymentDAO");
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
        return vehiclePrice.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRemaining(BigDecimal vehiclePrice) {
        requirePositiveMoney(vehiclePrice, "vehiclePrice");
        return vehiclePrice.subtract(calculateDeposit(vehiclePrice)).setScale(2, RoundingMode.HALF_UP);
    }

    private static void requirePositiveMoney(BigDecimal amount, String name) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
    }

    private static void requirePositiveId(int id, String name) {
        if (id <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
    }

}

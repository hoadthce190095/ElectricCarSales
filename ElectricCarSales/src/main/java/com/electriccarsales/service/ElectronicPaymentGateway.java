package com.electriccarsales.service;

import java.util.List;
import java.util.Map;

/** Provider boundary for hosted payment checkout, card tokenization, and callback verification. */
public interface ElectronicPaymentGateway {
    CheckoutSession createCheckoutSession(CheckoutRequest request);
    CardSetupSession createCardSetupSession(String customerReference, String returnUrl);
    List<StoredPaymentMethod> getStoredPaymentMethods(String customerReference);
    void removeStoredPaymentMethod(String customerReference, String paymentMethodReference);

    /** The adapter validates the signature and queries provider details when its protocol requires it. */
    VerifiedPayment verifyNotification(GatewayNotification notification);

    enum PaymentChannel { CARD, BANK_DEBIT, WALLET, QR }
    enum PaymentPurpose { DEPOSIT, REMAINING, DIRECT_ORDER, INSTALLMENT, SETTLEMENT }

    record CheckoutRequest(String businessReference, String customerReference, PaymentPurpose purpose,
                           PaymentChannel channel, String storedPaymentMethodReference,
                           PaymentService.GatewayPaymentAmount amount, String returnUrl) { }
    record CheckoutSession(String sessionReference, String redirectUrl, String status) { }
    record CardSetupSession(String sessionReference, String redirectUrl) { }
    record StoredPaymentMethod(String paymentMethodReference, PaymentChannel channel, String brand,
                               String lastFourDigits, Integer expiryMonth, Integer expiryYear) { }
    record GatewayNotification(String rawBody, Map<String, String> headers) { }
    record VerifiedPayment(boolean verified, boolean successful, String businessReference,
                           String providerTransactionReference, String currency, String gatewayAmount) { }
}

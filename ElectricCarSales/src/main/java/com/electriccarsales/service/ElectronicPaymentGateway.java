package com.electriccarsales.service;

import java.util.List;
import java.util.Map;

/** Provider boundary for hosted payment checkout, card tokenization, and callback verification. */
public interface ElectronicPaymentGateway {
    CheckoutSession createCheckoutSession(CheckoutRequest request);

    CardSetupSession createCardSetupSession(String customerReference, String returnUrl);

    List<StoredPaymentMethod> getStoredPaymentMethods(String customerReference);

    void removeStoredPaymentMethod(String customerReference, String paymentMethodReference);

    /** The provider adapter must validate the signature and query details where its protocol requires it. */
    VerifiedPayment verifyNotification(GatewayNotification notification);

    enum PaymentChannel {
        CARD,
        BANK_DEBIT,
        WALLET,
        QR
    }

    enum PaymentPurpose {
        DEPOSIT,
        REMAINING,
        DIRECT_ORDER,
        INSTALLMENT,
        SETTLEMENT
    }

    record CheckoutRequest(String businessReference,
                           String customerReference,
                           PaymentPurpose purpose,
                           PaymentChannel channel,
                           String storedPaymentMethodReference,
                           PaymentService.GatewayPaymentAmount amount,
                           String returnUrl) {
    }

    /** Redirect to this hosted URL; the website does not collect card numbers or security codes. */
    record CheckoutSession(String sessionReference, String redirectUrl, String status) {
    }

    /** Hosted setup page used to add a card to the provider's token vault. */
    record CardSetupSession(String sessionReference, String redirectUrl) {
    }

    /** Contains display information and an opaque provider reference, never PAN or CVV. */
    record StoredPaymentMethod(String paymentMethodReference,
                               PaymentChannel channel,
                               String brand,
                               String lastFourDigits,
                               Integer expiryMonth,
                               Integer expiryYear) {
    }

    record GatewayNotification(String rawBody, Map<String, String> headers) {
    }

    /** Returned only after the provider adapter has verified the notification. */
    record VerifiedPayment(boolean verified,
                           boolean successful,
                           String businessReference,
                           String providerTransactionReference,
                           String currency,
                           String gatewayAmount) {
    }
}

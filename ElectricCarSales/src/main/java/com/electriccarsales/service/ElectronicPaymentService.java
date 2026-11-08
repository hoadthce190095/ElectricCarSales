package com.electriccarsales.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Coordinates hosted checkout, provider tokenization, and verified payment notifications. */
public class ElectronicPaymentService {
    private final ElectronicPaymentGateway gateway;

    public ElectronicPaymentService(ElectronicPaymentGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    /**
     * Starts a hosted checkout. The caller must obtain the business amount from trusted server-side
     * order, installment, or settlement data, never directly from a browser request.
     */
    public ElectronicPaymentGateway.CheckoutSession createCheckout(
            String businessReference,
            String customerReference,
            ElectronicPaymentGateway.PaymentPurpose purpose,
            ElectronicPaymentGateway.PaymentChannel channel,
            String storedPaymentMethodReference,
            PaymentService.GatewayPaymentAmount amount,
            String returnUrl) {
        requireText(businessReference, "businessReference");
        requireText(customerReference, "customerReference");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(amount, "amount");
        requireHttpsUrl(returnUrl, "returnUrl");

        if (amount.originalAmount().signum() <= 0 || amount.gatewayAmount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amounts must be greater than zero");
        }
        if (storedPaymentMethodReference != null && storedPaymentMethodReference.isBlank()) {
            storedPaymentMethodReference = null;
        }

        ElectronicPaymentGateway.CheckoutRequest request = new ElectronicPaymentGateway.CheckoutRequest(
                businessReference.trim(), customerReference.trim(), purpose, channel,
                storedPaymentMethodReference, amount, returnUrl.trim());
        ElectronicPaymentGateway.CheckoutSession session = gateway.createCheckoutSession(request);
        if (session == null) {
            throw new IllegalStateException("Payment provider did not return a checkout session");
        }
        requireText(session.sessionReference(), "provider session reference");
        requireHttpsUrl(session.redirectUrl(), "provider checkout URL");
        return session;
    }

    /** Starts the provider's hosted card setup flow; card number and CVV never pass through this app. */
    public ElectronicPaymentGateway.CardSetupSession startCardLink(
            String customerReference, String returnUrl) {
        requireText(customerReference, "customerReference");
        requireHttpsUrl(returnUrl, "returnUrl");

        ElectronicPaymentGateway.CardSetupSession session =
                gateway.createCardSetupSession(customerReference.trim(), returnUrl.trim());
        if (session == null) {
            throw new IllegalStateException("Payment provider did not return a card setup session");
        }
        requireText(session.sessionReference(), "provider session reference");
        requireHttpsUrl(session.redirectUrl(), "provider card setup URL");
        return session;
    }

    public List<ElectronicPaymentGateway.StoredPaymentMethod> getLinkedPaymentMethods(
            String customerReference) {
        requireText(customerReference, "customerReference");
        List<ElectronicPaymentGateway.StoredPaymentMethod> methods =
                gateway.getStoredPaymentMethods(customerReference.trim());
        if (methods == null) {
            throw new IllegalStateException("Payment provider did not return linked payment methods");
        }
        return List.copyOf(methods);
    }

    public void removeLinkedPaymentMethod(String customerReference, String paymentMethodReference) {
        requireText(customerReference, "customerReference");
        requireText(paymentMethodReference, "paymentMethodReference");
        gateway.removeStoredPaymentMethod(customerReference.trim(), paymentMethodReference.trim());
    }

    /**
     * Validates a provider notification against the expected pending business payment. The caller
     * must load that pending record from the database before calling this method and may mark it
     * successful only after this method returns a verified, successful result.
     */
    public ElectronicPaymentGateway.VerifiedPayment verifyNotification(
            ElectronicPaymentGateway.GatewayNotification notification,
            String expectedBusinessReference,
            PaymentService.GatewayPaymentAmount expectedAmount) {
        Objects.requireNonNull(notification, "notification");
        requireText(expectedBusinessReference, "expectedBusinessReference");
        Objects.requireNonNull(expectedAmount, "expectedAmount");

        ElectronicPaymentGateway.VerifiedPayment payment = gateway.verifyNotification(notification);
        if (payment == null || !payment.verified()) {
            throw new SecurityException("Payment provider notification could not be verified");
        }
        if (!expectedBusinessReference.trim().equals(payment.businessReference())) {
            throw new SecurityException("Provider notification does not match the pending business payment");
        }
        if (payment.currency() == null || !expectedAmount.currency().equalsIgnoreCase(payment.currency())) {
            throw new SecurityException("Provider notification currency does not match the pending payment");
        }
        if (!sameAmount(payment.gatewayAmount(), expectedAmount.gatewayAmount())) {
            throw new SecurityException("Provider notification amount does not match the pending payment");
        }
        if (payment.successful()) {
            requireText(payment.providerTransactionReference(), "provider transaction reference");
        }
        return payment;
    }

    private static boolean sameAmount(String actualAmount, BigDecimal expectedAmount) {
        if (actualAmount == null || actualAmount.isBlank()) {
            return false;
        }
        try {
            return new BigDecimal(actualAmount).compareTo(expectedAmount) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void requireHttpsUrl(String value, String name) {
        requireText(value, name);
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !"https".equals(scheme.toLowerCase(Locale.ROOT))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException(name + " must be an absolute HTTPS URL");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " must be a valid absolute HTTPS URL", e);
        }
    }
}

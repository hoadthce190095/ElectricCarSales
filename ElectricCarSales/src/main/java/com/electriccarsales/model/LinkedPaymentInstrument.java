package com.electriccarsales.model;

/** A customer's tokenized card, wallet, or bank instrument held by a payment provider. */
public class LinkedPaymentInstrument {
    private int linkedInstrumentID;
    private int customerID;
    private String gatewayCode;
    private String instrumentType;
    private String network;
    private String providerToken;
    private String maskedLabel;
    private boolean defaultInstrument;
    private boolean active;

    public LinkedPaymentInstrument() {
    }

    public LinkedPaymentInstrument(int linkedInstrumentID, int customerID, String gatewayCode,
                                   String instrumentType, String network, String providerToken,
                                   String maskedLabel, boolean defaultInstrument, boolean active) {
        this.linkedInstrumentID = linkedInstrumentID;
        this.customerID = customerID;
        this.gatewayCode = gatewayCode;
        this.instrumentType = instrumentType;
        this.network = network;
        this.providerToken = providerToken;
        this.maskedLabel = maskedLabel;
        this.defaultInstrument = defaultInstrument;
        this.active = active;
    }

    public int getLinkedInstrumentID() {
        return linkedInstrumentID;
    }

    public void setLinkedInstrumentID(int linkedInstrumentID) {
        this.linkedInstrumentID = linkedInstrumentID;
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    /** Token supplied by the payment provider. Never store a card number or security code here. */
    public String getProviderToken() {
        return providerToken;
    }

    public void setProviderToken(String providerToken) {
        this.providerToken = providerToken;
    }

    public String getMaskedLabel() {
        return maskedLabel;
    }

    public void setMaskedLabel(String maskedLabel) {
        this.maskedLabel = maskedLabel;
    }

    public boolean isDefaultInstrument() {
        return defaultInstrument;
    }

    public void setDefaultInstrument(boolean defaultInstrument) {
        this.defaultInstrument = defaultInstrument;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

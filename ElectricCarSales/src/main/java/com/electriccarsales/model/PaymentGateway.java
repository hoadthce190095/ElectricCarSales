package com.electriccarsales.model;

import java.util.ArrayList;
import java.util.List;

/** Describes a configured electronic payment provider and its supported channels. */
public class PaymentGateway {
    private String gatewayCode;
    private String gatewayName;
    private List<String> supportedChannels;
    private boolean active;

    public PaymentGateway() {
        this.supportedChannels = new ArrayList<>();
    }

    public PaymentGateway(String gatewayCode, String gatewayName,
                          List<String> supportedChannels, boolean active) {
        this.gatewayCode = gatewayCode;
        this.gatewayName = gatewayName;
        setSupportedChannels(supportedChannels);
        this.active = active;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public List<String> getSupportedChannels() {
        return new ArrayList<>(supportedChannels);
    }

    public void setSupportedChannels(List<String> supportedChannels) {
        if (supportedChannels == null) {
            this.supportedChannels = new ArrayList<>();
        } else {
            this.supportedChannels = new ArrayList<>(supportedChannels);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

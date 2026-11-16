package com.electriccarsales.model;

import java.util.Set;

public enum SalesPreviewKind {
    DEPOSIT("deposit-preview.docx", Set.of("order.id", "payment.id", "payment.amount", "customer.name")),
    CONTRACT("contract-preview.docx", Set.of("order.id", "contract.number", "order.totalAmount", "customer.name")),
    INVOICE("invoice-preview.docx", Set.of("order.id", "invoice.number", "invoice.totalAmount", "customer.name"));

    private final String filename;
    private final Set<String> requiredFields;

    SalesPreviewKind(String filename, Set<String> requiredFields) {
        this.filename = filename;
        this.requiredFields = requiredFields;
    }

    public String filename() { return filename; }
    public Set<String> requiredFields() { return requiredFields; }
}

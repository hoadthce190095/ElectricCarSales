package com.electriccarsales.model;

import java.time.LocalDate;

public class ReportPeriod {
    private LocalDate from;
    private LocalDate to;

    public ReportPeriod() {
    }

    public ReportPeriod(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }
}

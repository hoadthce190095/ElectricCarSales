package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;

public class StatusTotal {
    private String status;
    private long count;
    private BigDecimal amount;

    public StatusTotal() {
    }

    public StatusTotal(String status, long count, BigDecimal amount) {
        this.status = status;
        this.count = count;
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

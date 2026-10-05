package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PromotionRequest {
    private String promotionName;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private List<Integer> modelIds;

    public PromotionRequest() {
    }

    public PromotionRequest(String promotionName, String description, String discountType, BigDecimal discountValue,
        LocalDateTime startDate, LocalDateTime endDate, String status, List<Integer> modelIds) {
        this.promotionName = promotionName;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.modelIds = modelIds;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Integer> getModelIds() {
        return modelIds;
    }

    public void setModelIds(List<Integer> modelIds) {
        this.modelIds = modelIds;
    }

    @Override
    public String toString() {
        return "PromotionRequest[" + "promotionName=" + promotionName + ", description=" + description
            + ", discountType=" + discountType + ", discountValue=" + discountValue + ", startDate=" + startDate
            + ", endDate=" + endDate + ", status=" + status + ", modelIds=" + modelIds + "]";
    }
}

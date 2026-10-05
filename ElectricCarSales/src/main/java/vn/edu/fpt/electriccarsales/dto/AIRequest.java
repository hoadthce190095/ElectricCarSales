package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monetary values are VND, range is kilometres; intent may be omitted for text detection.
 */
public class AIRequest {
    private String message;
    private BigDecimal budget;
    private BigDecimal minRange;
    private Integer minSeats;
    private List<Integer> modelIds;
    private String intent;

    public AIRequest() {
    }

    public AIRequest(String message, BigDecimal budget, BigDecimal minRange, Integer minSeats, List<Integer> modelIds,
        String intent) {
        setMessage(message);
        setBudget(budget);
        setMinRange(minRange);
        setMinSeats(minSeats);
        setModelIds(modelIds);
        setIntent(intent);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getMinRange() {
        return minRange;
    }

    public void setMinRange(BigDecimal minRange) {
        this.minRange = minRange;
    }

    public Integer getMinSeats() {
        return minSeats;
    }

    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    public List<Integer> getModelIds() {
        return modelIds;
    }

    public void setModelIds(List<Integer> modelIds) {
        this.modelIds = modelIds == null ? null : Collections.unmodifiableList(new ArrayList<>(modelIds));
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}

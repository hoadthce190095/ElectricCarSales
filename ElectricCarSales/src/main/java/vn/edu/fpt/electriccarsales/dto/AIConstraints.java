package vn.edu.fpt.electriccarsales.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product preferences derived from the current conversation only.
 */
public class AIConstraints {
    private BigDecimal budget;
    private BigDecimal minRange;
    private Integer minSeats;
    private String usagePurpose;
    private String preferredBrand;
    private BigDecimal dailyDistanceKm;
    private List<String> notes = List.of();
    private List<String> inferredFields = List.of();

    public AIConstraints() {
    }

    public AIConstraints(BigDecimal budget, BigDecimal minRange, Integer minSeats, String usagePurpose,
        String preferredBrand, BigDecimal dailyDistanceKm, List<String> notes, List<String> inferredFields) {
        setBudget(budget);
        setMinRange(minRange);
        setMinSeats(minSeats);
        setUsagePurpose(usagePurpose);
        setPreferredBrand(preferredBrand);
        setDailyDistanceKm(dailyDistanceKm);
        setNotes(notes);
        setInferredFields(inferredFields);
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

    public String getUsagePurpose() {
        return usagePurpose;
    }

    public void setUsagePurpose(String usagePurpose) {
        this.usagePurpose = usagePurpose;
    }

    public String getPreferredBrand() {
        return preferredBrand;
    }

    public void setPreferredBrand(String preferredBrand) {
        this.preferredBrand = preferredBrand;
    }

    public BigDecimal getDailyDistanceKm() {
        return dailyDistanceKm;
    }

    public void setDailyDistanceKm(BigDecimal dailyDistanceKm) {
        this.dailyDistanceKm = dailyDistanceKm;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public List<String> getInferredFields() {
        return inferredFields;
    }

    public void setInferredFields(List<String> inferredFields) {
        this.inferredFields = inferredFields == null ? List.of() : List.copyOf(inferredFields);
    }
}

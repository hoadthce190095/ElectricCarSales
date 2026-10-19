package com.electriccarsales.model;

import java.time.LocalDateTime;

public class ComparisonSession {
    private int comparisonSessionID;
    private int userID;
    private LocalDateTime createDate;

    public ComparisonSession() {
    }

    public int getComparisonSessionID() {
        return comparisonSessionID;
    }

    public void setComparisonSessionID(int comparisonSessionID) {
        this.comparisonSessionID = comparisonSessionID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "ComparisonSession{" + "comparisonSessionID=" + comparisonSessionID + ", userID=" + userID + ", createDate=" + createDate + '}';
    }

}

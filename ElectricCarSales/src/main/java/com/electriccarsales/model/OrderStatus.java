package com.electriccarsales.model;

public class OrderStatus {
    private int id;
    private String statusName;
    private String description;

    public OrderStatus() {
    }

    public OrderStatus(int id, String statusName, String description) {
        this.id = id;
        this.statusName = statusName;
        this.description = description;
    }

    public static String resolveDisplayName(int id, String statusName, String description) {
        if (description != null && !description.isBlank()) {
            return description;
        }
        String key = statusName != null ? statusName.trim().toUpperCase() : String.valueOf(id);
        switch (key) {
            case "1":
            case "PENDING":
                return "Chờ đặt cọc";
            case "2":
            case "RESERVED":
                return "Đã giữ xe";
            case "3":
            case "DEPOSITED":
                return "Đã đặt cọc";
            case "4":
            case "PROCESSING":
                return "Đang làm thủ tục";
            case "5":
            case "PAID":
                return "Đã thanh toán đủ";
            case "6":
            case "COMPLETED":
                return "Hoàn tất giao dịch";
            case "7":
            case "CANCELLED":
                return "Đã hủy";
            default:
                return statusName != null ? statusName : "Không xác định";
        }
    }

    public int getStatusId() {
        return id;
    }

    public String getStatusCode() {
        return statusName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderStatusID() {
        return id;
    }

    public void setOrderStatusID(int id) {
        this.id = id;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return resolveDisplayName(id, statusName, description);
    }
}

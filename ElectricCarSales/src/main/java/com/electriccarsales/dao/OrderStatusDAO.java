package com.electriccarsales.dao;

import com.electriccarsales.model.OrderStatus;
import util.DBContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderStatusDAO extends DBContext {

    private static final List<OrderStatus> DEFAULT_STATUSES = List.of(
            new OrderStatus(1, "PENDING", "Chờ đặt cọc"),
            new OrderStatus(2, "DEPOSITED", "Đã đặt cọc"),
            new OrderStatus(3, "PROCESSING", "Chờ hợp đồng"),
            new OrderStatus(4, "PAYMENT_PENDING", "Chờ thanh toán"),
            new OrderStatus(5, "READY_FOR_DELIVERY", "Chờ bàn giao xe"),
            new OrderStatus(6, "COMPLETED", "Đã hoàn tất bàn giao"),
            new OrderStatus(7, "CANCELLED", "Đã hủy")
    );

    public static String toStatusCode(int statusId) {
        switch (statusId) {
            case 2: return "DEPOSITED";
            case 3: return "PROCESSING";
            case 4: return "PAYMENT_PENDING";
            case 5: return "READY_FOR_DELIVERY";
            case 6: return "COMPLETED";
            case 7: return "CANCELLED";
            default: return "PENDING";
        }
    }

    public List<OrderStatus> findAll() {
        return new ArrayList<>(DEFAULT_STATUSES);
    }

    public OrderStatus findById(int statusId) {
        for (OrderStatus s : DEFAULT_STATUSES) {
            if (s.getStatusId() == statusId) {
                return s;
            }
        }
        return DEFAULT_STATUSES.get(0);
    }

    public OrderStatus findByCode(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT_STATUSES.get(0);
        }
        for (OrderStatus s : DEFAULT_STATUSES) {
            if (s.getStatusCode().equalsIgnoreCase(code) || String.valueOf(s.getStatusId()).equals(code)) {
                return s;
            }
        }
        return DEFAULT_STATUSES.get(0);
    }
}

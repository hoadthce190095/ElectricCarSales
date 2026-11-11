package com.electriccarsales.service;

import com.electriccarsales.dao.DeliveryDAO;
import com.electriccarsales.model.Delivery;

import java.util.List;

public class DeliveryService {
    private final DeliveryDAO deliveryDAO;

    public DeliveryService() {
        this.deliveryDAO = new DeliveryDAO();
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryDAO.getAllDeliveries();
    }

    public List<Delivery> getCustomerDeliveries(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Mã khách hàng không hợp lệ.");
        return deliveryDAO.findByCustomerId(customerId);
    }

    public boolean addDelivery(Delivery d) {
        return deliveryDAO.addDelivery(d);
    }

    public Delivery completeHandover(int orderId, Integer employeeId, String deliveryAddress, String note) {
        if (orderId <= 0) throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        return deliveryDAO.completeHandover(orderId, employeeId, deliveryAddress, note);
    }
}
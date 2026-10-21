package com.electriccarsales.service;

import com.electriccarsales.dao.DeliveryDAO;
import com.electriccarsales.model.Delivery;
import java.util.List;

public class DeliveryService {
    private DeliveryDAO deliveryDAO;

    public DeliveryService() {
        this.deliveryDAO = new DeliveryDAO();
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryDAO.getAllDeliveries();
    }

    public boolean addDelivery(Delivery d) {
        return deliveryDAO.addDelivery(d);
    }
}
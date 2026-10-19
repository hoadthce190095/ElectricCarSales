package com.electriccarsales.service;

import com.electriccarsales.dao.VehicleTCODAO;
import com.electriccarsales.model.VehicleTCO;

import java.util.List;

public class VehicleTCOService {
    private VehicleTCODAO vehicleTCODAO;

    public VehicleTCOService() {
        vehicleTCODAO = new VehicleTCODAO();
    }

    public List<VehicleTCO> getAllVehicleTCO(){
        return vehicleTCODAO.getAll();
    }
}

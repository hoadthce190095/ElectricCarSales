package com.electriccarsales.service;

import com.electriccarsales.dao.CarModelDAO;
import com.electriccarsales.model.CarModel;

import java.util.List;

public class CarModelService {
    private CarModelDAO carModelDAO;

    public CarModelService() {
        carModelDAO = new CarModelDAO();
    }

    public List<CarModel> getAllCarModels() {
        return carModelDAO.getAll();
    }

    public CarModel getCarModelById(int modelID) {
        return carModelDAO.getById(modelID);
    }
}

package com.electriccarsales.service;

import com.electriccarsales.dao.CarDAO;
import com.electriccarsales.dao.CarStatusDAO;
import com.electriccarsales.model.Car;
import com.electriccarsales.model.CarStatus;
import java.util.List;

public class CarInventoryService {
    private CarDAO carDAO;
    private CarStatusDAO carStatusDAO;

    public CarInventoryService() {
        this.carDAO = new CarDAO();
        this.carStatusDAO = new CarStatusDAO();
    }

    public List<Car> getAllCars() {
        return carDAO.getAllCars();
    }

    public boolean addCar(Car c) {
        return carDAO.addCar(c);
    }

    public List<CarStatus> getAllCarStatuses() {
        return carStatusDAO.getAllCarStatuses();
    }
}
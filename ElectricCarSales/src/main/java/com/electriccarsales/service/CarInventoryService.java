package com.electriccarsales.service;

import com.electriccarsales.dao.CarDAO;
import com.electriccarsales.dao.CarInventoryDAO;
import com.electriccarsales.dao.CarStatusDAO;
import com.electriccarsales.model.Car;
import com.electriccarsales.model.CarStatus;
import com.electriccarsales.model.InventoryCarDetail;
import com.electriccarsales.model.InventoryItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CarInventoryService {
    private final CarDAO carDAO;
    private final CarInventoryDAO carInventoryDAO;
    private final CarStatusDAO carStatusDAO;

    public CarInventoryService() {
        this.carDAO = new CarDAO();
        this.carInventoryDAO = new CarInventoryDAO();
        this.carStatusDAO = new CarStatusDAO();
    }

    public CarInventoryService(CarDAO carDAO, CarInventoryDAO carInventoryDAO, CarStatusDAO carStatusDAO) {
        this.carDAO = carDAO;
        this.carInventoryDAO = carInventoryDAO;
        this.carStatusDAO = carStatusDAO;
    }

    public List<Car> getInventoryCars(String keyword, String status) throws SQLException {
        return carInventoryDAO.getInventoryCars(keyword, status);
    }

    public Map<String, Integer> getInventoryStats() throws SQLException {
        return carInventoryDAO.getInventoryStats();
    }

    public List<InventoryItem> getInventorySummary() throws SQLException {
        return carInventoryDAO.getInventorySummary();
    }

    public List<InventoryCarDetail> getCarDetails(int modelId) throws SQLException {
        if (modelId <= 0) {
            throw new IllegalArgumentException("Mã mẫu xe không hợp lệ.");
        }
        return carInventoryDAO.getCarDetails(modelId);
    }

    public Car importVehicle(Car car, int employeeId, Integer supplierId, String note) {
        if (car == null) {
            throw new IllegalArgumentException("Thông tin xe nhập kho không được để trống.");
        }
        return carDAO.importVehicleInTransaction(car, employeeId, supplierId, note);
    }

    public boolean exportVehicle(int carId, int employeeId, String exportReason) {
        return carDAO.exportVehicleInTransaction(carId, employeeId, exportReason);
    }

    public List<Car> getAllCars() {
        return carDAO.getAllCars();
    }

    public boolean addCar(Car c) {
        return carDAO.addCar(c);
    }

    public boolean updateCarStatus(int carId, String statusName) {
        return carDAO.updateCarStatus(carId, statusName);
    }

    public List<CarStatus> getAllCarStatuses() {
        return carStatusDAO.getAllCarStatuses();
    }
}

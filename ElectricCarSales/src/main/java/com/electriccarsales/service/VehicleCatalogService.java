package com.electriccarsales.service;

import com.electriccarsales.dao.*;
import com.electriccarsales.model.*;

import java.util.List;

public class VehicleCatalogService {
    private BrandDAO brandDAO;
    private CarLineDAO carLineDAO;
    private CarModelDAO carModelDAO;
    private CarVersionDAO carVersionDAO;
    private SpecificationDAO specificationDAO;
    private ColorDAO colorDAO;
    private ModelColorDAO modelColorDAO;
    private CarImageDAO carImageDAO;

    public VehicleCatalogService() {
        brandDAO = new BrandDAO();
        carLineDAO = new CarLineDAO();
        carModelDAO = new CarModelDAO();
        carVersionDAO = new CarVersionDAO();
        specificationDAO = new SpecificationDAO();
        colorDAO = new ColorDAO();
        modelColorDAO = new ModelColorDAO();
        carImageDAO = new CarImageDAO();
    }

    public List<Brand> getAllBrands() {
        return brandDAO.getAll();
    }

    public List<CarLine> getAllCarLines() {
        return carLineDAO.getAll();
    }

    public List<CarModel> getAllCarModels() {
        return carModelDAO.getAll();
    }

    public List<CarVersion> getAllCarVersions() {
        return carVersionDAO.getAll();
    }

    public List<Specification> getAllSpecifications() {
        return specificationDAO.getAll();
    }

    public List<Color> getAllColors() {
        return colorDAO.getAll();
    }

    public List<ModelColor> getAllModelColors() {
        return modelColorDAO.getAll();
    }

    public List<CarImage> getAllCarImages() {
        return carImageDAO.getAll();
    }


    // Lấy thông tin chi tiết một mẫu xe theo ModelID
    public CarModel getCarModelById(int modelID) {
        return carModelDAO.getById(modelID);
    }

    public CarModel getCarModelByNameOrLine(String modelName, String carLineName) {
        if (modelName != null && !modelName.isBlank()) {
            return carModelDAO.getByName(modelName);
        }

        if (carLineName == null || carLineName.isBlank()) {
            return null;
        }

        String requestedLine = normalizeName(carLineName);
        List<Integer> matchingLineIds = carLineDAO.getAll().stream()
                .filter(line -> normalizeName(line.getCarLineName()).equals(requestedLine))
                .map(CarLine::getCarLineID)
                .toList();
        if (matchingLineIds.isEmpty()) {
            return null;
        }

        List<CarModel> matchingModels = carModelDAO.getAll().stream()
                .filter(car -> matchingLineIds.contains(car.getCarLineID()))
                .toList();
        return matchingModels.size() == 1 ? matchingModels.get(0) : null;
    }

    private String normalizeName(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    // Lấy thông số kỹ thuật theo ModelID
    public Specification getSpecificationByModelId(int modelID) {
        return specificationDAO.getByModelId(modelID);
    }

    // Lấy danh sách hình ảnh theo ModelID
    public List<CarImage> getImagesByModelId(int modelID) {
        return carImageDAO.getByModelId(modelID);
    }

}
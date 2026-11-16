package com.electriccarsales.service;

import com.electriccarsales.dao.*;
import com.electriccarsales.model.*;

import java.math.BigDecimal;
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
    private CatalogVehicleDAO catalogVehicleDAO;

    public VehicleCatalogService() {
    }

    private BrandDAO getBrandDAO() {
        if (brandDAO == null) brandDAO = new BrandDAO();
        return brandDAO;
    }

    private CarLineDAO getCarLineDAO() {
        if (carLineDAO == null) carLineDAO = new CarLineDAO();
        return carLineDAO;
    }

    private CarModelDAO getCarModelDAO() {
        if (carModelDAO == null) carModelDAO = new CarModelDAO();
        return carModelDAO;
    }

    private CarVersionDAO getCarVersionDAO() {
        if (carVersionDAO == null) carVersionDAO = new CarVersionDAO();
        return carVersionDAO;
    }

    private SpecificationDAO getSpecificationDAO() {
        if (specificationDAO == null) specificationDAO = new SpecificationDAO();
        return specificationDAO;
    }

    private ColorDAO getColorDAO() {
        if (colorDAO == null) colorDAO = new ColorDAO();
        return colorDAO;
    }

    private ModelColorDAO getModelColorDAO() {
        if (modelColorDAO == null) modelColorDAO = new ModelColorDAO();
        return modelColorDAO;
    }

    private CarImageDAO getCarImageDAO() {
        if (carImageDAO == null) carImageDAO = new CarImageDAO();
        return carImageDAO;
    }

    private CatalogVehicleDAO getCatalogVehicleDAO() {
        if (catalogVehicleDAO == null) catalogVehicleDAO = new CatalogVehicleDAO();
        return catalogVehicleDAO;
    }

    public List<Brand> getAllBrands() {
        return getBrandDAO().getAll();
    }

    public List<CarLine> getAllCarLines() {
        return getCarLineDAO().getAll();
    }

    public List<CarModel> getAllCarModels() {
        return getCarModelDAO().getAll();
    }

    public List<CarModel> searchCarModels(String keyword, Integer brandId, Integer carLineId,
                                          BigDecimal maxPrice, BigDecimal minRange, Integer colorId,
                                          String sort) {
        return getCarModelDAO().search(keyword, brandId, carLineId, maxPrice, minRange, colorId, sort);
    }

    public List<CatalogVehicle> getCatalogVehicles() {
        return getCatalogVehicleDAO().getAll();
    }

    public List<CarVersion> getAllCarVersions() {
        return getCarVersionDAO().getAll();
    }

    public List<Specification> getAllSpecifications() {
        return getSpecificationDAO().getAll();
    }

    public List<Color> getAllColors() {
        return getColorDAO().getAll();
    }

    public List<ModelColor> getAllModelColors() {
        return getModelColorDAO().getAll();
    }

    public List<CarImage> getAllCarImages() {
        return getCarImageDAO().getAll();
    }


    // Lấy thông tin chi tiết một mẫu xe theo ModelID
    public CarModel getCarModelById(int modelID) {
        return getCarModelDAO().getById(modelID);
    }

    public CarModel getCarModelByNameOrLine(String modelName, String carLineName) {
        if (modelName != null && !modelName.isBlank()) {
            return getCarModelDAO().getByName(modelName);
        }

        if (carLineName == null || carLineName.isBlank()) {
            return null;
        }

        String requestedLine = normalizeName(carLineName);
        List<Integer> matchingLineIds = getCarLineDAO().getAll().stream()
                .filter(line -> normalizeName(line.getCarLineName()).equals(requestedLine))
                .map(CarLine::getCarLineID)
                .toList();
        if (matchingLineIds.isEmpty()) {
            return null;
        }

        List<CarModel> matchingModels = getCarModelDAO().getAll().stream()
                .filter(car -> matchingLineIds.contains(car.getCarLineID()))
                .toList();
        return matchingModels.size() == 1 ? matchingModels.get(0) : null;
    }

    private String normalizeName(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    // Lấy thông số kỹ thuật theo ModelID
    public Specification getSpecificationByModelId(int modelID) {
        return getSpecificationDAO().getByModelId(modelID);
    }

    // Lấy danh sách hình ảnh theo ModelID
    public List<CarImage> getImagesByModelId(int modelID) {
        return getCarImageDAO().getByModelId(modelID);
    }

}
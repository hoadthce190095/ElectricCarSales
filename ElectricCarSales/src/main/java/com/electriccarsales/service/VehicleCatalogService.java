package com.electriccarsales.service;

import com.electriccarsales.dao.BrandDAO;
import com.electriccarsales.dao.CarImageDAO;
import com.electriccarsales.dao.CarLineDAO;
import com.electriccarsales.dao.CarModelDAO;
import com.electriccarsales.dao.CarVersionDAO;
import com.electriccarsales.dao.CatalogVehicleDAO;
import com.electriccarsales.dao.ColorDAO;
import com.electriccarsales.dao.ModelColorDAO;
import com.electriccarsales.dao.SpecificationDAO;
import com.electriccarsales.model.Brand;
import com.electriccarsales.model.CarImage;
import com.electriccarsales.model.CarLine;
import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.CarVersion;
import com.electriccarsales.model.CatalogVehicle;
import com.electriccarsales.model.Color;
import com.electriccarsales.model.ModelColor;
import com.electriccarsales.model.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehicleCatalogService {
    private final BrandDAO brandDAO = new BrandDAO();
    private final CarLineDAO carLineDAO = new CarLineDAO();
    private final CarModelDAO carModelDAO = new CarModelDAO();
    private final CarVersionDAO carVersionDAO = new CarVersionDAO();
    private final SpecificationDAO specificationDAO = new SpecificationDAO();
    private final ColorDAO colorDAO = new ColorDAO();
    private final ModelColorDAO modelColorDAO = new ModelColorDAO();
    private final CarImageDAO carImageDAO = new CarImageDAO();
    private final CatalogVehicleDAO catalogVehicleDAO = new CatalogVehicleDAO();

    public List<Brand> getAllBrands() {
        return brandDAO.getAll();
    }

    public List<CarLine> getAllCarLines() {
        return carLineDAO.getAll();
    }

    public List<CarModel> getAllCarModels() {
        return carModelDAO.getAll();
    }

    public List<CarModel> searchCarModels(String keyword, Integer brandId, Integer carLineId,
                                          BigDecimal maxPrice, BigDecimal minRange, Integer colorId,
                                          String sort) {
        return carModelDAO.search(keyword, brandId, carLineId, maxPrice, minRange, colorId, sort);
    }

    public List<CatalogVehicle> getCatalogVehicles() {
        return catalogVehicleDAO.getAll();
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

        List<Integer> matchingLineIds = new ArrayList<>();
        for (CarLine line : carLineDAO.getAll()) {
            if (normalizeName(line.getCarLineName()).equals(normalizeName(carLineName))) {
                matchingLineIds.add(line.getCarLineID());
            }
        }

        List<CarModel> matchingModels = new ArrayList<>();
        for (CarModel car : carModelDAO.getAll()) {
            if (matchingLineIds.contains(car.getCarLineID())) {
                matchingModels.add(car);
            }
        }
        return matchingModels.size() == 1 ? matchingModels.get(0) : null;
    }

    private String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public Specification getSpecificationByModelId(int modelID) {
        return specificationDAO.getByModelId(modelID);
    }

    public List<CarImage> getImagesByModelId(int modelID) {
        return carImageDAO.getByModelId(modelID);
    }
}
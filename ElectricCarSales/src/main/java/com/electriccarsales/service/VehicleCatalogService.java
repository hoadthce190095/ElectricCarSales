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

    // Lấy thông số kỹ thuật theo ModelID
    public Specification getSpecificationByModelId(int modelID) {
        List<Specification> specs = specificationDAO.getAll();
        for (Specification spec : specs) {
            if (spec.getModelID() == modelID) {
                return spec;
            }
        }
        return null;
    }

    // Lấy danh sách hình ảnh theo ModelID
    public List<CarImage> getImagesByModelId(int modelID) {
        List<CarImage> allImages = carImageDAO.getAll();
        List<CarImage> result = new java.util.ArrayList<>();
        for (CarImage img : allImages) {
            if (img.getModelID() == modelID) {
                result.add(img);
            }
        }
        return result;
    }

}
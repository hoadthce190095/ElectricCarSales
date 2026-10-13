package com.electriccarsales.service;

import com.electriccarsales.dao.*;
import com.electriccarsales.model.*;

import java.util.List;

public class VehicleCatalogService {
    private BrandDAO brandDAO;
    private CarLineDAO carLineDAO;
    private CarModelDAO  carModelDAO;
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


    public static void main(String[] args) {
        VehicleCatalogService vehicleCatalogService = new VehicleCatalogService();
        List<Brand> brands = vehicleCatalogService.getAllBrands();
        for (Brand brand : brands) {
            System.out.println(brand);
        }
    }
}

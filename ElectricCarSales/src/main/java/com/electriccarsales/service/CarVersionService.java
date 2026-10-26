package com.electriccarsales.service;

import com.electriccarsales.dao.CarVersionDAO;
import com.electriccarsales.model.CarVersion;

import java.util.List;

public class CarVersionService {
    private CarVersionDAO carVersionDAO;

    public  CarVersionService() {
        carVersionDAO = new CarVersionDAO();
    }

    public List<CarVersion> getAllCarVersion() {
        return carVersionDAO.getAll();
    }

}


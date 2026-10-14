package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.model.CarImage;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@WebServlet(name = "VehicleController", urlPatterns = {"/home", "/cars"})
public class VehicleController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        VehicleCatalogService service = new VehicleCatalogService();


        List<CarModel> carList = service.getAllCarModels();


        Map<Integer, String> carImagesMap = new HashMap<>();
        for (CarModel car : carList) {
            List<CarImage> images = service.getImagesByModelId(car.getModelID());
            if (images != null && !images.isEmpty()) {
                carImagesMap.put(car.getModelID(), images.get(0).getImageURL());
            } else {
                carImagesMap.put(car.getModelID(), "assets/images/default-car.png");
            }
        }


        request.setAttribute("carList", carList);
        request.setAttribute("carImagesMap", carImagesMap);


        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}
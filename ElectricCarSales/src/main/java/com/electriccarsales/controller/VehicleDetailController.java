package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.service.CarModelService;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("vehicle-detail")
public class VehicleDetailController extends HttpServlet {
    private CarModelService carModelService;

    @Override
    public void init() {
        carModelService = new CarModelService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doGet(request, response);

        String id = request.getParameter("id");
        int modelID = Integer.parseInt(id);

        CarModel carModel = carModelService.getCarModelById(modelID);
        request.setAttribute("carModel", carModel);
        request.getRequestDispatcher("/vehicle/vehicle-detail.jsp").forward(request, response);
    }
}

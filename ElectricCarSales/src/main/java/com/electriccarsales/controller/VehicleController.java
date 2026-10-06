package com.electriccarsales.controller;

import com.electriccarsales.model.CarModel;
import com.electriccarsales.service.VehicleCatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/vehicles")
public class VehicleController extends HttpServlet {
    private VehicleCatalogService vehicleCatalogService;

    @Override
    public void init(){
        vehicleCatalogService = new VehicleCatalogService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        List<CarModel> carModels = vehicleCatalogService.getAllCarModels();

        request.setAttribute("carModels", carModels);
        request.getRequestDispatcher("/vehicle.jsp").forward(request, response);

    }
}

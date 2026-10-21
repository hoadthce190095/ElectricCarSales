package com.electriccarsales.controller;

import com.electriccarsales.model.Car;
import com.electriccarsales.model.CarStatus;
import com.electriccarsales.service.CarInventoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CarController", urlPatterns = {"/car"})
public class CarController extends HttpServlet {

    private CarInventoryService carService;

    @Override
    public void init() throws ServletException {
        carService = new CarInventoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");


        List<Car> cars = carService.getAllCars();
        List<CarStatus> statuses = carService.getAllCarStatuses();

        request.setAttribute("carList", cars);
        request.setAttribute("statusList", statuses);

        request.getRequestDispatcher("/car.jsp").forward(request, response);
    }
}
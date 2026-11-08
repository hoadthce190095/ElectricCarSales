package com.electriccarsales.controller;

import com.electriccarsales.model.Supplier;
import com.electriccarsales.service.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "SupplierController", urlPatterns = {"/supplier"})
public class SupplierController extends HttpServlet {

    private SupplierService supplierService;

    @Override
    public void init() throws ServletException {
        supplierService = new SupplierService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }

        if (action.equals("list")) {
            listSuppliers(request, response);
        } else {
            listSuppliers(request, response);
        }
    }

    private void listSuppliers(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        request.setAttribute("supplierList", suppliers);

        request.getRequestDispatcher("/supplier.jsp").forward(request, response);
    }
}
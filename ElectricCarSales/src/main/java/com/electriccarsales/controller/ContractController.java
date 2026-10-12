package com.electriccarsales.controller;

import com.electriccarsales.model.Contract;
import com.electriccarsales.service.ContractsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ContractController", urlPatterns = {"/contract"})
public class ContractController extends HttpServlet {

    private ContractsService contractsService;

    @Override
    public void init() throws ServletException {
        contractsService = new ContractsService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");

        List<Contract> contracts = contractsService.getAllContracts();
        request.setAttribute("contractList", contracts);

        request.getRequestDispatcher("/contract.jsp").forward(request, response);
    }
}
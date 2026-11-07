package com.electriccarsales.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "VehicleSearchController", urlPatterns = {"/cars-search", "/cars/search"})
public class VehicleSearchController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String keyword = request.getParameter("q");
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.length() > 100) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Từ khóa tìm kiếm tối đa 100 ký tự.");
                return;
            }
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        request.setAttribute("searchKeyword", keyword);
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}

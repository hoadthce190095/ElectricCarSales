/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import dao.OrderDAO;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import model.OrderDetail;
import model.Orders;

/**
 *
 * @author Hoa Hà
 */
@WebServlet(name = "OrderServlet", urlPatterns = {"/order"})
public class OrderServlet extends HttpServlet {

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        OrderDAO dao = new OrderDAO();
        if (action == null || action.equalsIgnoreCase("list")) {

            request.setAttribute("orderList", dao.getAllOrders());

            request.getRequestDispatcher("/order-list.jsp").forward(request, response);

        } else if (action.equalsIgnoreCase("detail")) {
            try {
                int orderID = Integer.parseInt(request.getParameter("id"));
                Orders order = dao.getOrderByID(orderID);
                if (order != null) {
                    List<OrderDetail> detailList = dao.getOrderDetailByID(orderID);

                    request.setAttribute("order", order);
                    request.setAttribute("detailList", detailList);
                    request.getRequestDispatcher("/order-detail.jsp").forward(request, response);

                } else {
                    response.sendRedirect(request.getContextPath() + "/order");
                }

            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/order");
            }
        }

    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

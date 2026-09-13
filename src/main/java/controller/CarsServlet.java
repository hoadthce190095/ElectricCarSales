/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import dao.CarModelDAO;
import dao.CarsDAO;
import dao.OrderDAO;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.CarModels;
import model.Cars;

/**
 *
 * @author Hoa Hà
 */
@WebServlet(name = "CarsServlet", urlPatterns = {"/cars"})
public class CarsServlet extends HttpServlet {

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
        //LIST
        if (action == null) {
            CarModelDAO dao = new CarModelDAO();
            request.setAttribute("carList", dao.getAll());
            request.getRequestDispatcher("/car-list.jsp").forward(request, response);
            //DETAIL
        } else if (action.equalsIgnoreCase("detail")) {
            String id = request.getParameter("id");
            int modelID = Integer.parseInt(id);

            CarModelDAO modelDAO = new CarModelDAO();
            CarsDAO carDAO = new CarsDAO();
            //thong tin mau xe va xe cu the
            CarModels carModel = modelDAO.getByID(modelID);

            request.setAttribute("carModel", carModel);
            request.setAttribute("carList", carDAO.getByModelID(modelID));
            request.getRequestDispatcher("/car-detail.jsp").forward(request, response);
            //ORDER
        } else if (action.equalsIgnoreCase("order")) {
            String id = request.getParameter("id");
            int carID = Integer.parseInt(id);
            CarsDAO carDAO = new CarsDAO();
            Cars car = carDAO.getByID(carID);

            request.setAttribute("car", car);

            request.getRequestDispatcher("/order.jsp").forward(request, response);
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

        String action = request.getParameter("action");

        if (action != null && action.equalsIgnoreCase("order")) {
            try {
                //lay thong tin tu order.jsp
                int carID = Integer.parseInt(request.getParameter("carID"));
                String customerName = request.getParameter("customerName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String address = request.getParameter("address");

                // xac thuc thong tin
                if (customerName == null
                        || customerName.trim().isEmpty()
                        || phone == null
                        || phone.trim().isEmpty()
                        || address == null
                        || address.trim().isEmpty()) {
                    request.setAttribute("error", "Please all required information");

                    request.getRequestDispatcher("/order.jsp").forward(request, response);
                    return;
                }

                //Order
                OrderDAO orderDAO = new OrderDAO();

                int orderID = orderDAO.createOrder(
                        customerName.trim(),
                        phone.trim(),
                        email != null ? email.trim() : "",
                        address.trim(),
                        carID);

                //Result
                if (orderID != -1) {
                    request.setAttribute("orderID", orderID);
                    request.setAttribute("customerName", customerName);
                    request.setAttribute("carID", carID);
                    request.getRequestDispatcher("/order-success.jsp").forward(request, response);

                } else {
                    request.setAttribute("error", "This car no longer available or order creation failed");
                    request.getRequestDispatcher("/order.jsp").forward(request, response);
                }
            } catch (NumberFormatException e) {
                request.setAttribute("error", "Invaild Car ID");

                request.getRequestDispatcher("/order.jsp").forward(request, response);
            } catch (Exception e) {

                e.printStackTrace();

                request.setAttribute("error", "An error occurred while creating the order."
                );

                request.getRequestDispatcher("/order.jsp").forward(request, response);
            }
        }
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

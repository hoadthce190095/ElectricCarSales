package com.electriccarsales.controller;

import com.electriccarsales.dao.ConsultationStatusDAO;
import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.CarModelDAO;
import com.electriccarsales.model.ConsultationRequest;
import com.electriccarsales.service.ConsultationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/** Standard form-based Servlet for customer consultation and staff status updates. */
@WebServlet(urlPatterns={"/consultation","/customer/consultation","/staff/consultations","/staff/consultation/update"})
public class ConsultationController extends HttpServlet {
    private final ConsultationService service = new ConsultationService();
    private final ConsultationStatusDAO statuses = new ConsultationStatusDAO();

    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException,IOException {
        try {
            if(request.getServletPath().startsWith("/staff/")){
                if (!isStaff(request)) { response.sendError(403); return; }
                List<ConsultationRequest> all=service.getAllRequests();
                String query=request.getParameter("q");
                String status=request.getParameter("status");
                List<ConsultationRequest> filtered=all.stream().filter(row->{
                    boolean st=status==null||status.isBlank()||String.valueOf(row.getStatusId()).equals(status);
                    boolean matches=query==null||query.isBlank()||
                        (String.valueOf(row.getId())+" "+row.getCustomerName()+" "+row.getCustomerPhone()+" "+row.getModelName())
                            .toLowerCase().contains(query.trim().toLowerCase());
                    return st&&matches;
                }).toList();
                request.setAttribute("consultationList",filtered);
                request.setAttribute("totalCount",all.size());
                request.setAttribute("pendingCount",all.stream().filter(c->"PENDING".equals(c.getStatus())).count());
                request.setAttribute("inProgressCount",all.stream().filter(c->"IN_PROGRESS".equals(c.getStatus())).count());
                request.setAttribute("completedCount",all.stream().filter(c->"COMPLETED".equals(c.getStatus())).count());
                request.setAttribute("statusList", statuses.findAll());
                request.setAttribute("searchKeyword",query);
                request.setAttribute("selectedStatus",status);
                request.setAttribute("consultationControllerLoaded",true);
                request.getRequestDispatcher("/staff/consultation-management.jsp").forward(request,response);
            } else {
                // Existing form is retained. Customer ID must be obtained from the server session, not hidden input.
                HttpSession session=request.getSession(false);
                if(session==null||session.getAttribute("userID")==null){
                    response.sendRedirect(request.getContextPath()+"/login");return;
                }
                request.setAttribute("carModels",new CarModelDAO().getAll());
                request.setAttribute("consultationControllerLoaded",true);
                request.getRequestDispatcher("/customer/consultation.jsp").forward(request,response);
            }
        } catch(RuntimeException ex){ log("Consultation load failed",ex); response.sendError(503,"Cannot load consultations"); }
    }

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)
        throws ServletException,IOException {
        request.setCharacterEncoding("UTF-8");
        String path=request.getServletPath();
        try {
            if(path.equals("/staff/consultation/update")) {
                if (!isStaff(request)) { response.sendError(403); return; }
                int id=Integer.parseInt(request.getParameter("requestId"));
                int status=Integer.parseInt(request.getParameter("statusId"));
                service.updateRequestStatus(id,status,null);
                response.sendRedirect(request.getContextPath()+"/staff/consultations?updated=true");
                return;
            }
            HttpSession session=request.getSession(false);
            Object id=session==null?null:session.getAttribute("userID");
            if(!(id instanceof Number)){
                response.sendRedirect(request.getContextPath()+"/login");return;
            }
            // Resolve CustomerID from Users.UserID on the server, not an untrusted form field.
            int customerId=CustomerDAO.findCustomerIdByUserId(((Number)id).intValue());
            if(customerId<=0)throw new IllegalArgumentException("Customer account missing");
            ConsultationRequest row=new ConsultationRequest();
            row.setCustomerId(customerId);
            String carModel=request.getParameter("carModel");
            if(carModel!=null&&!carModel.isBlank())row.setModelId(Integer.parseInt(carModel));
            row.setContent(request.getParameter("content"));
            service.createRequest(row);
            response.sendRedirect(request.getContextPath()+"/customer/consultation?success=true");
        } catch(Exception ex){
            log("Consultation save failed",ex);
            response.sendRedirect(request.getContextPath()+"/"+(path.startsWith("/staff/")?"staff/consultations":"customer/consultation")+"?error=true");
        }
    }
    private boolean isStaff(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s == null || s.getAttribute("userID") == null) return false;
        String role = String.valueOf(s.getAttribute("roleName"));
        return "ADMIN".equalsIgnoreCase(role) || "SALES".equalsIgnoreCase(role)
            || "MANAGER".equalsIgnoreCase(role) || "OPERATIONS".equalsIgnoreCase(role);
    }
}

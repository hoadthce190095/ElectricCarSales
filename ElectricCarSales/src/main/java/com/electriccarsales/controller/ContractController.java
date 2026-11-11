package com.electriccarsales.controller;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.dao.FinancialCompanyDAO;
import com.electriccarsales.model.Contract;
import com.electriccarsales.model.Customer;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.ContractsService;
import com.electriccarsales.service.InstallmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet(name = "ContractController", urlPatterns = {"/contract"})
public class ContractController extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ContractsService contractsService;
    private InstallmentService installmentService;

    @Override
    public void init() throws ServletException {
        contractsService = new ContractsService();
        installmentService = new InstallmentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);

        try {
            List<Contract> contracts;
            if ("CUSTOMER".equals(role)) {
                Customer customer = new CustomerDAO().findByUserId(user.getUserId());
                if (customer == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                contracts = contractsService.getCustomerContracts(customer.getCustomerID());
            } else if (List.of("SALES", "ACCOUNTANT", "MANAGER", "ADMIN", "OPERATIONS").contains(role)) {
                contracts = contractsService.getAllContracts();
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            request.setAttribute("contractList", contracts);
            request.setAttribute("installmentPlans", installmentService.getPlans());
            request.setAttribute("financialCompanies", new FinancialCompanyDAO().findAll());

            if (jsonRequested) {
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("contracts", contracts);
                payload.put("installmentPlans", installmentService.getPlans());
                payload.put("financialCompanies", new FinancialCompanyDAO().findAll());
                JSON.writeValue(response.getWriter(), payload);
                return;
            }

            if ("CUSTOMER".equals(role)) {
                request.getRequestDispatcher("/customer/my-contracts.jsp").forward(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/staff/dashboard#contracts");
            }
        } catch (RuntimeException ex) {
            log("Could not load contracts", ex);
            if (jsonRequested) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json;charset=UTF-8");
                JSON.writeValue(response.getWriter(), Map.of("error", ex.getMessage()));
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể tải danh sách hợp đồng.");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean jsonRequested = "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        String action = request.getParameter("action");
        if (action == null || action.isBlank()) {
            action = "create";
        }

        try {
            if ("create".equalsIgnoreCase(action)) {
                if (!List.of("SALES", "ACCOUNTANT", "ADMIN").contains(role)) {
                    sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                            "Theo RDS (UC-13), chỉ Sales Staff hoặc Accountant mới có quyền lập hợp đồng.", "/staff/dashboard#contracts");
                    return;
                }
                int orderId = Integer.parseInt(request.getParameter("orderId"));
                String terms = request.getParameter("note");
                Integer empId = (Integer) session.getAttribute("employeeId");
                Contract created = contractsService.createContractForOrder(orderId, empId, terms);
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_CREATED, true,
                        "Đã lập hợp đồng " + created.getContractNumber() + " cho đơn hàng #" + orderId + ".",
                        "/staff/dashboard#contracts");
                return;
            }

            if (!"CUSTOMER".equals(role)) {
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Chức năng ký hợp đồng bằng OTP dành cho Khách hàng.", "/contract");
                return;
            }
            Customer customer = new CustomerDAO().findByUserId(user.getUserId());
            if (customer == null) {
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_FORBIDDEN, false,
                        "Tài khoản chưa liên kết hồ sơ khách hàng.", "/contract");
                return;
            }

            int contractId = Integer.parseInt(request.getParameter("contractId"));
            if ("requestOtp".equalsIgnoreCase(action)) {
                contractsService.requestSigningOtp(contractId, customer.getCustomerID(), user.getUserId());
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã gửi mã OTP ký hợp đồng (hiệu lực 5 phút). Vui lòng kiểm tra email hoặc Console của IntelliJ.",
                        "/contract?otpSent=" + contractId);
                return;
            }

            if ("sign".equalsIgnoreCase(action)) {
                String otp = request.getParameter("otp");
                Contract signed = contractsService.verifyOtpAndSignContract(
                        contractId, customer.getCustomerID(), user.getUserId(), otp);
                sendResponse(request, response, jsonRequested, HttpServletResponse.SC_OK, true,
                        "Đã ký hợp đồng " + signed.getContractNumber() + " thành công bằng mã OTP!",
                        "/contract?signed=" + contractId);
                return;
            }

            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Thao tác hợp đồng không hợp lệ.", "/contract");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_BAD_REQUEST, false,
                    ex.getMessage(), "CUSTOMER".equals(role) ? "/contract" : "/staff/dashboard#contracts");
        } catch (RuntimeException ex) {
            log("Contract POST failed", ex);
            sendResponse(request, response, jsonRequested, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý hợp đồng: " + ex.getMessage(), "/contract");
        }
    }

    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequested,
                              int httpStatus, boolean success, String message, String redirectPath)
            throws IOException, ServletException {
        if (jsonRequested) {
            resp.setStatus(httpStatus);
            resp.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(resp.getWriter(), Map.of("success", success, "message", message));
            return;
        }
        if (success) {
            resp.sendRedirect(req.getContextPath() + redirectPath);
        } else {
            req.setAttribute("contractError", message);
            doGet(req, resp);
        }
    }
}
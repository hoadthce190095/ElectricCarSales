package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceDAOException;
import com.electriccarsales.model.InstallmentContract;
import com.electriccarsales.service.InstallmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Installment plan catalog and customer-owned contract/schedule queries. */
@WebServlet("/installment/*")
public class InstallmentController extends HttpServlet {
    private InstallmentService installmentService;

    public InstallmentController() {
        this(new InstallmentService());
    }

    InstallmentController(InstallmentService installmentService) {
        this.installmentService = installmentService;
    }

    @Override
    public void init() {
        if (installmentService == null) installmentService = new InstallmentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;

        try {
            String action = action(request);
            if (!action.isEmpty() && !action.equals("plans") && !action.equals("detail")
                    && !action.equals("schedule") && !action.equals("debt")
                    && !action.equals("status") && !action.equals("payments")) {
                FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Chức năng trả góp không tồn tại.");
                return;
            }
            request.setAttribute("installmentPlans", installmentService.getPlans());
            request.setAttribute("financialCompanies", installmentService.getFinancialCompanies());
            Integer contractId = FinanceControllerSupport.positiveInt(request.getParameter("contractId"));
            Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
            InstallmentContract contract = null;

            if (contractId != null) {
                contract = installmentService.getContractDetails(contractId);
                if (contract == null || !installmentService.contractBelongsToCustomer(contractId, customerId)) {
                    FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy hợp đồng trả góp.");
                    return;
                }
            } else if (orderId != null) {
                if (!installmentService.orderBelongsToCustomer(orderId, customerId)) {
                    FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy đơn hàng.");
                    return;
                }
                contract = installmentService.getContractForOrder(orderId);
                if (contract != null) {
                    contract = installmentService.getContractDetails(contract.getInstallmentContractID());
                }
            }

            if (contract != null) {
                request.setAttribute("installmentContract", contract);
                request.setAttribute("installmentSchedule",
                        installmentService.getSchedule(contract.getInstallmentContractID()));
                request.setAttribute("installmentPaymentHistory",
                        installmentService.getPaymentHistory(contract.getInstallmentContractID()));
                request.setAttribute("outstandingDebt",
                        installmentService.getOutstandingDebt(contract.getInstallmentContractID()));
            }
            request.getRequestDispatcher("/customer/my-contracts.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thông tin trả góp không hợp lệ.");
        } catch (IllegalStateException e) {
            getServletContext().log("Installment balance is inconsistent.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_CONFLICT,
                    "Dữ liệu công nợ trả góp chưa nhất quán; vui lòng liên hệ nhân viên hỗ trợ.");
        } catch (FinanceDAOException e) {
            getServletContext().log("Installment query failed.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải thông tin trả góp. Vui lòng thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;
        String action = action(request);
        if (!"create".equals(action) && !"pay".equals(action)) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Chức năng trả góp không tồn tại.");
            return;
        }
        Integer orderId = FinanceControllerSupport.positiveInt(request.getParameter("orderId"));
        Integer contractId = FinanceControllerSupport.positiveInt(request.getParameter("contractId"));
        try {
            if ("create".equals(action)) {
                if (orderId == null || !installmentService.orderBelongsToCustomer(orderId, customerId)) {
                    FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy đơn hàng.");
                    return;
                }
            } else {
                if (contractId == null || !installmentService.contractBelongsToCustomer(contractId, customerId)) {
                    FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Không tìm thấy hợp đồng trả góp.");
                    return;
                }
            }
            // Contract creation needs an eligible-order/confirmed-deposit transaction; payment needs a gateway.
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Nghiệp vụ này chưa được bật vì chưa có xác minh điều kiện đơn hàng và thanh toán.");
        } catch (FinanceDAOException e) {
            getServletContext().log("Installment authorization query failed.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể xác minh quyền truy cập. Vui lòng thử lại sau.");
        }
    }

    private static String action(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.equals("/") ? "" : path.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}

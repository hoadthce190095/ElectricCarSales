package com.electriccarsales.controller;

import com.electriccarsales.dao.FinanceDAOException;
import com.electriccarsales.dao.InstallmentContractDAO;
import com.electriccarsales.dao.InstallmentSettlementDAO;
import com.electriccarsales.model.InstallmentContract;
import com.electriccarsales.service.InstallmentService;
import com.electriccarsales.service.SettlementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Customer settlement quote lookup; settlement remains unavailable until a lender ledger and verified payment exist. */
@WebServlet("/settlement/*")
public class SettlementController extends HttpServlet {
    private SettlementService settlementService;
    private InstallmentService installmentService;

    public SettlementController() {
        this(new SettlementService(new InstallmentContractDAO(), new InstallmentSettlementDAO(), contractId -> {
            throw new IllegalStateException("Authoritative principal, accrued interest, and fee data are not configured.");
        }), new InstallmentService());
    }

    SettlementController(SettlementService settlementService, InstallmentService installmentService) {
        this.settlementService = settlementService;
        this.installmentService = installmentService;
    }

    @Override
    public void init() {
        if (settlementService == null || installmentService == null) {
            SettlementController defaults = new SettlementController();
            settlementService = defaults.settlementService;
            installmentService = defaults.installmentService;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;
        String action = action(request);
        if (!action.isEmpty() && !action.equals("quote") && !action.equals("status")) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Chức năng tất toán không tồn tại.");
            return;
        }
        Integer contractId = FinanceControllerSupport.positiveInt(request.getParameter("contractId"));
        if (contractId == null) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thiếu mã hợp đồng trả góp hợp lệ.");
            return;
        }
        try {
            InstallmentContract contract = installmentService.getContractDetails(contractId);
            if (contract == null || !installmentService.contractBelongsToCustomer(contractId, customerId)) {
                FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Không tìm thấy hợp đồng trả góp.");
                return;
            }
            request.setAttribute("installmentContract", contract);
            request.setAttribute("settlement", settlementService.getSettlement(contractId));
            try {
                request.setAttribute("settlementQuote", settlementService.calculateQuote(contractId));
            } catch (IllegalStateException | IllegalArgumentException unavailable) {
                request.setAttribute("settlementQuoteUnavailable", true);
            }
            request.getRequestDispatcher("/customer/my-contracts.jsp").forward(request, response);
        } catch (FinanceDAOException e) {
            getServletContext().log("Settlement query failed.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tải thông tin tất toán. Vui lòng thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer customerId = FinanceControllerSupport.customerId(request, response);
        if (customerId == null) return;
        String action = action(request);
        if (!action.equals("create") && !action.equals("request")) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Chức năng tất toán không tồn tại.");
            return;
        }
        Integer contractId = FinanceControllerSupport.positiveInt(request.getParameter("contractId"));
        if (contractId == null) {
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Thiếu mã hợp đồng trả góp hợp lệ.");
            return;
        }
        try {
            if (!installmentService.contractBelongsToCustomer(contractId, customerId)) {
                FinanceControllerSupport.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Không tìm thấy hợp đồng trả góp.");
                return;
            }
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể tạo yêu cầu tất toán khi chưa có báo giá từ đơn vị tài chính và xác minh thanh toán.");
        } catch (FinanceDAOException e) {
            getServletContext().log("Settlement authorization query failed.", e);
            FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Chưa thể xác minh quyền truy cập. Vui lòng thử lại sau.");
        }
    }

    private static String action(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.equals("/") ? "" : path.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}

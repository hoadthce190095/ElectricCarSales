package com.electriccarsales.controller;

import com.electriccarsales.dao.ContractsDAO;
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
            request.setAttribute("financialCompanies", installmentService.getFinancialCompanies());
            request.setAttribute("contractList", new ContractsDAO().findByCustomerId(customerId));

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
        // Per RDS EX-08 & BR-05, periodic installment collection is handled by the partner bank outside the website.
        FinanceControllerSupport.sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Website thu 30% tiền đặt cọc; sau khi Kế toán đối soát khoản cọc và khách ký hợp đồng, đại lý chờ nhận khoản giải ngân 70% còn lại. Các kỳ trả góp hàng tháng được thanh toán trực tiếp với đối tác.");
    }

    private static String action(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.equals("/") ? "" : path.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}

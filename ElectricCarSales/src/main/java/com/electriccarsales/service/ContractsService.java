package com.electriccarsales.service;

import com.electriccarsales.dao.ContractsDAO;
import com.electriccarsales.model.Contract;

import java.util.List;

public class ContractsService {
    private final ContractsDAO contractsDAO;
    private final OtpService otpService;

    public ContractsService() {
        this.contractsDAO = new ContractsDAO();
        this.otpService = new OtpService();
    }

    public List<Contract> getAllContracts() {
        return contractsDAO.getAllContracts();
    }

    public List<Contract> getCustomerContracts(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Mã khách hàng không hợp lệ.");
        return contractsDAO.findByCustomerId(customerId);
    }

    public Contract getContractById(int contractId) {
        if (contractId <= 0) throw new IllegalArgumentException("Mã hợp đồng không hợp lệ.");
        return contractsDAO.findById(contractId);
    }

    public Contract getContractByOrderId(int orderId) {
        if (orderId <= 0) throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        return contractsDAO.findByOrderId(orderId);
    }

    public boolean addContract(Contract c) {
        return contractsDAO.addContract(c);
    }

    public Contract createContractForOrder(int orderId, Integer staffEmployeeId, String customTerms) {
        if (orderId <= 0) throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        return contractsDAO.createContractForOrder(orderId, staffEmployeeId, customTerms);
    }

    public String requestSigningOtp(int contractId, int customerId, int userId) {
        Contract c = contractsDAO.findById(contractId);
        if (c == null || c.getCustomerID() != customerId) {
            throw new IllegalArgumentException("Không tìm thấy hợp đồng của bạn.");
        }
        if ("SIGNED".equalsIgnoreCase(c.getStatus())) {
            throw new IllegalStateException("Hợp đồng này đã được ký.");
        }
        return otpService.issuePurposeOtp(userId, "SIGN_CONTRACT");
    }

    public Contract verifyOtpAndSignContract(int contractId, int customerId, int userId, String otpCode) {
        if (otpCode == null || !otpCode.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP gồm 6 chữ số để ký hợp đồng.");
        }
        if (!otpService.verifyPurposeOtp(userId, "SIGN_CONTRACT", otpCode.trim())) {
            throw new IllegalArgumentException("Mã OTP ký hợp đồng không đúng hoặc đã hết hạn.");
        }
        return contractsDAO.signContract(contractId, customerId);
    }
}
package com.electriccarsales.service;

import com.electriccarsales.dao.CustomerDAO;
import com.electriccarsales.model.Customer;

import java.util.List;
import java.util.regex.Pattern;

public class CustomerService {
    private static final Pattern PHONE = Pattern.compile("^0[0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private final CustomerDAO customerDAO = new CustomerDAO();

    public Customer getOwnProfile(int userId) {
        if (userId <= 0) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        return customerDAO.findByUserId(userId);
    }

    public Customer getCustomerById(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException("Mã khách hàng không hợp lệ.");
        return customerDAO.findById(customerId);
    }

    public List<Customer> getManagedCustomers(Integer employeeId) {
        return employeeId == null ? List.of() : customerDAO.findManaged(employeeId);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public boolean updateAvatar(int userId, byte[] image, String type) {
        return customerDAO.updateAvatar(userId, image, type);
    }

    public boolean updateOwnProfile(int userId, String name, String phone, String email, String address) {
        name = clean(name);
        phone = clean(phone);
        email = clean(email);
        address = clean(address);
        if (userId <= 0 || name.isEmpty() || name.length() > 100)
            throw new IllegalArgumentException("Họ tên không hợp lệ.");
        if (!PHONE.matcher(phone).matches())
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải bắt đầu bằng số 0 và gồm đúng 10 chữ số, ví dụ: 0912345678).");
        if (email.isEmpty() || email.length() > 100 || !EMAIL.matcher(email).matches())
            throw new IllegalArgumentException("Email không hợp lệ (ví dụ: example@domain.com).");
        if (address.length() > 255) throw new IllegalArgumentException("Địa chỉ vượt quá giới hạn.");
        return customerDAO.updateOwnedProfile(userId, name, phone, email, address);
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
}

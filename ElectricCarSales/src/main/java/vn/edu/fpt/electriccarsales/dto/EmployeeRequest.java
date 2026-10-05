package vn.edu.fpt.electriccarsales.dto;

public class EmployeeRequest {
    private String employeeName;
    private String phone;
    private String email;

    public EmployeeRequest() {
    }

    public EmployeeRequest(String employeeName, String phone, String email) {
        this.employeeName = employeeName;
        this.phone = phone;
        this.email = email;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

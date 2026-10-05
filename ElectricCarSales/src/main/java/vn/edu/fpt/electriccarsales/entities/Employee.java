package vn.edu.fpt.electriccarsales.entities;

public class Employee {
    private int employeeId;
    private int userId;
    private String employeeName;
    private String phone;
    private String email;
    private String username;
    private String role;
    private String status;

    public Employee() {
    }

    public Employee(int employeeId, int userId, String employeeName, String phone, String email, String username,
        String role, String status) {
        this.employeeId = employeeId;
        this.userId = userId;
        this.employeeName = employeeName;
        this.phone = phone;
        this.email = email;
        this.username = username;
        this.role = role;
        this.status = status;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

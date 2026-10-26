package com.electriccarsales.service;

import com.electriccarsales.dao.RoleDAO;
import com.electriccarsales.dao.StatusDAO;
import com.electriccarsales.dao.UserDAO;
import com.electriccarsales.model.Role;
import com.electriccarsales.model.Status;
import com.electriccarsales.model.User;

public class AuthenticationService {
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();
    private final StatusDAO statusDAO = new StatusDAO();

    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) return null;
        User user = userDAO.findByUsername(username.trim());
        if (user == null || !userDAO.authenticate(password, user)) return null;
        Status status = statusDAO.findById(user.getStatusID());
        Role role = roleDAO.findById(user.getRoleID());
        if (status == null || role == null || !"ACTIVE".equalsIgnoreCase(status.getStatusName())) return null;
        return user;
    }

    public String getRoleName(int roleId) {
        Role role = roleDAO.findById(roleId);
        return role == null ? null : role.getRoleName();
    }
}

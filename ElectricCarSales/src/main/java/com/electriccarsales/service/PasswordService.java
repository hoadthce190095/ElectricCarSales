package com.electriccarsales.service;

import com.electriccarsales.dao.UserDAO;
import com.electriccarsales.model.User;

public class PasswordService {
    private final UserDAO userDAO = new UserDAO();

    public String hash(String password) {
        return userDAO.hashPassword(password);
    }

    public boolean matches(String password, User user) {
        return userDAO.authenticate(password, user);
    }
}

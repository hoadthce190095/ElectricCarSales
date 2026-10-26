package com.electriccarsales.dao;

public class FinanceDAOException extends RuntimeException {
    public FinanceDAOException(String message, Throwable cause) { super(message, cause); }
    public FinanceDAOException(String message) { super(message); }
}

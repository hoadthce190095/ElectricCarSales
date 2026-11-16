package com.electriccarsales.dao;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaleSettlementTest {
    @Test
    void requiresExactCompletedAmountsForBothPhases() throws SQLException {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:settlement;MODE=MSSQLServer");
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE Orders (OrderID INT, TotalAmount DECIMAL(18,2),
                                         DepositAmount DECIMAL(18,2), RemainingAmount DECIMAL(18,2));
                    CREATE TABLE Payments (OrderID INT, PaymentAmount DECIMAL(18,2),
                                           PaymentStatusID INT, PaymentTypeID INT);
                    CREATE TABLE PaymentStatuses (PaymentStatusID INT, StatusID VARCHAR(20));
                    CREATE TABLE PaymentTypes (PaymentTypeID INT, TypeName VARCHAR(20));
                    INSERT INTO Orders VALUES (8, 1000, 300, 0);
                    INSERT INTO PaymentStatuses VALUES (1, 'COMPLETED'), (2, 'PENDING');
                    INSERT INTO PaymentTypes VALUES (1, 'DEPOSIT'), (2, 'REMAINING');
                    INSERT INTO Payments VALUES (8, 300, 1, 1), (8, 700, 2, 2);
                    """);

            assertThrows(IllegalStateException.class, () -> PaymentDAO.requireExactSettlement(connection, 8));
            statement.executeUpdate("UPDATE Payments SET PaymentStatusID = 1 WHERE PaymentTypeID = 2");
            assertDoesNotThrow(() -> PaymentDAO.requireExactSettlement(connection, 8));

            statement.executeUpdate("INSERT INTO Payments VALUES (8, 700, 1, 2)");
            assertThrows(IllegalStateException.class, () -> PaymentDAO.requireExactSettlement(connection, 8));
            statement.executeUpdate("DELETE FROM Payments WHERE PaymentTypeID = 2");
            statement.executeUpdate("INSERT INTO Payments VALUES (8, 700, 1, 1)");
            assertThrows(IllegalStateException.class, () -> PaymentDAO.requireExactSettlement(connection, 8));
        }
    }
}

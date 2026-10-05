package vn.edu.fpt.electriccarsales.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

class AuthorizationServiceTest {
    @Test
    void adminAlwaysRetainsFullPermissionsAndGrantsAreReadFreshForStaff() throws Exception {
        String url = "jdbc:h2:mem:auth_" + UUID.randomUUID() + ";MODE=MSSQLServer";
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE RolePermissions(RoleID INT, PermissionCode VARCHAR(50), PRIMARY"
                + " KEY(RoleID,PermissionCode))");
            assertEquals(List.of("MANAGE_USERS", "MANAGE_PROMOTIONS", "VIEW_REPORTS", "VIEW_LOGS"),
                AuthorizationService.permissions(connection, 90, "ADMIN"));
            assertTrue(AuthorizationService.permissions(connection, 17, "MANAGER").isEmpty());
            statement.execute("INSERT INTO RolePermissions VALUES(17,'MANAGE_USERS'),(17,'VIEW_REPORTS')");
            assertEquals(List.of("MANAGE_USERS", "VIEW_REPORTS"),
                AuthorizationService.permissions(connection, 17, "MANAGER"));
            statement.execute("DELETE FROM RolePermissions WHERE RoleID=17 AND" + " PermissionCode='MANAGE_USERS'");
            assertEquals(List.of("VIEW_REPORTS"), AuthorizationService.permissions(connection, 17, "MANAGER"));
            assertTrue(AuthorizationService.permissions(connection, 18, "CUSTOMER").isEmpty());
        }
    }
}

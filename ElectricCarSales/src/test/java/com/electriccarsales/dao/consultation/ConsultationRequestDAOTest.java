package com.electriccarsales.dao.consultation;

import com.electriccarsales.dao.SalesSchemaTestDatabase;
import com.electriccarsales.model.consultation.ConsultationRequest;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationRequestDAOTest {
    @Test
    void insertsAndReadsActualConsultationColumnsIncludingAssignedEmployeeAndModel() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            ConsultationRequestDAO dao = new ConsultationRequestDAO(database::open);
            ConsultationRequest input = newRequest();
            input.setStaffId(81);
            input.setModelId(91);
            input.setStatus(" completed ");
            input.setCreatedAt(Timestamp.valueOf("2026-10-26 11:00:00"));
            dao.insert(input);

            assertTrue(input.getId() > 0);
            ConsultationRequest actual = dao.findAll().getFirst();
            assertEquals(input.getId(), actual.getId());
            assertEquals(71, actual.getCustomerId());
            assertEquals(81, actual.getStaffId());
            assertEquals(91, actual.getModelId());
            assertEquals("COMPLETED", actual.getStatus());
            assertEquals(input.getContent(), actual.getContent());
            assertEquals(input.getCreatedAt(), actual.getCreatedAt());
        }
    }

    @Test
    void acceptsUnassignedRequestAndExistingServicePendingCasing() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            ConsultationRequestDAO dao = new ConsultationRequestDAO(database::open);
            ConsultationRequest input = newRequest();
            input.setStatus("Pending");
            dao.insert(input);

            ConsultationRequest actual = dao.findAll().getFirst();
            assertEquals(0, actual.getStaffId());
            assertNull(actual.getModelId());
            assertEquals("PENDING", actual.getStatus());
            assertNotNull(actual.getCreatedAt());
            try (var connection = database.open(); var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT EmployeeID,ModelID,ConsultationStatusID FROM ConsultationRequests")) {
                assertTrue(rows.next());
                assertNull(rows.getObject("EmployeeID"));
                assertNull(rows.getObject("ModelID"));
                assertEquals(63, rows.getInt("ConsultationStatusID"));
            }
        }
    }

    @Test
    void rejectsInvalidRequestContentBeforeWriting() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            ConsultationRequestDAO dao = new ConsultationRequestDAO(database::open);
            ConsultationRequest input = newRequest();
            input.setContent(" ");
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));
            input.setContent("x".repeat(1001));
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));
            assertTrue(dao.findAll().isEmpty());
        }
    }

    @Test
    void propagatesDatabaseErrorsRatherThanReturningEmptyOrReportingSuccess() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            ConsultationRequestDAO dao = new ConsultationRequestDAO(database::open);
            ConsultationRequest input = newRequest();
            input.setStatus("UNKNOWN");
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> dao.insert(input));
            assertInstanceOf(SQLException.class, failure.getCause());
            assertEquals(0, input.getId());
            assertTrue(dao.findAll().isEmpty());
        }
        ConsultationRequestDAO unavailable = new ConsultationRequestDAO(() -> {
            throw new SQLException("Database unavailable");
        });
        assertInstanceOf(SQLException.class,
            assertThrows(IllegalStateException.class, unavailable::findAll).getCause());
        assertInstanceOf(SQLException.class,
            assertThrows(IllegalStateException.class, () -> unavailable.insert(newRequest())).getCause());
    }

    private ConsultationRequest newRequest() {
        ConsultationRequest request = new ConsultationRequest();
        request.setCustomerId(71);
        request.setContent("Tư vấn xe điện cho gia đình 5 người");
        return request;
    }
}

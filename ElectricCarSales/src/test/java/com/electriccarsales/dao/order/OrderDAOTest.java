package com.electriccarsales.dao.order;

import com.electriccarsales.dao.SalesSchemaTestDatabase;
import com.electriccarsales.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class OrderDAOTest {
    @Test
    void insertsAndReadsActualOrderColumnsAndPreservesOriginalMoney() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            OrderDAO dao = new OrderDAO(database::open);
            Order input = newOrder();
            input.setEmployeeId(81);
            input.setStatus(" paid ");
            input.setDepositAmount(new BigDecimal("20000000.00"));
            input.setOrderDate(Timestamp.valueOf("2026-10-26 10:30:00"));
            input.setNote("Giao xe tại showroom");

            dao.insert(input);

            assertTrue(input.getId() > 0);
            Order actual = dao.findAll().getFirst();
            assertEquals(input.getId(), actual.getId());
            assertEquals(71, actual.getCustomerId());
            assertEquals(81, actual.getEmployeeId());
            assertEquals(51, actual.getPaymentMethodId());
            assertEquals("PAID", actual.getStatus());
            assertEquals(new BigDecimal("123456789.99"), actual.getOriginalAmount());
            assertEquals(new BigDecimal("20000000.00"), actual.getDepositAmount());
            assertEquals(new BigDecimal("103456789.99"), actual.getRemainingAmount());
            assertEquals(input.getOrderDate(), actual.getOrderDate());
            assertEquals(input.getNote(), actual.getNote());
        }
    }

    @Test
    void initializesOptionalOrderFieldsAndResolvesPendingStatusByName() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            OrderDAO dao = new OrderDAO(database::open);
            Order input = newOrder();
            dao.insert(input);

            Order actual = dao.findAll().getFirst();
            assertNull(actual.getEmployeeId());
            assertEquals("PENDING", actual.getStatus());
            assertEquals(new BigDecimal("0.00"), actual.getDepositAmount());
            assertEquals(input.getOriginalAmount(), actual.getRemainingAmount());
            assertNotNull(actual.getOrderDate());
        }
    }

    @Test
    void rejectsMissingPaymentMethodAndInconsistentBalanceBeforeWriting() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            OrderDAO dao = new OrderDAO(database::open);
            Order input = newOrder();
            input.setPaymentMethodId(0);
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));
            input.setPaymentMethodId(51);
            input.setDepositAmount(BigDecimal.TEN);
            input.setRemainingAmount(BigDecimal.ZERO);
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));
            assertTrue(dao.findAll().isEmpty());
        }
    }

    @Test
    void rejectsSubCentAmountsAndDecimalOverflowWithoutInserting() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            OrderDAO dao = new OrderDAO(database::open);
            Order input = newOrder();
            input.setOriginalAmount(new BigDecimal("0.01"));
            input.setDepositAmount(new BigDecimal("0.005"));
            input.setRemainingAmount(new BigDecimal("0.005"));
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));

            input.setOriginalAmount(new BigDecimal("0.011"));
            input.setDepositAmount(new BigDecimal("0.001"));
            input.setRemainingAmount(new BigDecimal("0.010"));
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));

            input.setOriginalAmount(new BigDecimal("10000000000000000.00"));
            input.setDepositAmount(BigDecimal.ZERO);
            input.setRemainingAmount(null);
            assertThrows(IllegalArgumentException.class, () -> dao.insert(input));
            assertEquals(0, input.getId());
            assertTrue(dao.findAll().isEmpty());
        }
    }

    @Test
    void acceptsExactCentAmountsWithTrailingZeroesAndPreservesStoredBalance() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            OrderDAO dao = new OrderDAO(database::open);
            Order input = newOrder();
            input.setOriginalAmount(new BigDecimal("0.010"));
            input.setDepositAmount(new BigDecimal("0.000"));
            input.setRemainingAmount(new BigDecimal("0.010"));
            dao.insert(input);

            Order actual = dao.findAll().getFirst();
            assertEquals(new BigDecimal("0.01"), actual.getOriginalAmount());
            assertEquals(new BigDecimal("0.00"), actual.getDepositAmount());
            assertEquals(new BigDecimal("0.01"), actual.getRemainingAmount());
            assertEquals(actual.getOriginalAmount(), actual.getDepositAmount().add(actual.getRemainingAmount()));
        }
    }

    @Test
    void propagatesDatabaseErrorsRatherThanReturningEmptyOrReportingSuccess() throws Exception {
        try (SalesSchemaTestDatabase database = new SalesSchemaTestDatabase()) {
            OrderDAO dao = new OrderDAO(database::open);
            Order input = newOrder();
            input.setCustomerId(9999);
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> dao.insert(input));
            assertInstanceOf(SQLException.class, failure.getCause());
            assertEquals(0, input.getId());
            assertTrue(dao.findAll().isEmpty());
        }
        OrderDAO unavailable = new OrderDAO(() -> {
            throw new SQLException("Database unavailable");
        });
        assertInstanceOf(SQLException.class,
            assertThrows(IllegalStateException.class, unavailable::findAll).getCause());
        assertInstanceOf(SQLException.class,
            assertThrows(IllegalStateException.class, () -> unavailable.insert(newOrder())).getCause());
    }

    private Order newOrder() {
        Order order = new Order();
        order.setCustomerId(71);
        order.setPaymentMethodId(51);
        order.setOriginalAmount(new BigDecimal("123456789.99"));
        return order;
    }
}

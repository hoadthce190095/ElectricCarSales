package com.electriccarsales.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.electriccarsales.model.VehicleModel;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

class AIDAOTest {
    @Test
    void onlyPublicCatalogueTablesAreNeededAndOnlySellableInventoryProvidesPrice() throws Exception {
        String url = "jdbc:h2:mem:ai_" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        // There are deliberately no account, customer, order or financial tables in this database.
        List<String> statements = List.of(
            "CREATE TABLE BrandStatuses(BrandStatusID INT PRIMARY KEY, StatusName" + " NVARCHAR(20))",
            "CREATE TABLE Brands(BrandID INT PRIMARY KEY, BrandName NVARCHAR(100)," + " BrandStatusID INT)",
            "CREATE TABLE CarLines(CarLineID INT PRIMARY KEY, BrandID INT, CarLineName" + " NVARCHAR(100))",
            "CREATE TABLE CarModels(ModelID INT PRIMARY KEY, CarLineID INT, ModelName"
                + " NVARCHAR(100), Description NVARCHAR(500))",
            "CREATE TABLE Specifications(ModelID INT PRIMARY KEY, Dimensions"
                + " NVARCHAR(100), BatteryCapacity DECIMAL(10,2), WheelType"
                + " NVARCHAR(100), EnergyConsumption DECIMAL(10,2), RangeKM"
                + " DECIMAL(10,2), MaxPower DECIMAL(10,2), MaxSpeed DECIMAL(10,2),"
                + " SeatingCapacity INT, ChargingTime DECIMAL(10,2), AirbagCount INT," + " Description NVARCHAR(500))",
            "CREATE TABLE CarStatuses(CarStatusID INT PRIMARY KEY, StatusName" + " NVARCHAR(30))",
            "CREATE TABLE CarVersions(VersionID INT PRIMARY KEY, ModelID INT," + " VersionName NVARCHAR(100))",
            "CREATE TABLE Colors(ColorID INT PRIMARY KEY, ColorName NVARCHAR(50))",
            "CREATE TABLE Cars(CarID INT PRIMARY KEY, ModelID INT, VersionID INT,"
                + " ColorID INT, CarStatusID INT, Price DECIMAL(18,2))",
            "INSERT INTO BrandStatuses VALUES(9,'ACTIVE'),(17,'INACTIVE')",
            "INSERT INTO Brands VALUES(1,'VinFast',9),(2,'Inactive Brand',17)",
            "INSERT INTO CarLines VALUES(1,1,'SUV'),(2,2,'SUV')",
            "INSERT INTO CarModels VALUES(1,1,'Available',NULL),(2,1,'Sold"
                + " Only',NULL),(3,2,'Inactive',NULL),(4,1,'Missing Specs',NULL)",
            "INSERT INTO Specifications(ModelID,RangeKM,SeatingCapacity)" + " VALUES(1,450,5),(2,500,7)",
            "INSERT INTO CarStatuses" + " VALUES(1,'AVAILABLE'),(2,'SOLD'),(3,'RESERVED'),(4,'Unknown'),(5,N'Có"
                + " sẵn')",
            "INSERT INTO CarVersions" + " VALUES(11,1,'Standard'),(12,1,'Plus'),(21,2,'Only')",
            "INSERT INTO Colors VALUES(1,N'Trắng'),(2,N'Đen'),(3,N'Đỏ')", "INSERT INTO Cars"
                + " VALUES(1,1,11,1,1,400000000),(2,1,12,2,5,500000000),(3,1,11,3,2,100000000),(4,1,11,3,3,200000000),(5,1,11,3,4,250000000),(6,2,21,3,2,300000000),(7,3,11,1,1,600000000)");
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
        AIDAO repository = new AIDAO(() -> DriverManager.getConnection(url));
        var models = repository.findModels();
        assertEquals(List.of(1, 2, 4), models.stream().map(VehicleModel::getModelId).toList());
        VehicleModel available = models.getFirst();
        assertEquals(2, available.getAvailableCars());
        assertEquals(0, new BigDecimal("400000000").compareTo(available.getMinPrice()));
        assertEquals(0, new BigDecimal("500000000").compareTo(available.getMaxPrice()));
        assertEquals(List.of("Standard", "Plus"), available.getVersions());
        assertEquals(List.of("Trắng", "Đen"), available.getColors());
        assertEquals(5, available.getSpecifications().getSeatingCapacity());
        VehicleModel sold = models.get(1);
        assertEquals(0, sold.getAvailableCars());
        assertNull(sold.getMinPrice());
        assertNull(sold.getMaxPrice());
        assertTrue(sold.getColors().isEmpty());
        assertNull(models.get(2).getSpecifications().getRangeKM());
        assertNull(models.get(2).getSpecifications().getSeatingCapacity());
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }
}

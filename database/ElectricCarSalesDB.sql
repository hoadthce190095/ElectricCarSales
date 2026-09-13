/* =========================================================
   ELECTRIC CAR SALES DATABASE
   ========================================================= */

USE master;
GO

/* Delete old database if it already exists */
IF DB_ID('ElectricCarSalesDB') IS NOT NULL
BEGIN
    ALTER DATABASE ElectricCarSalesDB
    SET SINGLE_USER
    WITH ROLLBACK IMMEDIATE;

    DROP DATABASE ElectricCarSalesDB;
END
GO


/* =========================================================
   1. CREATE DATABASE
   ========================================================= */

CREATE DATABASE ElectricCarSalesDB;
GO

USE ElectricCarSalesDB;
GO


/* =========================================================
   2. USERS
   ========================================================= */

CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,

    Username VARCHAR(50) NOT NULL UNIQUE,

    Password VARCHAR(255) NOT NULL,

    Role VARCHAR(20) NOT NULL,

    Status BIT NOT NULL DEFAULT 1
);
GO


/* =========================================================
   3. CUSTOMERS
   ========================================================= */

CREATE TABLE Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,

    UserID INT NULL,

    FullName NVARCHAR(100) NOT NULL,

    Phone VARCHAR(20) NOT NULL,

    Email VARCHAR(100),

    Address NVARCHAR(255),

    CreatedDate DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_Customers_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID)
);
GO


/* =========================================================
   4. EMPLOYEES
   ========================================================= */

CREATE TABLE Employees (
    EmployeeID INT IDENTITY(1,1) PRIMARY KEY,

    UserID INT NOT NULL,

    FullName NVARCHAR(100) NOT NULL,

    Phone VARCHAR(20),

    Position NVARCHAR(50),

    Status BIT NOT NULL DEFAULT 1,

    CONSTRAINT FK_Employees_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID)
);
GO


/* =========================================================
   5. BRANDS
   ========================================================= */

CREATE TABLE Brands (
    BrandID INT IDENTITY(1,1) PRIMARY KEY,

    BrandName NVARCHAR(100) NOT NULL,

    Country NVARCHAR(100)
);
GO


/* =========================================================
   6. CAR MODELS
   ========================================================= */

CREATE TABLE CarModels (
    ModelID INT IDENTITY(1,1) PRIMARY KEY,

    BrandID INT NOT NULL,

    ModelName NVARCHAR(100) NOT NULL,

    BatteryCapacity DECIMAL(10,2),

    RangeKM INT,

    Price DECIMAL(18,2) NOT NULL,

    Description NVARCHAR(500),

    CONSTRAINT FK_CarModels_Brands
        FOREIGN KEY (BrandID)
        REFERENCES Brands(BrandID)
);
GO


/* =========================================================
   7. CARS
   ========================================================= */

CREATE TABLE Cars (
    CarID INT IDENTITY(1,1) PRIMARY KEY,

    ModelID INT NOT NULL,

    VIN VARCHAR(50) NOT NULL UNIQUE,

    Color NVARCHAR(50),

    ManufacturingYear INT,

    CarStatus VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT FK_Cars_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID)
);
GO


/* =========================================================
   8. INVENTORY
   ========================================================= */

CREATE TABLE Inventory (
    InventoryID INT IDENTITY(1,1) PRIMARY KEY,

    CarID INT NOT NULL UNIQUE,

    Location NVARCHAR(100),

    InventoryStatus VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK',

    UpdatedDate DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_Inventory_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID)
);
GO


/* =========================================================
   9. ORDERS
   ========================================================= */

CREATE TABLE Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,

    CustomerID INT NOT NULL,

    EmployeeID INT NULL,

    OrderDate DATETIME NOT NULL DEFAULT GETDATE(),

    OrderStatus VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    TotalAmount DECIMAL(18,2) NOT NULL DEFAULT 0,

    Note NVARCHAR(500),

    CONSTRAINT FK_Orders_Customers
        FOREIGN KEY (CustomerID)
        REFERENCES Customers(CustomerID),

    CONSTRAINT FK_Orders_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID)
);
GO


/* =========================================================
   10. ORDER DETAILS
   ========================================================= */

CREATE TABLE OrderDetails (
    OrderDetailID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL,

    CarID INT NOT NULL,

    Quantity INT NOT NULL DEFAULT 1,

    UnitPrice DECIMAL(18,2) NOT NULL,

    SubTotal AS (Quantity * UnitPrice),

    CONSTRAINT CK_OrderDetails_Quantity
        CHECK (Quantity = 1),

    CONSTRAINT FK_OrderDetails_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_OrderDetails_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID)
);
GO


/* =========================================================
   11. PAYMENTS
   ========================================================= */

CREATE TABLE Payments (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL,

    PaymentDate DATETIME NOT NULL DEFAULT GETDATE(),

    Amount DECIMAL(18,2) NOT NULL,

    PaymentType VARCHAR(30) NOT NULL,

    PaymentMethod VARCHAR(30),

    PaymentStatus VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',

    TransactionCode VARCHAR(100),

    CONSTRAINT FK_Payments_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID)
);
GO

/* =========================================================
   SAMPLE DATA
   ========================================================= */


/* USERS */

INSERT INTO Users
(Username, Password, Role)
VALUES
('admin', '123', 'ADMIN'),
('sales01', '123', 'SALES'),
('warehouse01', '123', 'WAREHOUSE'),
('cskh01', '123', 'CSKH');
GO


/* EMPLOYEES */

INSERT INTO Employees
(UserID, FullName, Phone, Position)
VALUES
(1, N'Nguyễn Văn Admin', '0900000001', N'Admin'),
(2, N'Nguyễn Văn An', '0900000002', N'Sales'),
(3, N'Trần Văn Bình', '0900000003', N'Warehouse'),
(4, N'Lê Thị Chi', '0900000004', N'CSKH');
GO


/* CUSTOMERS */

INSERT INTO Customers
(FullName, Phone, Email, Address)
VALUES
(N'Đặng Ngọc Thanh',
 '0900000011',
 'hoa@example.com',
 N'Cần Thơ'),

(N'Nguyễn Văn Minh',
 '0900000012',
 'minh@example.com',
 N'Cần Thơ'),

(N'Trần Thị Lan',
 '0900000013',
 'lan@example.com',
 N'An Giang');
GO


/* BRANDS */

INSERT INTO Brands
(BrandName, Country)
VALUES
(N'VinFast', N'Vietnam'),
(N'Tesla', N'United States'),
(N'BYD', N'China'),
(N'Hyundai', N'South Korea');
GO


/* CAR MODELS */

INSERT INTO CarModels
(
    BrandID,
    ModelName,
    BatteryCapacity,
    RangeKM,
    Price,
    Description
)
VALUES

(
    1,
    N'VF 5',
    37.23,
    326,
    500000000,
    N'Electric city car'
),

(
    1,
    N'VF 6',
    59.60,
    399,
    675000000,
    N'Electric SUV'
),

(
    1,
    N'VF 8',
    87.70,
    471,
    1000000000,
    N'Electric SUV'
),

(
    2,
    N'Model 3',
    57.50,
    513,
    1200000000,
    N'Electric sedan'
),

(
    3,
    N'Atto 3',
    60.48,
    410,
    850000000,
    N'Electric SUV'
);
GO


/* CARS */

INSERT INTO Cars
(
    ModelID,
    VIN,
    Color,
    ManufacturingYear,
    CarStatus
)
VALUES

(1, 'VF5-001', N'White', 2026, 'AVAILABLE'),

(1, 'VF5-002', N'Black', 2026, 'AVAILABLE'),

(1, 'VF5-003', N'Red', 2026, 'AVAILABLE'),

(2, 'VF6-001', N'Blue', 2026, 'AVAILABLE'),

(2, 'VF6-002', N'White', 2026, 'AVAILABLE'),

(3, 'VF8-001', N'White', 2026, 'AVAILABLE'),

(3, 'VF8-002', N'Black', 2026, 'AVAILABLE'),

(4, 'TESLA3-001', N'Red', 2026, 'AVAILABLE'),

(5, 'BYDATTO3-001', N'Gray', 2026, 'AVAILABLE');
GO


/* INVENTORY */

INSERT INTO Inventory
(
    CarID,
    Location,
    InventoryStatus
)
VALUES

(1, N'Showroom Cần Thơ', 'IN_STOCK'),

(2, N'Showroom Cần Thơ', 'IN_STOCK'),

(3, N'Showroom Cần Thơ', 'IN_STOCK'),

(4, N'Showroom Cần Thơ', 'IN_STOCK'),

(5, N'Showroom Cần Thơ', 'IN_STOCK'),

(6, N'Kho TP.HCM', 'IN_STOCK'),

(7, N'Kho TP.HCM', 'IN_STOCK'),

(8, N'Kho TP.HCM', 'IN_STOCK'),

(9, N'Kho TP.HCM', 'IN_STOCK');
GO


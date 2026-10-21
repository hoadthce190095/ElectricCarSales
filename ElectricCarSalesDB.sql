-- Database chung cho tat ca thanh vien trong nhom.
-- Chay file nay trong SQL Server Management Studio.
-- Gom cau truc bang, danh muc VinFast va du lieu bao cao DEMO trong mot file.
-- Chi them du lieu con thieu; khong xoa database va khong tao tai khoan Admin.
-- Bo qua giao dich DEMO neu database da co giao dich khong phai DEMO.
USE master;
GO
IF DB_ID(N'ElectricCarSalesDB') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE ElectricCarSalesDB');
END;
GO
USE ElectricCarSalesDB;
GO
SET XACT_ABORT ON;
GO

/*
    ROLES vai tro
*/
IF OBJECT_ID(N'dbo.Roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Roles (
        RoleID INT IDENTITY(1, 1) PRIMARY KEY,
        RoleName VARCHAR(20) NOT NULL UNIQUE
    );
END;
GO
/*
    STATUSES trang thai users
*/
IF OBJECT_ID(N'dbo.Statuses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Statuses (
        StatusID INT IDENTITY(1, 1) PRIMARY KEY,
        StatusName VARCHAR(20) NOT NULL UNIQUE
    );
END;
GO

/* =========================================================
   1. USERS
   ========================================================= */

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserID INT IDENTITY(1, 1) PRIMARY KEY,
        Username VARCHAR(50) NOT NULL UNIQUE,
        PasswordHash VARCHAR(255) NOT NULL,
        Email VARCHAR(100) NULL UNIQUE,
        Phone VARCHAR(20) NULL UNIQUE,
        RoleID INT NOT NULL,
        StatusID INT NOT NULL,
        CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Users_Roles
            FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID),
        CONSTRAINT FK_Users_Statuses
            FOREIGN KEY (StatusID) REFERENCES dbo.Statuses(StatusID)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName = 'CUSTOMER')
BEGIN
    INSERT INTO dbo.Roles(RoleName)
    VALUES ('CUSTOMER');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName = 'SALES')
BEGIN
    INSERT INTO dbo.Roles(RoleName)
    VALUES ('SALES');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName = 'OPERATIONS')
BEGIN
    INSERT INTO dbo.Roles(RoleName)
    VALUES ('OPERATIONS');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName = 'ACCOUNTANT')
BEGIN
    INSERT INTO dbo.Roles(RoleName)
    VALUES ('ACCOUNTANT');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName = 'MANAGER')
BEGIN
    INSERT INTO dbo.Roles(RoleName)
    VALUES ('MANAGER');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleName = 'ADMIN')
BEGIN
    INSERT INTO dbo.Roles(RoleName)
    VALUES ('ADMIN');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Statuses WHERE StatusName = 'ACTIVE')
BEGIN
    INSERT INTO dbo.Statuses(StatusName)
    VALUES ('ACTIVE');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Statuses WHERE StatusName = 'INACTIVE')
BEGIN
    INSERT INTO dbo.Statuses(StatusName)
    VALUES ('INACTIVE');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Statuses WHERE StatusName = 'LOCKED')
BEGIN
    INSERT INTO dbo.Statuses(StatusName)
    VALUES ('LOCKED');
END;
GO

/*
    STATUSES trang thai Brand
*/
IF OBJECT_ID(N'dbo.BrandStatuses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BrandStatuses (
        BrandStatusID INT IDENTITY(1, 1) PRIMARY KEY,
        StatusName VARCHAR(20) NOT NULL UNIQUE
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.BrandStatuses WHERE StatusName = 'ACTIVE')
BEGIN
    INSERT INTO dbo.BrandStatuses(StatusName)
    VALUES ('ACTIVE');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.BrandStatuses WHERE StatusName = 'INACTIVE')
BEGIN
    INSERT INTO dbo.BrandStatuses(StatusName)
    VALUES ('INACTIVE');
END;
GO
/* =========================================================
  5. BRANDS
 ========================================================= */

IF OBJECT_ID(N'dbo.Brands', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Brands (
        BrandID INT IDENTITY(1, 1) PRIMARY KEY,
        BrandName NVARCHAR(100) NOT NULL UNIQUE,
        Description NVARCHAR(500) NULL,
        BrandStatusID INT NOT NULL,
        CONSTRAINT FK_Brands_BrandStatuses
            FOREIGN KEY (BrandStatusID)
            REFERENCES dbo.BrandStatuses(BrandStatusID)
    );
END;
GO
/*=========================================================
  CarLines dòng xe
 ========================================================= */
IF OBJECT_ID(N'dbo.CarLines', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CarLines (
        CarLineID INT IDENTITY(1, 1) PRIMARY KEY,
        BrandID INT NOT NULL,
        CarLineName NVARCHAR(100) NOT NULL,
        Description NVARCHAR(500) NULL,
        CONSTRAINT FK_CarLines_Brands
            FOREIGN KEY (BrandID)
            REFERENCES dbo.Brands(BrandID),
        CONSTRAINT UQ_CarLines_Brand_Name
            UNIQUE (BrandID, CarLineName)
    );
END;
GO

/*=========================================================
  CarModels
 ========================================================= */
IF OBJECT_ID(N'dbo.CarModels', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CarModels (
        ModelID INT IDENTITY(1, 1) PRIMARY KEY,
        CarLineID INT NOT NULL,
        ModelName NVARCHAR(100) NOT NULL,
        Description NVARCHAR(500) NULL,
        CONSTRAINT FK_CarModels_CarLines
            FOREIGN KEY (CarLineID)
            REFERENCES dbo.CarLines(CarLineID),
        CONSTRAINT UQ_CarModels_CarLine_Name
            UNIQUE (CarLineID, ModelName)
    );
END;
GO

/*=========================================================
  Specifications thông số
 ========================================================= */

IF OBJECT_ID(N'dbo.Specifications', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Specifications (
        SpecificationID INT IDENTITY(1, 1) PRIMARY KEY,
        ModelID INT NOT NULL,
        Dimensions NVARCHAR(100) NULL,
        BatteryCapacity DECIMAL(10, 2) NULL,
        WheelType NVARCHAR(100) NULL,
        EnergyConsumption DECIMAL(10, 2) NULL,
        RangeKM DECIMAL(10, 2) NULL,
        MaxPower DECIMAL(10, 2) NULL,
        MaxSpeed DECIMAL(10, 2) NULL,
        SeatingCapacity INT NULL,
        ChargingTime DECIMAL(10, 2) NULL,
        AirbagCount INT NULL,
        Description NVARCHAR(500) NULL,
        CONSTRAINT FK_Specifications_CarModels
            FOREIGN KEY (ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT UQ_Specifications_CarModels
            UNIQUE (ModelID)
    );
END;
GO
 /*=========================================================
  ComparisonSessions SO SANH XE
 ========================================================= */
IF OBJECT_ID(N'dbo.ComparisonSessions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ComparisonSessions (
        ComparisonSessionID INT IDENTITY(1, 1) PRIMARY KEY,
        UserID INT NOT NULL,
        CreateDate DATETIME2  NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ComparisonSessions_Users
            FOREIGN KEY (UserID)
            REFERENCES dbo.Users(UserID)
    );
END;
GO

 /*=========================================================
 ComparisonItems LUU CAC DONG XE
 ========================================================= */
IF OBJECT_ID(N'dbo.ComparisonItems', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ComparisonItems (
        ComparisonItemID INT IDENTITY(1, 1) PRIMARY KEY,
        ComparisonSessionID INT NOT NULL,
        ModelID INT NOT NULL,
        CONSTRAINT FK_ComparisonItems_Sessions
            FOREIGN KEY (ComparisonSessionID)
            REFERENCES dbo.ComparisonSessions(ComparisonSessionID),
        CONSTRAINT FK_ComparisonItems_CarModels
            FOREIGN KEY (ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT UQ_ComparisonItems_Session_Model
            UNIQUE (ComparisonSessionID, ModelID)
    );
END;
GO

 /*
    COLORS DANH SACH MAU
*/
IF OBJECT_ID(N'dbo.Colors', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Colors (
        ColorID INT IDENTITY(1, 1) PRIMARY KEY,
        ColorName NVARCHAR(50) NOT NULL UNIQUE,
        Description NVARCHAR(200) NULL
    );
END;
GO

 /*
    MODELCOLORS LUA CHON MAU
*/
IF OBJECT_ID(N'dbo.ModelColors', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ModelColors (
        ModelColorsID INT IDENTITY(1, 1) PRIMARY KEY,
        ModelID INT NOT NULL,
        ColorID INT NOT NULL,
        ColorImageURL NVARCHAR(500) NULL,
        CONSTRAINT FK_ModelColors_CarModels
            FOREIGN KEY (ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT FK_ModelColors_Colors
            FOREIGN KEY (ColorID)
            REFERENCES dbo.Colors(ColorID),
        CONSTRAINT UQ_ModelColors_Model_Color
            UNIQUE (ModelID, ColorID)
    );
END;
GO

IF COL_LENGTH(N'dbo.ModelColors', N'ColorImageURL') IS NULL
    ALTER TABLE dbo.ModelColors ADD ColorImageURL NVARCHAR(500) NULL;
GO

 /*
    CARVERSIONS PHIEN BAN XE
*/
IF OBJECT_ID(N'dbo.Carversions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Carversions (
        VersionID INT IDENTITY(1, 1) PRIMARY KEY,
        ModelID INT NOT NULL,
        VersionName NVARCHAR(100) NOT NULL,
        Description NVARCHAR(500) NULL,
        CatalogPrice DECIMAL(18, 2) NULL,
        RangeStandard VARCHAR(20) NULL,
        SourceURL NVARCHAR(500) NULL,
        PriceSourceURL NVARCHAR(500) NULL,
        RapidChargeMinutes INT NULL,
        PriceNote NVARCHAR(500) NULL,
        CONSTRAINT FK_Carversions_CarModels
            FOREIGN KEY(ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT UQ_Carversions_Model_Name
            UNIQUE (ModelID, VersionName)
    );
END;
GO

-- Bo sung cot danh muc cho database da tao truoc khi cap nhat trang chu.
IF COL_LENGTH(N'dbo.Carversions', N'CatalogPrice') IS NULL
    ALTER TABLE dbo.Carversions ADD CatalogPrice DECIMAL(18, 2) NULL;
IF COL_LENGTH(N'dbo.Carversions', N'RangeStandard') IS NULL
    ALTER TABLE dbo.Carversions ADD RangeStandard VARCHAR(20) NULL;
IF COL_LENGTH(N'dbo.Carversions', N'SourceURL') IS NULL
    ALTER TABLE dbo.Carversions ADD SourceURL NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Carversions', N'PriceSourceURL') IS NULL
    ALTER TABLE dbo.Carversions ADD PriceSourceURL NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Carversions', N'RapidChargeMinutes') IS NULL
    ALTER TABLE dbo.Carversions ADD RapidChargeMinutes INT NULL;
IF COL_LENGTH(N'dbo.Carversions', N'PriceNote') IS NULL
    ALTER TABLE dbo.Carversions ADD PriceNote NVARCHAR(500) NULL;
GO

 /*
    CARSTATUS TRANG THAI XE
*/
IF OBJECT_ID(N'dbo.CarStatuses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CarStatuses (
        CarStatusID INT IDENTITY(1, 1) PRIMARY KEY,
        StatusName NVARCHAR(30) NOT NULL UNIQUE,
        Description NVARCHAR(200) NULL
    );
END;
GO

/*
    CARS
*/
IF OBJECT_ID(N'dbo.Cars', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Cars (
        CarID INT IDENTITY(1, 1) PRIMARY KEY,
        CarCode VARCHAR(50) NOT NULL UNIQUE,
        ModelID INT NOT NULL,
        VersionID INT NOT NULL,
        ColorID INT NOT NULL,
        CarStatusID INT NOT NULL,
        Origin NVARCHAR(100) NULL,
        Year INT NOT NULL,
        VIN VARCHAR(17) NOT NULL UNIQUE,
        EngineNumber VARCHAR(50) NOT NULL UNIQUE,
        LotCode VARCHAR(50) NULL,
        Price DECIMAL(18, 2) NOT NULL,
        CONSTRAINT FK_Cars_CarModels
            FOREIGN KEY (ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT FK_Cars_CarVersions
            FOREIGN KEY(VersionID)
            REFERENCES dbo.Carversions(VersionID),
        CONSTRAINT FK_Cars_Colors
            FOREIGN KEY (ColorID)
            REFERENCES dbo.Colors(ColorID),
        CONSTRAINT FK_Cars_CarStatuses
            FOREIGN KEY (CarStatusID)
            REFERENCES dbo.CarStatuses(CarStatusID)
    );
END;
GO

/*
  CUSTOMERS
  */
IF OBJECT_ID(N'dbo.Customers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Customers (
        CustomerID INT IDENTITY(1, 1) PRIMARY KEY,
        UserID INT NOT NULL UNIQUE,
        CustomerName NVARCHAR(100) NOT NULL,
        Phone VARCHAR(20) NOT NULL UNIQUE,
        Email VARCHAR(100) NULL,
        Address NVARCHAR(255) NULL,
        CONSTRAINT FK_Customers_Users
            FOREIGN KEY (UserID)
            REFERENCES dbo.Users(UserID)
    );
END;
GO

/* =========================================================
   3. EMPLOYEES
   ========================================================= */
IF OBJECT_ID(N'dbo.Employees', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Employees (
        EmployeeID INT IDENTITY(1, 1) PRIMARY KEY,
        UserID INT NOT NULL UNIQUE,
        EmployeeName NVARCHAR(100) NOT NULL,
        Phone VARCHAR(20) NOT NULL,
        Email VARCHAR(100) NULL,
        CONSTRAINT FK_Employees_Users
            FOREIGN KEY (UserID)
            REFERENCES dbo.Users(UserID)
    );
END;
GO
/*
  ConsultationStatus   trang thai tu van
  */
IF OBJECT_ID(N'dbo.ConsultationStatuses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ConsultationStatuses (
        ConsultationStatusID INT IDENTITY(1, 1) PRIMARY KEY,
        StatusName VARCHAR(30) NOT NULL UNIQUE
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.ConsultationStatuses WHERE StatusName = 'PENDING')
BEGIN
    INSERT INTO dbo.ConsultationStatuses(StatusName)
    VALUES ('PENDING');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.ConsultationStatuses WHERE StatusName = 'IN_PROGRESS')
BEGIN
    INSERT INTO dbo.ConsultationStatuses(StatusName)
    VALUES ('IN_PROGRESS');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.ConsultationStatuses WHERE StatusName = 'COMPLETED')
BEGIN
    INSERT INTO dbo.ConsultationStatuses(StatusName)
    VALUES ('COMPLETED');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.ConsultationStatuses WHERE StatusName = 'CANCELLED')
BEGIN
    INSERT INTO dbo.ConsultationStatuses(StatusName)
    VALUES ('CANCELLED');
END;

GO

/*
  ConsultationRequests  yeu cau tu van
  */
IF OBJECT_ID(N'dbo.ConsultationRequests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ConsultationRequests (
        ConsultationRequestID INT IDENTITY(1, 1) PRIMARY KEY,
        CustomerID INT NOT NULL,
        EmployeeID INT NULL,
        ModelID INT NULL,
        ConsultationStatusID INT NOT NULL,
        RequestContent NVARCHAR(1000) NOT NULL,
        RequestDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ConsultationRequests_Customers
            FOREIGN KEY (CustomerID)
            REFERENCES dbo.Customers(CustomerID),
        CONSTRAINT FK_ConsultationRequests_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT FK_ConsultationRequests_CarModels
            FOREIGN KEY (ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT FK_ConsultationRequests_Statuses
            FOREIGN KEY (ConsultationStatusID)
            REFERENCES dbo.ConsultationStatuses(ConsultationStatusID)
    );
END;
GO

/*
  ORDERSTATUS  trang thai don hang
  */
IF OBJECT_ID(N'dbo.OrderStatuses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderStatuses (
        OrderStatusID INT IDENTITY(1, 1) PRIMARY KEY,
        StatusName VARCHAR(30) NOT NULL UNIQUE,
        Description NVARCHAR(200) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'PENDING')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('PENDING', 'New order created');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'RESERVED')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('RESERVED', 'Vehicle has been reserved');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'DEPOSITED')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('DEPOSITED', 'Deposit has been paid');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'PROCESSING')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('PROCESSING', 'Order is being processed');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'PAID')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('PAID', 'Full payment has been completed');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'COMPLETED')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('COMPLETED', 'Order has been completed');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.OrderStatuses WHERE StatusName = 'CANCELLED')
BEGIN
    INSERT INTO dbo.OrderStatuses(StatusName, Description)
    VALUES ('CANCELLED', 'Order has been cancelled');
END;

GO

/*
  PAYMENTMETHODS
  */
IF OBJECT_ID(N'dbo.PaymentMethods', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PaymentMethods (
        PaymentMethodID INT IDENTITY(1, 1) PRIMARY KEY,
        MethodName VARCHAR(30) NOT NULL UNIQUE,
        Description NVARCHAR(200) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentMethods WHERE MethodName = 'CASH')
BEGIN
    INSERT INTO dbo.PaymentMethods(MethodName, Description)
    VALUES ('CASH', 'Cash payment');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentMethods WHERE MethodName = 'BANK_TRANSFER')
BEGIN
    INSERT INTO dbo.PaymentMethods(MethodName, Description)
    VALUES ('BANK_TRANSFER', 'Bank tranfer payment');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentMethods WHERE MethodName = 'INSTALLMENT')
BEGIN
    INSERT INTO dbo.PaymentMethods(MethodName, Description)
    VALUES ('INSTALLMENT', 'Installment payment');
END;
GO

/*
  ORDERS
  */
IF OBJECT_ID(N'dbo.Orders', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Orders (
        OrderID INT IDENTITY(1, 1) PRIMARY KEY,
        CustomerID INT NOT NULL,
        EmployeeID INT NULL,
        OrderStatusID INT NOT NULL,
        PaymentMethodID INT NOT NULL,
        OrderDate DATETIME2 NOT NULL,
        TotalAmount DECIMAL(18, 2) NOT NULL,
        DepositAmount DECIMAL(18, 2) NOT NULL,
        RemainingAmount DECIMAL(18, 2) NOT NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_Orders_Customers
            FOREIGN KEY(CustomerID)
            REFERENCES dbo.Customers(CustomerID),
        CONSTRAINT FK_Orders_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT FK_Orders_OrderStatuses
            FOREIGN KEY(OrderStatusID)
            REFERENCES dbo.OrderStatuses(OrderStatusID),
        CONSTRAINT FK_Orders_PaymentMethods
            FOREIGN KEY (PaymentMethodID)
            REFERENCES dbo.PaymentMethods(PaymentMethodID)
    );
END;
GO

/*
  ORDERS DETAILS
  */
IF OBJECT_ID(N'dbo.OrderDetails', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderDetails (
        OrderDetailID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        CarID INT NOT NULL UNIQUE,
        SalesPrice DECIMAL(18, 2) NOT NULL,
        CONSTRAINT FK_OrderDetails_Orders
            FOREIGN KEY (OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_OrderDetails_Cars
            FOREIGN KEY (CarID)
            REFERENCES dbo.Cars(CarID)
    );
END;
GO

/*
  PAYMENT TYPE
  */
IF OBJECT_ID(N'dbo.PaymentTypes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PaymentTypes (
        PaymentTypeID INT IDENTITY(1, 1) PRIMARY KEY,
        TypeName VARCHAR(30) NOT NULL UNIQUE,
        Description NVARCHAR(200) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentTypes WHERE TypeName = 'DEPOSIT')
BEGIN
    INSERT INTO dbo.PaymentTypes(TypeName, Description)
    VALUES ('DEPOSIT', 'Vehicle deposit payment');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentTypes WHERE TypeName = 'REMAINING')
BEGIN
    INSERT INTO dbo.PaymentTypes(TypeName, Description)
    VALUES ('REMAINING', 'Remaining amount payment');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentTypes WHERE TypeName = 'INSTALLMENT')
BEGIN
    INSERT INTO dbo.PaymentTypes(TypeName, Description)
    VALUES ('INSTALLMENT', 'Installment payment');
END;
GO

/*
  PAYMENT STATUS
  */
IF OBJECT_ID(N'dbo.PaymentStatuses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PaymentStatuses (
        PaymentStatusID INT IDENTITY(1, 1) PRIMARY KEY,
        StatusID VARCHAR(30) NOT NULL UNIQUE,
        Description NVARCHAR(200) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentStatuses WHERE StatusID = 'PENDING')
BEGIN
    INSERT INTO dbo.PaymentStatuses(StatusID, Description)
    VALUES ('PENDING' , 'Payment is pending');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentStatuses WHERE StatusID = 'COMPLETED')
BEGIN
    INSERT INTO dbo.PaymentStatuses(StatusID, Description)
    VALUES ('COMPLETED', 'Payment has been completed');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentStatuses WHERE StatusID = 'FAILD')
BEGIN
    INSERT INTO dbo.PaymentStatuses(StatusID, Description)
    VALUES ('FAILD', 'Payment has failed');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.PaymentStatuses WHERE StatusID = 'CANCELLED')
BEGIN
    INSERT INTO dbo.PaymentStatuses(StatusID, Description)
    VALUES ('CANCELLED', 'Payment has been cancelled');
END;
GO

/*
  PAYMENTS
  */
IF OBJECT_ID(N'dbo.Payments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Payments (
        PaymentID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        PaymentMethodID INT NOT NULL,
        PaymentTypeID INT NOT NULL,
        PaymentStatusID INT NOT NULL,
        PaymentAmount DECIMAL(18, 2) NOT NULL,
        PaymentDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        ReferenceCode VARCHAR(100) NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_Payments_Orders
            FOREIGN KEY(OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_Payments_PaymentMethods
            FOREIGN KEY (PaymentMethodID)
            REFERENCES dbo.PaymentMethods(PaymentMethodID),
        CONSTRAINT FK_Payments_PaymentTypes
            FOREIGN KEY (PaymentTypeID)
            REFERENCES dbo.PaymentTypes(PaymentTypeID),
        CONSTRAINT FK_Payments_PaymentStatuses
            FOREIGN KEY (PaymentStatusID)
            REFERENCES dbo.PaymentStatuses(PaymentStatusID)
    );
END;
GO

/*
  InstallmentPlans
  */
IF OBJECT_ID(N'dbo.InstallmentPlans', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InstallmentPlans (
        PlanID  INT IDENTITY(1, 1) PRIMARY KEY,
        PlanName NVARCHAR(100) NOT NULL UNIQUE,
        LoanTermMonths INT NOT NULL,
        InterestRate DECIMAL(5, 2) NOT NULL,
        Description NVARCHAR(500) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.InstallmentPlans WHERE PlanName = '12 Months')
BEGIN
    INSERT INTO dbo.InstallmentPlans(PlanName, LoanTermMonths, InterestRate, Description)
    VALUES ('12 Months', 12, 0.00, '12-month installment plan');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.InstallmentPlans WHERE PlanName = '24 Months')
BEGIN
    INSERT INTO dbo.InstallmentPlans(PlanName, LoanTermMonths, InterestRate, Description)
    VALUES ('24 Months', 24, 0.00, '24-month installment plan');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.InstallmentPlans WHERE PlanName = '36 Months')
BEGIN
    INSERT INTO dbo.InstallmentPlans(PlanName, LoanTermMonths, InterestRate, Description)
    VALUES ('36 Months', 36, 0.00, '36-month installment plan');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.InstallmentPlans WHERE PlanName = '48 Months')
BEGIN
    INSERT INTO dbo.InstallmentPlans(PlanName, LoanTermMonths, InterestRate, Description)
    VALUES ('48 Months', 48, 0.00, '48-month installment plan');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.InstallmentPlans WHERE PlanName = '60 Months')
BEGIN
    INSERT INTO dbo.InstallmentPlans(PlanName, LoanTermMonths, InterestRate, Description)
    VALUES ('60 Months', 60, 0.00, '60-month installment plan');
END;
GO

/*
  FinancialCompanies
  */
IF OBJECT_ID(N'dbo.FinancialCompanies', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.FinancialCompanies (
        CompanyID INT IDENTITY(1, 1) PRIMARY KEY,
        CompanyName NVARCHAR(150) NOT NULL UNIQUE,
        Phone VARCHAR(20) NULL,
        Email VARCHAR(100) NULL,
        Address NVARCHAR(255) NULL,
        Description NVARCHAR(500) NULL
    );
END;
GO

/*
  InstallmentContracts
  */
IF OBJECT_ID(N'dbo.InstallmentContracts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InstallmentContracts (
        InstallmentContractID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        CompanyID INT NOT NULL,
        PlanID INT NOT NULL,
        VehiclePrice DECIMAL(18, 2) NOT NULL,
        DownPayment DECIMAL(18, 2) NOT NULL,
        LoanAmount DECIMAL(18, 2) NOT NULL,
        LoanTermMonths INT NOT NULL,
        InterestRate DECIMAL(5, 2) NOT NULL,
        TotalInterest DECIMAL(18, 2) NOT NULL,
        TotalInstallmentAmount DECIMAL(18, 2) NOT NULL,
        PeriodicPayment DECIMAL(18, 2) NOT NULL,
        ContractDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Status VARCHAR(30) NOT NULL,
        CONSTRAINT FK_InstallmentContracts_Orders
            FOREIGN KEY (OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_InstallmentContracts_FinancialCompanies
            FOREIGN KEY (CompanyID)
            REFERENCES dbo.FinancialCompanies(CompanyID),
        CONSTRAINT FK_InstallmentContracts_InstallmentPlans
            FOREIGN KEY (PlanID)
            REFERENCES dbo.InstallmentPlans(PlanID),
        CONSTRAINT UQ_InstallmentContracts_Order
            UNIQUE (OrderID)
    );
END;
GO

/*
  InstallmentSchedules
  */
IF OBJECT_ID(N'dbo.InstallmentSchedules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InstallmentSchedules (
        ScheduleID INT IDENTITY(1, 1) PRIMARY KEY,
        InstallmentContractID INT NOT NULL,
        PeriodNumber INT NOT NULL,
        DueDate DATE NOT NULL,
        PrincipalAmount DECIMAL(18, 2) NOT NULL,
        InterestAmount DECIMAL(18, 2) NOT NULL,
        PaymentAmount DECIMAL(18, 2) NOT NULL,
        PaidAmount DECIMAL(18, 2) NOT NULL DEFAULT 0,
        Status VARCHAR(30) NOT NULL,
        CONSTRAINT FK_InstallmentSchedules_InstallmentContracts
            FOREIGN KEY (InstallmentContractID)
            REFERENCES dbo.InstallmentContracts(InstallmentContractID),
        CONSTRAINT UQ_InstallmentSchedules_Contract_Period
            UNIQUE (InstallmentContractID, PeriodNumber)
    );
END;
GO

/*
  InstallmentPayments
  */
IF OBJECT_ID(N'dbo.InstallmentPayments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InstallmentPayments (
        InstallmentPaymentID INT IDENTITY(1, 1) PRIMARY KEY,
        ScheduleID INT NOT NULL,
        PaymentAmount DECIMAL(18, 2) NOT NULL,
        PaymentDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        PaymentMethodID INT NOT NULL,
        PaymentStatusID INT NOT NULL,
        ReferenceCode VARCHAR(100) NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_InstallmentPayments_Schedules
            FOREIGN KEY (ScheduleID)
            REFERENCES dbo.InstallmentSchedules(ScheduleID),
        CONSTRAINT FK_InstallmentPayments_PaymentMethods
            FOREIGN KEY (PaymentMethodID)
            REFERENCES dbo.PaymentMethods(PaymentMethodID),
        CONSTRAINT FK_InstallmentPayments_PaymentStatuses
            FOREIGN KEY (PaymentStatusID)
            REFERENCES dbo.PaymentStatuses(PaymentStatusID)
    );
END;
GO

/*
  InstallmentPayments
  */
IF OBJECT_ID(N'dbo.InstallmentSettlements', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InstallmentSettlements (
        SettlementID INT IDENTITY(1, 1) PRIMARY KEY,
        InstallmentContractID INT NOT NULL,
        SettlementDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        PrincipalRemaining DECIMAL(18, 2) NOT NULL,
        AccruedInterest DECIMAL(18, 2) NOT NULL,
        SettlementFee DECIMAL(18, 2) NOT NULL,
        SettlementAmount DECIMAL(18, 2) NOT NULL,
        PaymentMethodID INT NOT NULL,
        EmployeeID INT NOT NULL,
        ReferenceCode VARCHAR(100) NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_InstallmentSettlements_Contracts
            FOREIGN KEY (InstallmentContractID)
            REFERENCES dbo.InstallmentContracts(InstallmentContractID),
        CONSTRAINT FK_InstallmentSettlements_PaymentMethods
            FOREIGN KEY (PaymentMethodID)
            REFERENCES dbo.PaymentMethods(PaymentMethodID),
        CONSTRAINT FK_InstallmentSettlements_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT UQ_InstallmentSettlements_Contract
            UNIQUE (InstallmentContractID)
    );
END;
GO

/*
  Invoices
  */
IF OBJECT_ID(N'dbo.Invoices', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Invoices (
        InvoiceID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        CustomerID INT NOT NULL,
        EmployeeID INT NOT NULL,
        InvoiceNumber VARCHAR(50) NOT NULL UNIQUE,
        InvoiceDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        SubTotal DECIMAL(18, 2) NOT NULL,
        DiscountAmount DECIMAL(18, 2) NOT NULL DEFAULT 0,
        TaxAmount DECIMAL(18, 2) NOT NULL DEFAULT 0,
        TotalAmount DECIMAL(18, 2) NOT NULL,
        Status VARCHAR(30) NOT NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_Invoices_Orders
            FOREIGN KEY (OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_Invoices_Customers
            FOREIGN KEY (CustomerID)
            REFERENCES dbo.Customers(CustomerID),
        CONSTRAINT FK_Invoices_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT UQ_Invoices_Order
            UNIQUE (OrderID)
    );
END;
GO

/*
  Contracts
  */
IF OBJECT_ID(N'dbo.Contracts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Contracts (
        ContractID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        CustomerID INT NOT NULL,
        EmployeeID INT NOT NULL,
        ContractNumber VARCHAR(50) NOT NULL UNIQUE,
        ContractDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        ContractAmount DECIMAL(18, 2) NOT NULL,
        Status VARCHAR(30) NOT NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_Contracts_Orders
            FOREIGN KEY (OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_Contracts_Customers
            FOREIGN KEY (CustomerID)
            REFERENCES dbo.Customers(CustomerID),
        CONSTRAINT FK_Contracts_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT UQ_Contracts_Order
            UNIQUE (OrderID)
    );
END;
GO

/*
  Contracts
  */
IF OBJECT_ID(N'dbo.Deliveries', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Deliveries (
        DeliveryID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        CustomerID INT NOT NULL,
        EmployeeID INT NOT NULL,
        CarID INT NOT NULL,
        DeliveryDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        DeliveryAddress NVARCHAR(255) NOT NULL,
        DeliveryStatus VARCHAR(30) NOT NULL,
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_Deliveries_Orders
            FOREIGN KEY (OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_Deliveries_Customers
            FOREIGN KEY (CustomerID)
            REFERENCES dbo.Customers(CustomerID),
        CONSTRAINT FK_Deliveries_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT FK_Deliveries_Cars
            FOREIGN KEY (CarID)
            REFERENCES dbo.Cars(CarID),
        CONSTRAINT UQ_Deliveries_Order
            UNIQUE (OrderID)
    );
END;
GO
/*
  OTPVerificationsacts
  */
IF OBJECT_ID(N'dbo.OTPVerifications', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OTPVerifications (
        OTPVerificationID INT IDENTITY(1, 1) PRIMARY KEY,
        UserID INT NOT NULL,
        OTPCode VARCHAR(10) NOT NULL,
        Purpose VARCHAR(50) NOT NULL,
        CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        ExpiredDate DATETIME2 NOT NULL,
        VerifiedDate DATETIME2 NULL,
        Status VARCHAR(30) NOT NULL,
        CONSTRAINT FK_OTPVerifications_Users
            FOREIGN KEY (UserID)
            REFERENCES dbo.Users(UserID)
    );
END;
GO

/*
  Suppliers
  */
IF OBJECT_ID(N'dbo.Suppliers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Suppliers (
        SupplierID INT IDENTITY(1, 1) PRIMARY KEY,
        SupplierName NVARCHAR(150) NOT NULL UNIQUE,
        Phone VARCHAR(20) NULL,
        Email VARCHAR(100) NULL,
        Address NVARCHAR(255) NULL,
        Description NVARCHAR(500) NULL
    );
END;
GO

/*
  InventoryTransactions
  */
IF OBJECT_ID(N'dbo.InventoryTransactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InventoryTransactions (
        InventoryTransactionID INT IDENTITY(1, 1) PRIMARY KEY,
        CarID INT NOT NULL,
        EmployeeID INT NOT NULL,
        SupplierID INT NULL,
        TransactionType VARCHAR(30) NOT NULL,
        TransactionDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_InventoryTransactions_Cars
            FOREIGN KEY (CarID)
            REFERENCES dbo.Cars(CarID),
        CONSTRAINT FK_InventoryTransactions_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID),
        CONSTRAINT FK_InventoryTransactions_Suppliers
            FOREIGN KEY (SupplierID)
            REFERENCES dbo.Suppliers(SupplierID)
    );
END;
GO

/*
  CarImages
  */
IF OBJECT_ID(N'dbo.CarImages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CarImages (
        CarImageID INT IDENTITY(1, 1) PRIMARY KEY,
        ModelID INT NOT NULL,
        ImageURL NVARCHAR(500) NOT NULL,
        ImageType VARCHAR(30) NULL,
        IsPrimary BIT NULL,
        DisplayOrder INT NULL,
        CreateDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_CarImages_CarModels
            FOREIGN KEY(ModelID)
            REFERENCES dbo.CarModels(ModelID)
    );
END;
GO
/*
  Promotions
  */
IF OBJECT_ID(N'dbo.Promotions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Promotions (
        PromotionID INT IDENTITY(1, 1) PRIMARY KEY,
        PromotionName NVARCHAR(150) NOT NULL,
        Description NVARCHAR(500) NULL,
        DiscountType VARCHAR(30) NULL,
        DiscountValue DECIMAL(18, 2) NULL,
        StartDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        EndDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Status VARCHAR(30)
    );
END;
GO

/*
  PromotionCars
  */
IF OBJECT_ID(N'dbo.PromotionCars', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PromotionCars (
        PromotionCarID INT IDENTITY(1, 1) PRIMARY KEY,
        PromotionID INT NOT NULL,
        ModelID INT NOT NULL,
        CONSTRAINT FK_PromotionCars_Promotions
            FOREIGN KEY(PromotionID)
            REFERENCES dbo.Promotions(PromotionID),
        CONSTRAINT FK_PromotionCars_CarModels
            FOREIGN KEY(ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT UQ_PromotionCars_Promotion_Model
            UNIQUE (PromotionID, ModelID)
    );
END;
GO


/*
  VehicleTCO
  */
IF OBJECT_ID(N'dbo.VehicleTCO', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.VehicleTCO (
        TCOID INT IDENTITY(1, 1) PRIMARY KEY,
        ModelID INT NOT NULL,
        VehiclePrice DECIMAL(18, 2) NULL,
        EnergyCost DECIMAL(18, 2) NULL,
        MaintenanceCost DECIMAL(18, 2) NULL,
        InsuranceCost DECIMAL(18, 2) NULL,
        RegistrationCost DECIMAL(18, 2) NULL,
        IncentiveAmount DECIMAL(18, 2) NULL,
        TotalCost DECIMAL(18, 2) NULL,
        PeriodMonths INT NULL,
        Description NVARCHAR(500) NULL,
        CreateDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_VehicleTCO_CarModels
            FOREIGN KEY (ModelID)
            REFERENCES dbo.CarModels(ModelID)
    );
END;
GO
/*
  OrderStatusHistory
  */
IF OBJECT_ID(N'dbo.OrderStatusHistory', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderStatusHistory (
        OrderStatusHistoryID INT IDENTITY(1, 1) PRIMARY KEY,
        OrderID INT NOT NULL,
        OrderStatusID INT NOT NULL,
        EmployeeID INT NULL,
        ChangedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Note NVARCHAR(500) NULL,
        CONSTRAINT FK_OrderStatusHistory_Orders
            FOREIGN KEY (OrderID)
            REFERENCES dbo.Orders(OrderID),
        CONSTRAINT FK_OrderStatusHistory_OrderStatuses
            FOREIGN KEY (OrderStatusID)
            REFERENCES dbo.OrderStatuses(OrderStatusID),
        CONSTRAINT FK_OrderStatusHistory_Employees
            FOREIGN KEY (EmployeeID)
            REFERENCES dbo.Employees(EmployeeID)
    );
END;
GO

-- Quan tri, phan quyen va nhat ky cua Pham Tran Phuc Tan.
IF OBJECT_ID(N'dbo.SystemLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemLogs (
        LogID BIGINT IDENTITY(1, 1) PRIMARY KEY,
        UserID INT NULL,
        Action VARCHAR(50) NOT NULL,
        TargetType VARCHAR(50) NOT NULL,
        TargetID VARCHAR(100) NULL,
        Detail NVARCHAR(MAX) NOT NULL,
        CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_SystemLogs_Users
            FOREIGN KEY(UserID) REFERENCES dbo.Users(UserID)
    );
    CREATE INDEX IX_SystemLogs_CreatedDate ON dbo.SystemLogs(CreatedDate DESC, LogID DESC);
    CREATE INDEX IX_SystemLogs_UserID ON dbo.SystemLogs(UserID, CreatedDate DESC);
END;

IF OBJECT_ID(N'dbo.RolePermissions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RolePermissions (
        RoleID INT NOT NULL,
        PermissionCode VARCHAR(50) NOT NULL,
        CONSTRAINT PK_RolePermissions
            PRIMARY KEY(RoleID, PermissionCode),
        CONSTRAINT FK_RolePermissions_Roles
            FOREIGN KEY(RoleID) REFERENCES dbo.Roles(RoleID),
        CONSTRAINT CK_RolePermissions_Code
            CHECK(PermissionCode IN ('MANAGE_USERS', 'MANAGE_PROMOTIONS', 'VIEW_REPORTS', 'VIEW_LOGS'))
    );
    INSERT INTO dbo.RolePermissions(RoleID, PermissionCode)
        SELECT RoleID, 'VIEW_REPORTS' FROM dbo.Roles WHERE RoleName IN ('MANAGER', 'ACCOUNTANT');
END;
IF OBJECT_ID(N'dbo.SystemConfigurations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemConfigurations (
        ConfigKey VARCHAR(50) PRIMARY KEY,
        ConfigValue VARCHAR(100) NOT NULL,
        UpdatedBy INT NULL REFERENCES dbo.Users(UserID),
        UpdatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT CK_SystemConfigurations_Key
            CHECK(ConfigKey IN ('SESSION_TIMEOUT_MINUTES', 'REPORT_MAX_DAYS'))
    );
    INSERT INTO dbo.SystemConfigurations(ConfigKey, ConfigValue) VALUES('SESSION_TIMEOUT_MINUTES', '30'), ('REPORT_MAX_DAYS', '3660');
END;
GO

-- Application logs are append-only, including SQL writes outside the UI.
CREATE OR ALTER TRIGGER dbo.TR_SystemLogs_Immutable
ON dbo.SystemLogs
INSTEAD OF UPDATE, DELETE
AS
BEGIN
    THROW 51001, N'System logs are immutable.', 1;
END;
GO

-- BEGIN DEMO DATA
-- 1. Danh muc xe VinFast va 30 xe DEMO.
-- Danh mục VinFast dùng cho bài báo cáo Phạm Trần Phúc Tấn.
-- Kiểm tra nguồn chính thức ngày 05/10/2026, chi tiết ở docs/NGUON_DU_LIEU_VINFAST.md.
-- Danh mục và xe mô phỏng được nạp sau phần tạo bảng trong cùng file này.
-- Script chỉ thêm dữ liệu còn thiếu, không sửa/xóa danh mục hoặc giao dịch sẵn có.
-- Xe có mã DEMO/VIN giả lập phục vụ học tập, không phải tồn kho của VinFast.
USE ElectricCarSalesDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.CarModels', N'U') IS NULL OR OBJECT_ID(N'dbo.Cars', N'U') IS NULL
    THROW 51000, N'Chưa có schema xe. Chạy ElectricCarSalesDB.sql trước.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM BrandStatuses WHERE StatusName = 'ACTIVE')
        INSERT INTO BrandStatuses(StatusName) VALUES ('ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM Brands WHERE BrandName = N'VinFast')
        INSERT INTO Brands(BrandName, Description, BrandStatusID)
        SELECT N'VinFast', N'Danh mục xe điện tham khảo từ nguồn chính thức VinFast.', BrandStatusID
        FROM BrandStatuses WHERE StatusName = 'ACTIVE';

    IF NOT EXISTS (SELECT 1 FROM CarStatuses WHERE StatusName = N'AVAILABLE')
        INSERT INTO CarStatuses(StatusName, Description)
        VALUES (N'AVAILABLE', N'Xe sẵn sàng tại showroom');

    DECLARE @BrandID INT = (SELECT BrandID FROM Brands WHERE BrandName = N'VinFast');
    DECLARE @AvailableID INT = (SELECT CarStatusID FROM CarStatuses WHERE StatusName = N'AVAILABLE');

    -- Mỗi bản Eco/Plus là một CarModel để bảng Specifications không bị trộn thông số.
    DECLARE @Catalogue TABLE (
        DemoNumber INT PRIMARY KEY,
        ModelName NVARCHAR(100),
        CarLineName NVARCHAR(100),
        VersionName NVARCHAR(100),
        DemoCode VARCHAR(30),
        Price DECIMAL(18, 2),
        Dimensions NVARCHAR(100),
        BatteryCapacity DECIMAL(10, 2),
        RangeKM DECIMAL(10, 2),
        MaxPower DECIMAL(10, 2),
        SeatingCapacity INT,
        WheelType NVARCHAR(100),
        AirbagCount INT,
        MaxSpeed DECIMAL(10, 2),
        SpecificationDescription NVARCHAR(500),
        ImageURL NVARCHAR(500),
        DemoColor1 NVARCHAR(50),
        DemoColor2 NVARCHAR(50),
        DemoColor3 NVARCHAR(50)
    );

    INSERT INTO @Catalogue VALUES
    (
        1, N'VF 3', N'VF 3',
        N'Eco - Tiêu chuẩn 2', N'VF3', 285000000,
        N'3190 x 1679 x 1652 mm', 18.64, 215,
        30, 4, N'16 inch',
        1, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf3; kiểm tra 05/10/2026. Range 215 km (NEDC). Sạc nhanh 10-70%: 36 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/xe-dien-vinfast-vf3-chon-mau-nen-trang-logo-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đỏ (Solar Ruby)'
    ),
    (
        2, N'VF 5 Plus', N'VF 5',
        N'Plus - Tiêu chuẩn 1', N'VF5PLUS', 496000000,
        N'3967 x 1723 x 1579 mm', 37.23, 326,
        100, 5, N'17 inch',
        6, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf5; kiểm tra 05/10/2026. Range 326 km (NEDC). Sạc nhanh 10-70%: 33 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/xe-dien-vinfast-vf5-ngoai-that-mau-xam-toan-canh-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đỏ (Solar Ruby)'
    ),
    (
        3, N'VF 6 Eco', N'VF 6',
        N'Eco - Tiêu chuẩn 1', N'VF6ECO', 646000000,
        N'4241 x 1834 x 1580 mm', 59.6, 485,
        130, 5, NULL,
        4, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf6; kiểm tra 05/10/2026. Range 485 km (NEDC). Sạc nhanh 10-70%: 25 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-logo-vf6-mau-do-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đen (Jet Black)'
    ),
    (
        4, N'VF 6 Plus', N'VF 6',
        N'Plus - Tiêu chuẩn 2', N'VF6PLUS', 699000000,
        N'4241 x 1834 x 1580 mm', 59.6, 460,
        150, 5, N'18 inch',
        NULL, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf6; kiểm tra 05/10/2026. Range 460 km (NEDC). Sạc nhanh 10-70%: 25 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-logo-vf6-mau-xanh-reu-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đen (Jet Black)'
    ),
    (
        5, N'VF 7 Eco', N'VF 7',
        N'Eco - Tiêu chuẩn 2', N'VF7ECO', 740000000,
        N'4545 x 1890 x 1635.75 mm', 59.6, 440,
        130, 5, N'19 inch',
        4, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf7; kiểm tra 05/10/2026. Range 440 km (NEDC). Sạc nhanh 10-70%: 25 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-text-logo-banner-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đen (Jet Black)'
    ),
    (
        6, N'VF 7 Plus', N'VF 7',
        N'Plus FWD - Trần thép - Tiêu chuẩn 2', N'VF7PLUS', 830000000,
        N'4545 x 1890 x 1635.75 mm', 70, 500.5,
        150, 5, N'20 inch',
        NULL, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf7; kiểm tra 05/10/2026. Range 500.5 km (NEDC). Sạc nhanh 10-70%: 28 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-plus-text-logo-banner-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đen (Jet Black)'
    ),
    (
        7, N'VF 8 Eco', N'VF 8',
        N'Eco FWD - Pin CATL VN - Tiêu chuẩn 1', N'VF8ECO', 898000000,
        N'4750 x 1934 x 1667 mm', 87.7, 635,
        150, 5, N'19 inch',
        11, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-vf8; kiểm tra 05/10/2026. Range 635 km (NEDC). Sạc nhanh 10-70%: 31 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf8/hinh-anh-vinfast-vf8-eco-text-logo-banner-desktop.webp', N'Trắng (Infinity Blanc)', N'Đen (Jet Black)',
        N'Đỏ (Crimson Red)'
    ),
    (
        8, N'VF 8 Plus', N'VF 8',
        N'Plus AWD - Pin CATL VN - Tiêu chuẩn 1', N'VF8PLUS', 1079000000,
        N'4750 x 1934 x 1667 mm', 87.7, 475,
        300, 5, N'20 inch',
        11, NULL, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-vf8; kiểm tra 05/10/2026. Range 475 km (NEDC). Sạc nhanh 10-70%: 31 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-xanh-reu-02.webp', N'Trắng (Infinity Blanc)', N'Đen (Jet Black)',
        N'Đỏ (Crimson Red)'
    ),
    (
        9, N'VF 9 Eco', N'VF 9',
        N'Eco - Pin CATL - Điều hòa 3 vùng', N'VF9ECO', 1348000000,
        N'5119 x 2254 x 1697 mm', 123, 626,
        300, 7, N'20 inch',
        11, 200, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-vf9; kiểm tra 05/10/2026. Range 626 km (WLTP). Sạc nhanh 10-70%: 35 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-eco-text-logo-banner-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đen (Jet Black)'
    ),
    (
        10, N'VF 9 Plus', N'VF 9',
        N'Plus - Trần thép - 7 chỗ - Pin CATL', N'VF9PLUS', 1529000000,
        N'5119 x 2254 x 1697 mm', 123, 602,
        300, 7, N'21 inch',
        11, 200, N'Nguồn: https://vinfastauto.com/vn_vi/dat-coc-xe-vf9; kiểm tra 05/10/2026. Range 602 km (WLTP). Sạc nhanh 10-70%: 35 phút; không phải thời gian sạc đầy.',
        N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-product-summary-desktop.webp', N'Trắng (Infinity Blanc)', N'Xám (Zenith Grey)',
        N'Đen (Jet Black)'
    );

    INSERT INTO CarLines(BrandID, CarLineName, Description)
    SELECT DISTINCT @BrandID, c.CarLineName, N'Dòng xe điện VinFast'
    FROM @Catalogue c
    WHERE NOT EXISTS (
        SELECT 1 FROM CarLines l WHERE l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    );

    INSERT INTO CarModels(CarLineID, ModelName, Description)
    SELECT l.CarLineID, c.ModelName, N'VinFast; cấu hình tham khảo: ' + c.VersionName
    FROM @Catalogue c
    INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    WHERE NOT EXISTS (
        SELECT 1 FROM CarModels m WHERE m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
    );

    INSERT INTO Carversions(ModelID, VersionName, Description)
    SELECT m.ModelID, c.VersionName, N'Giá MSRP kèm pin và VAT, chính sách 15/07/2026. Trang bị tùy cấu hình.'
    FROM @Catalogue c
    INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    INNER JOIN CarModels m ON m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
    WHERE NOT EXISTS (
        SELECT 1 FROM Carversions v WHERE v.ModelID = m.ModelID AND v.VersionName = c.VersionName
    );

    -- Chi dien metadata con thieu tu danh muc tham khao da co; giu gia va nguon da duoc cap nhat.
    DECLARE @CatalogueMetadata TABLE (
        ModelName NVARCHAR(100),
        VersionName NVARCHAR(100),
        RangeStandard VARCHAR(20),
        SourceURL NVARCHAR(500),
        PriceSourceURL NVARCHAR(500),
        RapidChargeMinutes INT,
        PriceNote NVARCHAR(500)
    );
    INSERT INTO @CatalogueMetadata VALUES
    (N'VF 3', N'Eco - Tiêu chuẩn 2', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf3', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 36, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Eco - Tiêu chuẩn 2. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 5 Plus', N'Plus - Tiêu chuẩn 1', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf5', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 33, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Plus - Tiêu chuẩn 1. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 6 Eco', N'Eco - Tiêu chuẩn 1', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf6', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 25, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Eco - Tiêu chuẩn 1. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 6 Plus', N'Plus - Tiêu chuẩn 2', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf6', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 25, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Plus - Tiêu chuẩn 2. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 7 Eco', N'Eco - Tiêu chuẩn 2', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf7', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 25, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Eco - Tiêu chuẩn 2. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 7 Plus', N'Plus FWD - Trần thép - Tiêu chuẩn 2', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf7', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 28, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Plus FWD - Trần thép - Tiêu chuẩn 2. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 8 Eco', N'Eco FWD - Pin CATL VN - Tiêu chuẩn 1', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf8', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 31, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Eco FWD - Pin CATL VN - Tiêu chuẩn 1. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 8 Plus', N'Plus AWD - Pin CATL VN - Tiêu chuẩn 1', N'NEDC', N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf8', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 31, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Plus AWD - Pin CATL VN - Tiêu chuẩn 1. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 9 Eco', N'Eco - Pin CATL - Điều hòa 3 vùng', N'WLTP', N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf9', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 35, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Eco - Pin CATL - Điều hòa 3 vùng. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.'),
    (N'VF 9 Plus', N'Plus - Trần thép - 7 chỗ - Pin CATL', N'WLTP', N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf9', N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf', 35, N'MSRP kèm pin, gồm VAT, chính sách 15/07/2026; cấu hình Plus - Trần thép - 7 chỗ - Pin CATL. Chưa tính phí lăn bánh, bộ sạc tại nhà, màu nâng cao hoặc ưu đãi riêng.');

    UPDATE v
    SET CatalogPrice = COALESCE(v.CatalogPrice, c.Price),
        RangeStandard = COALESCE(v.RangeStandard, metadata.RangeStandard),
        SourceURL = COALESCE(v.SourceURL, metadata.SourceURL),
        PriceSourceURL = COALESCE(v.PriceSourceURL, metadata.PriceSourceURL),
        RapidChargeMinutes = COALESCE(v.RapidChargeMinutes, metadata.RapidChargeMinutes),
        PriceNote = COALESCE(v.PriceNote, metadata.PriceNote)
    FROM Carversions v
    INNER JOIN CarModels m ON m.ModelID = v.ModelID
    INNER JOIN CarLines l ON l.CarLineID = m.CarLineID AND l.BrandID = @BrandID
    INNER JOIN @Catalogue c ON c.ModelName = m.ModelName AND c.CarLineName = l.CarLineName
        AND c.VersionName = v.VersionName
    INNER JOIN @CatalogueMetadata metadata ON metadata.ModelName = c.ModelName
        AND metadata.VersionName = c.VersionName
    WHERE v.CatalogPrice IS NULL OR v.RangeStandard IS NULL OR v.SourceURL IS NULL
        OR v.PriceSourceURL IS NULL OR v.RapidChargeMinutes IS NULL OR v.PriceNote IS NULL;

    -- Không suy ra mức tiêu thụ điện hoặc thời gian sạc đầy từ số liệu sạc nhanh.
    INSERT INTO Specifications(ModelID, Dimensions, BatteryCapacity, WheelType,
        EnergyConsumption, RangeKM, MaxPower, MaxSpeed, SeatingCapacity, ChargingTime,
        AirbagCount, Description)
    SELECT m.ModelID, c.Dimensions, c.BatteryCapacity, c.WheelType,
        NULL, c.RangeKM, c.MaxPower, c.MaxSpeed, c.SeatingCapacity, NULL,
        c.AirbagCount, c.SpecificationDescription
    FROM @Catalogue c
    INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    INNER JOIN CarModels m ON m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
    WHERE NOT EXISTS (SELECT 1 FROM Specifications s WHERE s.ModelID = m.ModelID);

    DECLARE @ModelColors TABLE (
        ModelName NVARCHAR(100),
        ColorName NVARCHAR(50)
    );
    INSERT INTO @ModelColors VALUES
    (N'VF 3', N'Trắng (Infinity Blanc)'),
    (N'VF 3', N'Xám (Zenith Grey)'),
    (N'VF 3', N'Đỏ (Solar Ruby)'),
    (N'VF 3', N'Vàng (Summer Yellow)'),
    (N'VF 3', N'Xanh dương (Sky Blue)'),
    (N'VF 3', N'Hồng (Rose Pink)'),
    (N'VF 3', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 5 Plus', N'Trắng (Infinity Blanc)'),
    (N'VF 5 Plus', N'Xám (Zenith Grey)'),
    (N'VF 5 Plus', N'Đỏ (Solar Ruby)'),
    (N'VF 5 Plus', N'Vàng (Summer Yellow) + nóc đen'),
    (N'VF 5 Plus', N'Xanh dương (Sky Blue) + nóc trắng'),
    (N'VF 5 Plus', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 6 Eco', N'Trắng (Infinity Blanc)'),
    (N'VF 6 Eco', N'Xám (Zenith Grey)'),
    (N'VF 6 Eco', N'Đen (Jet Black)'),
    (N'VF 6 Eco', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 6 Eco', N'Đỏ (Solar Ruby)'),
    (N'VF 6 Plus', N'Trắng (Infinity Blanc)'),
    (N'VF 6 Plus', N'Xám (Zenith Grey)'),
    (N'VF 6 Plus', N'Đen (Jet Black)'),
    (N'VF 6 Plus', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 6 Plus', N'Đỏ (Solar Ruby)'),
    (N'VF 7 Eco', N'Trắng (Infinity Blanc)'),
    (N'VF 7 Eco', N'Xám (Zenith Grey)'),
    (N'VF 7 Eco', N'Đen (Jet Black)'),
    (N'VF 7 Eco', N'Đỏ (Solar Ruby)'),
    (N'VF 7 Eco', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 7 Plus', N'Trắng (Infinity Blanc)'),
    (N'VF 7 Plus', N'Xám (Zenith Grey)'),
    (N'VF 7 Plus', N'Đen (Jet Black)'),
    (N'VF 7 Plus', N'Đỏ (Solar Ruby)'),
    (N'VF 7 Plus', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 8 Eco', N'Trắng (Infinity Blanc)'),
    (N'VF 8 Eco', N'Đen (Jet Black)'),
    (N'VF 8 Eco', N'Đỏ (Crimson Red)'),
    (N'VF 8 Eco', N'Xanh lá đậm (Ivy Green)'),
    (N'VF 8 Plus', N'Trắng (Infinity Blanc)'),
    (N'VF 8 Plus', N'Đen (Jet Black)'),
    (N'VF 8 Plus', N'Đỏ (Crimson Red)'),
    (N'VF 8 Plus', N'Xanh lá đậm (Ivy Green)'),
    (N'VF 9 Eco', N'Trắng (Infinity Blanc)'),
    (N'VF 9 Eco', N'Xám (Zenith Grey)'),
    (N'VF 9 Eco', N'Đen (Jet Black)'),
    (N'VF 9 Eco', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 9 Eco', N'Đỏ (Solar Ruby)'),
    (N'VF 9 Eco', N'Xanh lá đậm (Ivy Green)'),
    (N'VF 9 Eco', N'Bạc (Desat Silver)'),
    (N'VF 9 Plus', N'Trắng (Infinity Blanc)'),
    (N'VF 9 Plus', N'Xám (Zenith Grey)'),
    (N'VF 9 Plus', N'Đen (Jet Black)'),
    (N'VF 9 Plus', N'Xanh lá nhạt (Urban Mint)'),
    (N'VF 9 Plus', N'Đỏ (Solar Ruby)'),
    (N'VF 9 Plus', N'Xanh lá đậm (Ivy Green)'),
    (N'VF 9 Plus', N'Bạc (Desat Silver)');

    INSERT INTO Colors(ColorName, Description)
    SELECT DISTINCT c.ColorName, N'Màu công bố trên trang sản phẩm VinFast; màu nâng cao có phụ phí riêng.'
    FROM @ModelColors c
    WHERE NOT EXISTS (SELECT 1 FROM Colors old WHERE old.ColorName = c.ColorName);

    INSERT INTO ModelColors(ModelID, ColorID)
    SELECT m.ModelID, color.ColorID
    FROM @ModelColors selected
    INNER JOIN @Catalogue c ON c.ModelName = selected.ModelName
    INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    INNER JOIN CarModels m ON m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
    INNER JOIN Colors color ON color.ColorName = selected.ColorName
    WHERE NOT EXISTS (
        SELECT 1 FROM ModelColors old WHERE old.ModelID = m.ModelID AND old.ColorID = color.ColorID
    );

    INSERT INTO CarImages(ModelID, ImageURL, ImageType, IsPrimary, DisplayOrder)
    SELECT m.ModelID, c.ImageURL, 'VINFAST_OFFICIAL',
        CASE WHEN EXISTS (SELECT 1 FROM CarImages old WHERE old.ModelID = m.ModelID AND old.IsPrimary = 1)
            THEN 0 ELSE 1 END, 1
    FROM @Catalogue c
    INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    INNER JOIN CarModels m ON m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
    WHERE NOT EXISTS (
        SELECT 1 FROM CarImages old WHERE old.ModelID = m.ModelID AND old.ImageURL = c.ImageURL
    );

    -- Tạo 3 xe giả lập mỗi mẫu. Chỉ dùng màu tiêu chuẩn để giữ giá MSRP bên trên.
    DECLARE @Number INT = 1;
    WHILE @Number <= 3
    BEGIN
        INSERT INTO Cars(CarCode, ModelID, VersionID, ColorID, CarStatusID,
            Origin, Year, VIN, EngineNumber, LotCode, Price)
        SELECT 'DEMO-' + c.DemoCode + '-' + RIGHT('000' + CAST(@Number AS VARCHAR(3)), 3),
            m.ModelID, v.VersionID, color.ColorID, @AvailableID,
            N'Việt Nam - xe giả lập', 2026,
            'DEMO' + RIGHT('0000000000000' + CAST(
                (c.DemoNumber - 1) * 3 + @Number AS VARCHAR(13)), 13),
            'DEMO-ENGINE-' + c.DemoCode + '-' + CAST(@Number AS VARCHAR(3)),
            'DEMO-VINFAST-20261005', c.Price
        FROM @Catalogue c
        INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
        INNER JOIN CarModels m ON m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
        INNER JOIN Carversions v ON v.ModelID = m.ModelID AND v.VersionName = c.VersionName
        INNER JOIN Colors color ON color.ColorName = CASE @Number
            WHEN 1 THEN c.DemoColor1 WHEN 2 THEN c.DemoColor2 ELSE c.DemoColor3 END
        WHERE NOT EXISTS (
            SELECT 1 FROM Cars old
            WHERE old.CarCode = 'DEMO-' + c.DemoCode + '-' + RIGHT('000' + CAST(@Number AS VARCHAR(3)), 3)
        );
        SET @Number = @Number + 1;
    END;

    COMMIT TRANSACTION;
    PRINT N'Đã thêm danh mục VinFast và xe DEMO còn thiếu. Không thay đổi dữ liệu sẵn có.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- 2. Don hang, thanh toan, tra gop va khuyen mai DEMO.
/*
 * Du lieu MO PHONG cho bao cao cua Pham Tran Phuc Tan.
 * Khong phai giao dich, khach hang, nhan vien hay khuyen mai that cua VinFast.
 * Nap sau phan danh muc VinFast o tren, trong cung ElectricCarSalesDB.
 * Chi them ban ghi DEMO con thieu va doi trang thai xe DEMO khi tao don moi.
 * Khong tao Admin. Tai khoan DEMO INACTIVE va khong co mat khau dang nhap hop le.
 */
-- Bo qua phan nay khi database nhom da co giao dich khong phai DEMO.
USE ElectricCarSalesDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

-- Giu du lieu kinh doanh da co; chi nap giao dich mau vao database bai tap.
IF EXISTS (SELECT 1 FROM Orders WHERE Note IS NULL OR Note NOT LIKE N'DEMO-REPORT-%')
   OR EXISTS (SELECT 1 FROM Payments WHERE ReferenceCode IS NULL OR ReferenceCode NOT LIKE 'DEMO-PAY-%')
   OR EXISTS (
       SELECT 1 FROM InstallmentContracts ic JOIN Orders o ON o.OrderID = ic.OrderID
       WHERE o.Note IS NULL OR o.Note NOT LIKE N'DEMO-REPORT-%'
   )
BEGIN
    PRINT N'Da co giao dich khong phai DEMO. Giu nguyen du lieu va bo qua phan bao cao mo phong.';
    RETURN;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @LockResult INT;
    EXEC @LockResult = sys.sp_getapplock @Resource = 'DEMO-REPORT-SEED',
        @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 10000;
    IF @LockResult < 0
        THROW 51021, N'Khong khoa duoc qua trinh nap du lieu DEMO.', 1;

    DECLARE @Today DATE = CAST(GETDATE() AS DATE);
    DECLARE @MonthStart DATE = DATEFROMPARTS(YEAR(@Today), MONTH(@Today), 1);
    DECLARE @Inactive INT = (SELECT StatusID FROM Statuses WHERE StatusName = 'INACTIVE');
    DECLARE @CustomerRole INT = (SELECT RoleID FROM Roles WHERE RoleName = 'CUSTOMER');
    DECLARE @SalesRole INT = (SELECT RoleID FROM Roles WHERE RoleName = 'SALES');
    DECLARE @AccountantRole INT = (SELECT RoleID FROM Roles WHERE RoleName = 'ACCOUNTANT');
    DECLARE @BankTransfer INT = (SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName = 'BANK_TRANSFER');
    DECLARE @InstallmentMethod INT = (SELECT PaymentMethodID FROM PaymentMethods WHERE MethodName = 'INSTALLMENT');
    DECLARE @DepositType INT = (SELECT PaymentTypeID FROM PaymentTypes WHERE TypeName = 'DEPOSIT');
    DECLARE @RemainingType INT = (SELECT PaymentTypeID FROM PaymentTypes WHERE TypeName = 'REMAINING');
    DECLARE @PaymentCompleted INT = (SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID = 'COMPLETED');
    DECLARE @PaymentPending INT = (SELECT PaymentStatusID FROM PaymentStatuses WHERE StatusID = 'PENDING');
    IF @Inactive IS NULL OR @CustomerRole IS NULL OR @SalesRole IS NULL OR @AccountantRole IS NULL
       OR @BankTransfer IS NULL OR @InstallmentMethod IS NULL OR @DepositType IS NULL
       OR @RemainingType IS NULL OR @PaymentCompleted IS NULL OR @PaymentPending IS NULL
        THROW 51022, N'Thieu vai tro, trang thai hoac danh muc thanh toan. Hay chay ElectricCarSalesDB.sql truoc.', 1;

    IF NOT EXISTS (SELECT 1 FROM CarStatuses WHERE StatusName = 'SOLD')
        INSERT INTO CarStatuses(StatusName, Description) VALUES ('SOLD', N'Xe da ban');
    IF NOT EXISTS (SELECT 1 FROM CarStatuses WHERE StatusName = 'RESERVED')
        INSERT INTO CarStatuses(StatusName, Description) VALUES ('RESERVED', N'Xe da duoc giu cho khach hang');
    DECLARE @Sold INT = (SELECT CarStatusID FROM CarStatuses WHERE StatusName = 'SOLD');
    DECLARE @Reserved INT = (SELECT CarStatusID FROM CarStatuses WHERE StatusName = 'RESERVED');

    -- Ten dang nhap co tien to rieng; example.invalid khong phai dia chi email that.
    DECLARE @Index INT = 1, @Suffix VARCHAR(2), @Username VARCHAR(50), @UserID INT;
    WHILE @Index <= 6
    BEGIN
        SET @Suffix = RIGHT('00' + CAST(@Index AS VARCHAR(2)), 2);
        SET @Username = 'demo_customer_' + @Suffix;
        IF EXISTS (SELECT 1 FROM Users WHERE Username = @Username AND
            (PasswordHash <> 'DEMO_ACCOUNT_DISABLED' OR StatusID <> @Inactive OR RoleID <> @CustomerRole))
            THROW 51023, N'Ten tai khoan DEMO khach hang da duoc su dung cho du lieu khac.', 1;
        IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = @Username)
            INSERT INTO Users(Username, PasswordHash, Email, Phone, RoleID, StatusID)
            VALUES (@Username, 'DEMO_ACCOUNT_DISABLED', 'demo.customer.' + @Suffix + '@example.invalid',
                '00000001' + @Suffix, @CustomerRole, @Inactive);
        SET @UserID = (SELECT UserID FROM Users WHERE Username = @Username);
        IF NOT EXISTS (SELECT 1 FROM Customers WHERE UserID = @UserID)
            INSERT INTO Customers(UserID, CustomerName, Phone, Email, Address)
            VALUES (@UserID, N'DEMO - Khách hàng ' + @Suffix, '00000001' + @Suffix,
                'demo.customer.' + @Suffix + '@example.invalid', N'DEMO - Địa chỉ mô phỏng cho bài báo cáo');
        SET @Index += 1;
    END;

    SET @Index = 1;
    WHILE @Index <= 3
    BEGIN
        SET @Suffix = RIGHT('00' + CAST(@Index AS VARCHAR(2)), 2);
        SET @Username = 'demo_employee_' + @Suffix;
        DECLARE @EmployeeRole INT = CASE WHEN @Index = 3 THEN @AccountantRole ELSE @SalesRole END;
        IF EXISTS (SELECT 1 FROM Users WHERE Username = @Username AND
            (PasswordHash <> 'DEMO_ACCOUNT_DISABLED' OR StatusID <> @Inactive OR RoleID <> @EmployeeRole))
            THROW 51024, N'Ten tai khoan DEMO nhan vien da duoc su dung cho du lieu khac.', 1;
        IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = @Username)
            INSERT INTO Users(Username, PasswordHash, Email, Phone, RoleID, StatusID)
            VALUES (@Username, 'DEMO_ACCOUNT_DISABLED', 'demo.employee.' + @Suffix + '@example.invalid',
                '00000002' + @Suffix, @EmployeeRole, @Inactive);
        SET @UserID = (SELECT UserID FROM Users WHERE Username = @Username);
        IF NOT EXISTS (SELECT 1 FROM Employees WHERE UserID = @UserID)
            INSERT INTO Employees(UserID, EmployeeName, Phone, Email)
            VALUES (@UserID, N'DEMO - Nhân viên ' + @Suffix, '00000002' + @Suffix,
                'demo.employee.' + @Suffix + '@example.invalid');
        SET @Index += 1;
    END;

    -- Moi dong la mot kich ban, dung gia tu xe DEMO cua danh muc da nap.
    DECLARE @DemoOrders TABLE (
        Number INT PRIMARY KEY,
        CarCode VARCHAR(50),
        MonthOffset INT,
        OrderStatus VARCHAR(30),
        DepositPercent INT,
        IsInstallment BIT
    );
    INSERT INTO @DemoOrders VALUES
        (1, 'DEMO-VF3-001', -5, 'COMPLETED', 20, 0),
        (2, 'DEMO-VF5PLUS-001', -4, 'COMPLETED', 20, 0),
        (3, 'DEMO-VF6ECO-001', -3, 'COMPLETED', 20, 0),
        (4, 'DEMO-VF7ECO-001', -2, 'PAID', 30, 1),
        (5, 'DEMO-VF8ECO-001', -1, 'COMPLETED', 20, 0),
        (6, 'DEMO-VF9ECO-001', 0, 'PAID', 20, 0),
        (7, 'DEMO-VF6PLUS-001', 0, 'DEPOSITED', 10, 0),
        (8, 'DEMO-VF7PLUS-001', 0, 'PROCESSING', 10, 0),
        (9, 'DEMO-VF8PLUS-001', 0, 'CANCELLED', 0, 0);
    IF EXISTS (SELECT 1 FROM @DemoOrders d LEFT JOIN Cars c ON c.CarCode = d.CarCode WHERE c.CarID IS NULL)
        THROW 51025, N'Thieu xe DEMO tu phan danh muc VinFast trong file nay.', 1;

    DECLARE @CarCode VARCHAR(50), @MonthOffset INT, @OrderStatus VARCHAR(30), @DepositPercent INT;
    DECLARE @IsInstallment BIT, @CarID INT, @OrderID INT, @CustomerID INT, @EmployeeID INT;
    DECLARE @OrderStatusID INT, @MethodID INT, @OrderDate DATETIME2, @PaidDate DATETIME2;
    DECLARE @Total DECIMAL(18, 2), @Deposit DECIMAL(18, 2), @Remaining DECIMAL(18, 2);
    DECLARE @Marker NVARCHAR(100), @PaymentMarker VARCHAR(100);
    SET @Index = 1;
    WHILE @Index <= 9
    BEGIN
        SELECT @CarCode = CarCode, @MonthOffset = MonthOffset, @OrderStatus = OrderStatus,
            @DepositPercent = DepositPercent, @IsInstallment = IsInstallment
        FROM @DemoOrders WHERE Number = @Index;
        SET @Suffix = RIGHT('00' + CAST(@Index AS VARCHAR(2)), 2);
        SET @Marker = N'DEMO-REPORT-' + @Suffix + N' | Giao dịch mô phỏng, không phải bán hàng thật.';
        SELECT @CarID = CarID, @Total = Price FROM Cars WHERE CarCode = @CarCode;
        SET @Deposit = ROUND(@Total * @DepositPercent / 100.0, 2);
        SET @Remaining = CASE WHEN @OrderStatus IN ('PAID', 'COMPLETED') THEN 0 ELSE @Total - @Deposit END;
        SET @OrderStatusID = (SELECT OrderStatusID FROM OrderStatuses WHERE StatusName = @OrderStatus);
        SET @MethodID = CASE WHEN @IsInstallment = 1 THEN @InstallmentMethod ELSE @BankTransfer END;
        SET @OrderDate = CASE WHEN @MonthOffset = 0 THEN CAST(@Today AS DATETIME2)
            ELSE DATEADD(DAY, 2, DATEADD(MONTH, @MonthOffset, @MonthStart)) END;
        SET @CustomerID = (SELECT c.CustomerID FROM Customers c JOIN Users u ON u.UserID = c.UserID
            WHERE u.Username = 'demo_customer_' + RIGHT('00' + CAST(((@Index - 1) % 6) + 1 AS VARCHAR(2)), 2));
        SET @EmployeeID = (SELECT e.EmployeeID FROM Employees e JOIN Users u ON u.UserID = e.UserID
            WHERE u.Username = CASE WHEN @Index % 2 = 1 THEN 'demo_employee_01' ELSE 'demo_employee_02' END);

        IF NOT EXISTS (SELECT 1 FROM Orders WHERE Note = @Marker)
        BEGIN
            IF EXISTS (SELECT 1 FROM OrderDetails WHERE CarID = @CarID)
                THROW 51026, N'Xe DEMO da gan voi mot don hang khac. Khong thay doi don hang da co.', 1;
            INSERT INTO Orders(CustomerID, EmployeeID, OrderStatusID, PaymentMethodID, OrderDate,
                TotalAmount, DepositAmount, RemainingAmount, Note)
            VALUES (@CustomerID, @EmployeeID, @OrderStatusID, @MethodID, @OrderDate,
                @Total, @Deposit, @Remaining, @Marker);
            SET @OrderID = CAST(SCOPE_IDENTITY() AS INT);
            INSERT INTO OrderDetails(OrderID, CarID, SalesPrice) VALUES (@OrderID, @CarID, @Total);
            IF @OrderStatus IN ('PAID', 'COMPLETED')
                UPDATE Cars SET CarStatusID = @Sold WHERE CarID = @CarID AND CarCode LIKE 'DEMO-%';
            ELSE IF @OrderStatus IN ('DEPOSITED', 'PROCESSING')
                UPDATE Cars SET CarStatusID = @Reserved WHERE CarID = @CarID AND CarCode LIKE 'DEMO-%';
            INSERT INTO OrderStatusHistory(OrderID, OrderStatusID, EmployeeID, ChangedDate, Note)
            VALUES (@OrderID, @OrderStatusID, @EmployeeID, @OrderDate, N'DEMO - Trạng thái khởi tạo cho bài báo cáo');
        END;
        SELECT @OrderID = OrderID, @OrderDate = OrderDate, @Total = TotalAmount,
            @Deposit = DepositAmount FROM Orders WHERE Note = @Marker;
        SET @PaidDate = CASE WHEN CAST(@OrderDate AS DATE) = @Today THEN @OrderDate ELSE DATEADD(DAY, 1, @OrderDate) END;

        IF @Deposit > 0
        BEGIN
            SET @PaymentMarker = 'DEMO-PAY-' + @Suffix + '-DEPOSIT';
            IF NOT EXISTS (SELECT 1 FROM Payments WHERE ReferenceCode = @PaymentMarker)
                INSERT INTO Payments(OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID,
                    PaymentAmount, PaymentDate, ReferenceCode, Note)
                VALUES (@OrderID, @BankTransfer, @DepositType, @PaymentCompleted,
                    @Deposit, @OrderDate, @PaymentMarker, N'DEMO - Tiền đặt cọc mô phỏng');
        END;
        IF @OrderStatus IN ('PAID', 'COMPLETED')
        BEGIN
            SET @PaymentMarker = 'DEMO-PAY-' + @Suffix + '-REMAINING';
            IF NOT EXISTS (SELECT 1 FROM Payments WHERE ReferenceCode = @PaymentMarker)
                INSERT INTO Payments(OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID,
                    PaymentAmount, PaymentDate, ReferenceCode, Note)
                VALUES (@OrderID, @BankTransfer, @RemainingType, @PaymentCompleted,
                    @Total - @Deposit, @PaidDate, @PaymentMarker,
                    CASE WHEN @IsInstallment = 1 THEN N'DEMO - Bên tài chính mô phỏng chuyển phần còn lại cho showroom'
                        ELSE N'DEMO - Thanh toán phần còn lại mô phỏng' END);
        END;
        ELSE
        BEGIN
            -- Khoan PENDING khong duoc tinh vao tien thuc thu.
            SET @PaymentMarker = 'DEMO-PAY-' + @Suffix + '-PENDING';
            IF NOT EXISTS (SELECT 1 FROM Payments WHERE ReferenceCode = @PaymentMarker)
                INSERT INTO Payments(OrderID, PaymentMethodID, PaymentTypeID, PaymentStatusID,
                    PaymentAmount, PaymentDate, ReferenceCode, Note)
                VALUES (@OrderID, @BankTransfer, @RemainingType, @PaymentPending,
                    @Total - @Deposit, @OrderDate, @PaymentMarker, N'DEMO - Thanh toán đang chờ, không tính doanh thu');
        END;
        SET @Index += 1;
    END;

    -- Tra gop duoc theo doi rieng, khong cong trung voi Payments cua showroom.
    IF NOT EXISTS (SELECT 1 FROM FinancialCompanies WHERE CompanyName = N'DEMO - Công ty tài chính mô phỏng')
        INSERT INTO FinancialCompanies(CompanyName, Email, Description)
        VALUES (N'DEMO - Công ty tài chính mô phỏng', 'demo.finance@example.invalid',
            N'DEMO - Đơn vị giả lập; lãi suất và lịch trả không phải sản phẩm tài chính thực tế');
    IF NOT EXISTS (SELECT 1 FROM InstallmentPlans WHERE PlanName = N'DEMO - 12 tháng, lãi suất 0%')
        INSERT INTO InstallmentPlans(PlanName, LoanTermMonths, InterestRate, Description)
        VALUES (N'DEMO - 12 tháng, lãi suất 0%', 12, 0, N'DEMO - Dùng minh họa tính tổng và khoản quá hạn');

    DECLARE @CompanyID INT = (SELECT CompanyID FROM FinancialCompanies WHERE CompanyName = N'DEMO - Công ty tài chính mô phỏng');
    DECLARE @PlanID INT = (SELECT PlanID FROM InstallmentPlans WHERE PlanName = N'DEMO - 12 tháng, lãi suất 0%');
    SELECT @OrderID = OrderID, @OrderDate = OrderDate, @Total = TotalAmount, @Deposit = DepositAmount
        FROM Orders WHERE Note = N'DEMO-REPORT-04 | Giao dịch mô phỏng, không phải bán hàng thật.';
    DECLARE @Loan DECIMAL(18, 2) = @Total - @Deposit;
    DECLARE @Periodic DECIMAL(18, 2) = ROUND(@Loan / 12.0, 2);
    IF NOT EXISTS (SELECT 1 FROM InstallmentContracts WHERE OrderID = @OrderID)
        INSERT INTO InstallmentContracts(OrderID, CompanyID, PlanID, VehiclePrice, DownPayment,
            LoanAmount, LoanTermMonths, InterestRate, TotalInterest, TotalInstallmentAmount,
            PeriodicPayment, ContractDate, Status)
        VALUES (@OrderID, @CompanyID, @PlanID, @Total, @Deposit, @Loan, 12, 0, 0, @Loan,
            @Periodic, @OrderDate, 'ACTIVE');
    DECLARE @ContractID INT = (SELECT InstallmentContractID FROM InstallmentContracts WHERE OrderID = @OrderID);
    DECLARE @FirstDue DATE = DATEADD(DAY, 14, DATEFROMPARTS(YEAR(@OrderDate), MONTH(@OrderDate), 1));
    DECLARE @ScheduleID INT, @PeriodAmount DECIMAL(18, 2), @AmountPaid DECIMAL(18, 2), @DueDate DATE;
    SET @Index = 1;
    WHILE @Index <= 12
    BEGIN
        SET @DueDate = DATEADD(MONTH, @Index - 1, @FirstDue);
        SET @PeriodAmount = CASE WHEN @Index = 12 THEN @Loan - @Periodic * 11 ELSE @Periodic END;
        SET @AmountPaid = CASE WHEN @Index = 1 THEN @PeriodAmount WHEN @Index = 2 THEN ROUND(@PeriodAmount / 2, 2) ELSE 0 END;
        IF NOT EXISTS (SELECT 1 FROM InstallmentSchedules WHERE InstallmentContractID = @ContractID AND PeriodNumber = @Index)
            INSERT INTO InstallmentSchedules(InstallmentContractID, PeriodNumber, DueDate,
                PrincipalAmount, InterestAmount, PaymentAmount, PaidAmount, Status)
            VALUES (@ContractID, @Index, @DueDate, @PeriodAmount, 0, @PeriodAmount, @AmountPaid,
                CASE WHEN @Index = 1 THEN 'PAID' ELSE 'PENDING' END);
        SET @ScheduleID = (SELECT ScheduleID FROM InstallmentSchedules WHERE InstallmentContractID = @ContractID AND PeriodNumber = @Index);
        IF @AmountPaid > 0
        BEGIN
            SET @PaymentMarker = 'DEMO-INSTALLMENT-' + RIGHT('00' + CAST(@Index AS VARCHAR(2)), 2);
            IF NOT EXISTS (SELECT 1 FROM InstallmentPayments WHERE ReferenceCode = @PaymentMarker)
                INSERT INTO InstallmentPayments(ScheduleID, PaymentAmount, PaymentDate,
                    PaymentMethodID, PaymentStatusID, ReferenceCode, Note)
                VALUES (@ScheduleID, @AmountPaid, @DueDate, @BankTransfer, @PaymentCompleted,
                    @PaymentMarker, N'DEMO - Khoản trả kỳ của khách cho bên tài chính, trình bày riêng');
        END;
        SET @Index += 1;
    END;

    -- Khuyen mai la kich ban cua bai tap, khong phai chuong trinh VinFast.
    IF NOT EXISTS (SELECT 1 FROM Promotions WHERE PromotionName = N'DEMO - Ưu đãi bài báo cáo')
        INSERT INTO Promotions(PromotionName, Description, DiscountType, DiscountValue, StartDate, EndDate, Status)
        VALUES (N'DEMO - Ưu đãi bài báo cáo', N'DEMO - Giảm 5% giả lập để trình bày CRUD; không tự áp dụng vào giá xe hoặc đơn hàng.',
            'PERCENTAGE', 5, @MonthStart, DATEADD(MONTH, 1, @MonthStart), 'ACTIVE');
    IF NOT EXISTS (SELECT 1 FROM Promotions WHERE PromotionName = N'DEMO - Chương trình đã kết thúc')
        INSERT INTO Promotions(PromotionName, Description, DiscountType, DiscountValue, StartDate, EndDate, Status)
        VALUES (N'DEMO - Chương trình đã kết thúc', N'DEMO - Minh họa lịch sử khuyến mãi và trạng thái vô hiệu hóa.',
            'FIXED', 5000000, DATEADD(MONTH, -2, @MonthStart), DATEADD(DAY, -1, @MonthStart), 'INACTIVE');
    INSERT INTO PromotionCars(PromotionID, ModelID)
        SELECT p.PromotionID, cm.ModelID FROM Promotions p CROSS JOIN CarModels cm
        WHERE p.PromotionName = N'DEMO - Ưu đãi bài báo cáo' AND cm.ModelName IN (N'VF 3', N'VF 5 Plus')
          AND NOT EXISTS (SELECT 1 FROM PromotionCars pc WHERE pc.PromotionID = p.PromotionID AND pc.ModelID = cm.ModelID);

    COMMIT TRANSACTION;
    PRINT N'Nap xong du lieu DEMO: 9 don mo phong, 6 xe da ban, 1 hop dong tra gop, 2 khuyen mai. Khong tao Admin.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

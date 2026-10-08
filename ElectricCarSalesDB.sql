-- Database chung cho tat ca thanh vien trong nhom.
-- Chay file nay trong SQL Server Management Studio.
-- Chi tao database/bang va du lieu mac dinh con thieu; giu nguyen du lieu da co.
-- Danh muc xe va giao dich DEMO nam trong hai script nap du lieu tuy chon.
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
        CONSTRAINT FK_Carversions_CarModels
            FOREIGN KEY(ModelID)
            REFERENCES dbo.CarModels(ModelID),
        CONSTRAINT UQ_Carversions_Model_Name
            UNIQUE (ModelID, VersionName)
    );
END;
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

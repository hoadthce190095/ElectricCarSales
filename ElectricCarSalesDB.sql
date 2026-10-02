USE master;
GO

/* =========================================================
   DROP OLD DATABASE
   ========================================================= */

IF DB_ID(N'ElectricCarSalesDB') IS NOT NULL
BEGIN
    ALTER DATABASE ElectricCarSalesDB 
    SET SINGLE_USER WITH ROLLBACK IMMEDIATE;

    DROP DATABASE ElectricCarSalesDB;
END
GO

/* =========================================================
   CREATE DATABASE
   ========================================================= */

CREATE DATABASE ElectricCarSalesDB;
GO

USE ElectricCarSalesDB;
GO

/*
	ROLES vai tro
*/
CREATE TABLE Roles(
	RoleID INT IDENTITY(1,1) PRIMARY KEY,
	RoleName VARCHAR(20) NOT NULL UNIQUE
	);
GO
/*
	STATUSES trang thai users
*/
CREATE TABLE Statuses(
	StatusID INT IDENTITY(1,1) PRIMARY KEY,
	StatusName VARCHAR(20) NOT NULL UNIQUE
	);
	GO


/* =========================================================
   1. USERS
   ========================================================= */

CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(100) NULL UNIQUE,
    Phone VARCHAR(20) NULL UNIQUE, 
	RoleID INT NOT NULL,
	StatusID INT NOT NULL,

	CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

	CONSTRAINT FK_Users_Roles 
		FOREIGN KEY (RoleID) REFERENCES Roles(RoleID),

	CONSTRAINT FK_Users_Statuses
		FOREIGN KEY (StatusID) REFERENCES Statuses(StatusID)
);
GO

INSERT INTO Roles(RoleName)
	VALUES
	('CUSTOMER'),
	('SALES'),
	('OPERATIONS'),
	('ACCOUNTANT'),
	('MANAGER'),
	('ADMIN');
	GO

INSERT INTO Statuses(StatusName)
	VALUES 
	('ACTIVE'),
	('INACTIVE'),
	('LOCKED');
	GO

/*
	STATUSES trang thai Brand
*/
CREATE TABLE BrandStatuses(
	BrandStatusID INT IDENTITY(1,1) PRIMARY KEY,
	StatusName VARCHAR(20) NOT NULL UNIQUE
	);
	GO

INSERT INTO BrandStatuses(StatusName)
	VALUES 
	('ACTIVE'),
	('INACTIVE');
	GO 
/* =========================================================
  5. BRANDS
 ========================================================= */

CREATE TABLE Brands (
    BrandID INT IDENTITY(1,1) PRIMARY KEY,
    BrandName NVARCHAR(100) NOT NULL UNIQUE,
    Description NVARCHAR(500) NULL,
	BrandStatusID INT NOT NULL,

	CONSTRAINT FK_Brands_BrandStatuses
	FOREIGN KEY (BrandStatusID)
	REFERENCES BrandStatuses(BrandStatusID)
	);
GO
/*=========================================================
  CarLines dòng xe 
 ========================================================= */
CREATE TABLE CarLines(
	CarLineID INT IDENTITY(1,1) PRIMARY KEY,
	BrandID INT NOT NULL,
	CarLineName NVARCHAR(100) NOT NULL,
	Description NVARCHAR(500) NULL,

	CONSTRAINT FK_CarLines_Brands
		FOREIGN KEY (BrandID)
		REFERENCES Brands(BrandID),

		 CONSTRAINT UQ_CarLines_Brand_Name
        UNIQUE (BrandID, CarLineName)
);
GO

/*=========================================================
  CarModels 
 ========================================================= */
CREATE TABLE CarModels(
	ModelID INT IDENTITY(1,1) PRIMARY KEY,
	CarLineID INT NOT NULL,
	ModelName NVARCHAR(100) NOT NULL,
	Description NVARCHAR(500) NULL,

	CONSTRAINT FK_CarModels_CarLines
		FOREIGN KEY (CarLineID)
		REFERENCES CarLines(CarLineID),

	CONSTRAINT UQ_CarModels_CarLine_Name
		UNIQUE (CarLineID, ModelName)
);
GO

/*=========================================================
  Specifications thông số 
 ========================================================= */

 CREATE TABLE Specifications(
	SpecificationID INT IDENTITY(1,1) PRIMARY KEY,
	ModelID INT NOT NULL,
	Dimensions NVARCHAR(100) NULL,
	BatteryCapacity DECIMAL(10,2) NULL,
	WheelType NVARCHAR(100) NULL,
	EnergyConsumption DECIMAL(10,2) NULL,
	RangeKM DECIMAL(10,2) NULL,
	MaxPower DECIMAL(10,2) NULL,
	MaxSpeed DECIMAL(10,2) NULL,
	SeatingCapacity INT NULL,
	ChargingTime DECIMAL(10,2) NULL,
	AirbagCount INT NULL,
	Description NVARCHAR(500) NULL,

	CONSTRAINT FK_Specifications_CarModels
		FOREIGN KEY (ModelID)
		REFERENCES  CarModels(ModelID),

	CONSTRAINT UQ_Specifications_CarModels
		UNIQUE (ModelID)
 );
 GO
 /*=========================================================
  ComparisonSessions SO SANH XE 
 ========================================================= */
 CREATE TABLE ComparisonSessions(
	ComparisonSessionID INT IDENTITY(1,1) PRIMARY KEY,
	UserID INT NOT NULL,
	CreateDate DATETIME2  NOT NULL DEFAULT SYSDATETIME(),

	CONSTRAINT FK_ComparisonSessions_Users
		FOREIGN KEY (UserID)
		REFERENCES Users(UserID)
	);
GO


 /*=========================================================
 ComparisonItems LUU CAC DONG XE 
 ========================================================= */
 CREATE TABLE ComparisonItems (
	ComparisonItemID INT IDENTITY(1,1) PRIMARY KEY,
	ComparisonSessionID INT NOT NULL,
	ModelID INT NOT NULL,

	CONSTRAINT FK_ComparisonItems_Sessions
		FOREIGN KEY (ComparisonSessionID)
		REFERENCES ComparisonSessions(ComparisonSessionID),

	CONSTRAINT FK_ComparisonItems_CarModels
		FOREIGN KEY (ModelID)
		REFERENCES CarModels(ModelID),

	CONSTRAINT UQ_ComparisonItems_Session_Model
		UNIQUE (ComparisonSessionID, ModelID)
 );
 GO 

 /*
	COLORS DANH SACH MAU
*/
CREATE TABLE Colors(
	ColorID INT IDENTITY(1,1) PRIMARY KEY,
	ColorName NVARCHAR(50) NOT NULL UNIQUE,
	Description NVARCHAR(200) NULL
);
GO

 /*
	MODELCOLORS LUA CHON MAU 
*/
CREATE TABLE ModelColors(
	ModelColorsID INT IDENTITY(1,1) PRIMARY KEY,
	ModelID INT NOT NULL,
	ColorID INT NOT NULL,

	CONSTRAINT FK_ModelColors_CarModels
		FOREIGN KEY (ModelID)
		REFERENCES CarModels(ModelID),

	CONSTRAINT FK_ModelColors_Colors
		FOREIGN KEY (ColorID)
		REFERENCES Colors(ColorID),

	CONSTRAINT UQ_ModelColors_Model_Color
		UNIQUE (ModelID, ColorID)
);
GO

 /*
	CARVERSIONS PHIEN BAN XE 
*/
CREATE TABLE Carversions(
	VersionID INT IDENTITY(1,1) PRIMARY KEY,
	ModelID INT NOT NULL,
	VersionName NVARCHAR(100) NOT NULL,
	Description NVARCHAR(500) NULL,

	CONSTRAINT FK_Carversions_CarModels
		FOREIGN KEY(ModelID)
		REFERENCES CarModels(ModelID),

	CONSTRAINT UQ_Carversions_Model_Name
		UNIQUE (ModelID, VersionName)
);
GO

 /*
	CARSTATUS TRANG THAI XE 
*/
CREATE TABLE CarStatuses(
	CarStatusID INT IDENTITY(1,1) PRIMARY KEY,
	StatusName NVARCHAR(30) NOT NULL UNIQUE,
	Description NVARCHAR(200) NULL
);
GO

/* 
	CARS 
*/
CREATE TABLE Cars(
	CarID INT IDENTITY(1,1) PRIMARY KEY,
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
	Price DECIMAL(18,2) NOT NULL,

	CONSTRAINT FK_Cars_CarModels
		FOREIGN KEY (ModelID)
		REFERENCES CarModels(ModelID),

	CONSTRAINT FK_Cars_CarVersions
		FOREIGN KEY(VersionID)
		REFERENCES CarVersions(VersionID),

	CONSTRAINT FK_Cars_Colors
		FOREIGN KEY (ColorID)
		REFERENCES Colors(ColorID),

	CONSTRAINT FK_Cars_CarStatuses
		FOREIGN KEY (CarStatusID)
		REFERENCES CarStatuses(CarStatusID)
);
GO


/* 
  CUSTOMERS
  */
CREATE TABLE Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL UNIQUE,
    CustomerName NVARCHAR(100) NOT NULL,
    Phone VARCHAR(20) NOT NULL UNIQUE,
    Email VARCHAR(100) NULL,
    Address NVARCHAR(255) NULL,   

    CONSTRAINT FK_Customers_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID)
);
GO


/* =========================================================
   3. EMPLOYEES 
   ========================================================= */
CREATE TABLE Employees (
	EmployeeID INT IDENTITY(1,1) PRIMARY KEY,
	UserID INT NOT NULL UNIQUE,
	EmployeeName NVARCHAR(100) NOT NULL,
	Phone VARCHAR(20) NOT NULL,
	Email VARCHAR(100) NULL,

	CONSTRAINT FK_Employees_Users
		FOREIGN KEY (UserID)
		REFERENCES Users(UserID)
);
GO
/* 
  ConsultationStatus   trang thai tu van 
  */
CREATE TABLE ConsultationStatuses(
	ConsultationStatusID INT IDENTITY(1,1) PRIMARY KEY,
	StatusName VARCHAR(30) NOT NULL UNIQUE
);
GO

INSERT INTO ConsultationStatuses(StatusName)
	VALUES
	('PENDING'),
	('IN_PROGRESS'),
	('COMPLETED'),
	('CANCELLED');

GO

/* 
  ConsultationRequests  yeu cau tu van 
  */
  CREATE TABLE ConsultationRequests(
	ConsultationRequestID INT IDENTITY(1,1) PRIMARY KEY,
	CustomerID INT NOT NULL,
	EmployeeID INT NULL,
	ModelID INT NULL,
	ConsultationStatusID INT NOT NULL,
	RequestContent NVARCHAR(1000) NOT NULL,
	RequestDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

	CONSTRAINT FK_ConsultationRequests_Customers
		FOREIGN KEY (CustomerID)
		REFERENCES Customers(CustomerID),

	CONSTRAINT FK_ConsultationRequests_Employees
		FOREIGN KEY (EmployeeID)
		REFERENCES Employees(EmployeeID),

	CONSTRAINT FK_ConsultationRequests_CarModels
		FOREIGN KEY (ModelID)
		REFERENCES CarModels(ModelID),

	CONSTRAINT FK_ConsultationRequests_Statuses
		FOREIGN KEY (ConsultationStatusID)
		REFERENCES ConsultationStatuses(ConsultationStatusID)

  );
  GO


/* 
  ORDERSTATUS  trang thai don hang
  */
CREATE TABLE OrderStatuses(
	OrderStatusID INT IDENTITY(1,1) PRIMARY KEY,
	StatusName VARCHAR(30) NOT NULL UNIQUE,
	Description NVARCHAR(200) NULL

);
GO

INSERT INTO OrderStatuses(StatusName, Description)
	VALUES
	('PENDING', 'New order created'),
	('RESERVED', 'Vehicle has been reserved'),
	('DEPOSITED', 'Deposit has been paid'),
	('PROCESSING', 'Order is being processed'),
	('PAID', 'Full payment has been completed'),
	('COMPLETED', 'Order has been completed'),
	('CANCELLED', 'Order has been cancelled');
	
GO

/* 
  PAYMENTMETHODS
  */
CREATE TABLE PaymentMethods(
	PaymentMethodID INT IDENTITY(1,1) PRIMARY KEY,
	MethodName VARCHAR(30) NOT NULL UNIQUE,
	Description NVARCHAR(200) NULL
);
GO

INSERT INTO PaymentMethods(MethodName, Description)
	VALUES 
	('CASH', 'Cash payment'),
	('BANK_TRANSFER', 'Bank tranfer payment'),
	('INSTALLMENT', 'Installment payment');
GO

/* 
  ORDERS
  */
CREATE TABLE Orders(
	OrderID INT IDENTITY(1,1) PRIMARY KEY,
	CustomerID INT NOT NULL,
	EmployeeID INT NULL,
	OrderStatusID INT NOT NULL,
	PaymentMethodID INT NOT NULL,
	OrderDate DATETIME2 NOT NULL,
	TotalAmount DECIMAL(18,2) NOT NULL,
	DepositAmount DECIMAL(18,2) NOT NULL,
	RemainingAmount DECIMAL(18,2) NOT NULL,
	Note NVARCHAR(500) NULL,

	CONSTRAINT FK_Orders_Customers
		FOREIGN KEY(CustomerID)
		REFERENCES Customers(CustomerID),

	CONSTRAINT FK_Orders_Employees
		FOREIGN KEY (EmployeeID)
		REFERENCES Employees(EmployeeID),

	CONSTRAINT FK_Orders_OrderStatuses
		FOREIGN KEY(OrderStatusID)
		REFERENCES OrderStatuses(OrderStatusID),

	CONSTRAINT FK_Orders_PaymentMethods
		FOREIGN KEY (PaymentMethodID)
		REFERENCES PaymentMethods(PaymentMethodID)
);
GO

/* 
  ORDERS DETAILS 
  */
CREATE TABLE OrderDetails(
	OrderDetailID INT IDENTITY(1,1) PRIMARY KEY,
	OrderID INT NOT NULL,
	CarID INT NOT NULL UNIQUE,
	SalesPrice DECIMAL(18,2) NOT NULL,

	CONSTRAINT FK_OrderDetails_Orders
		FOREIGN KEY (OrderID)
		REFERENCES Orders(OrderID),

	CONSTRAINT FK_OrderDetails_Cars
		FOREIGN KEY (CarID)
		REFERENCES Cars(CarID)
);
GO

/* 
  PAYMENT TYPE
  */
CREATE TABLE PaymentTypes(
	PaymentTypeID INT IDENTITY(1,1) PRIMARY KEY,
	TypeName VARCHAR(30) NOT NULL UNIQUE,
	Description NVARCHAR(200) NULL
);
GO

INSERT INTO PaymentTypes(TypeName, Description)
	VALUES
	('DEPOSIT', 'Vehicle deposit payment'),
	('REMAINING', 'Remaining amount payment'),
	('INSTALLMENT', 'Installment payment');
GO

/* 
  PAYMENT STATUS 
  */
CREATE TABLE PaymentStatuses(
	PaymentStatusID INT IDENTITY(1,1) PRIMARY KEY,
	StatusID VARCHAR(30) NOT NULL UNIQUE,
	Description NVARCHAR(200) NULL
);
GO

INSERT INTO PaymentStatuses(StatusID, Description)
	VALUES
	('PENDING' ,'Payment is pending'),
	('COMPLETED', 'Payment has been completed'),
	('FAILD', 'Payment has failed'),
	('CANCELLED', 'Payment has been cancelled');
GO

/* 
  PAYMENTS
  */
CREATE TABLE Payments(
	PaymentID INT IDENTITY(1,1) PRIMARY KEY,
	OrderID INT NOT NULL,
	PaymentMethodID INT NOT NULL,
	PaymentTypeID INT NOT NULL,
	PaymentStatusID INT NOT NULL,
	PaymentAmount DECIMAL(18,2) NOT NULL,
	PaymentDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
	ReferenceCode VARCHAR(100) NULL,
	Note NVARCHAR(500) NULL,

	CONSTRAINT FK_Payments_Orders
		FOREIGN KEY(OrderID)
		REFERENCES Orders(OrderID),

	CONSTRAINT FK_Payments_PaymentMethods
		FOREIGN KEY (PaymentMethodID)
		REFERENCES PaymentMethods(PaymentMethodID),

	CONSTRAINT FK_Payments_PaymentTypes
		FOREIGN KEY (PaymentTypeID)
		REFERENCES PaymentTypes(PaymentTypeID),

	CONSTRAINT FK_Payments_PaymentStatuses
		FOREIGN KEY (PaymentStatusID)
		REFERENCES PaymentStatuses(PaymentStatusID)
);
GO 


/* 
  InstallmentPlans
  */
CREATE TABLE InstallmentPlans(
	PlanID  INT IDENTITY(1,1) PRIMARY KEY,
	PlanName NVARCHAR(100) NOT NULL UNIQUE,
	LoanTermMonths INT NOT NULL,
	InterestRate DECIMAL(5,2) NOT NULL,
	Description NVARCHAR(500) NULL
);
GO

INSERT INTO InstallmentPlans
    (PlanName, LoanTermMonths, InterestRate, Description)
VALUES
    ('12 Months', 12, 0.00, '12-month installment plan'),
    ('24 Months', 24, 0.00, '24-month installment plan'),
    ('36 Months', 36, 0.00, '36-month installment plan'),
    ('48 Months', 48, 0.00, '48-month installment plan'),
    ('60 Months', 60, 0.00, '60-month installment plan');
GO

/* 
  FinancialCompanies
  */
CREATE TABLE FinancialCompanies(
	CompanyID INT IDENTITY(1,1) PRIMARY KEY,
	CompanyName NVARCHAR(150) NOT NULL UNIQUE,
	Phone VARCHAR(20) NULL,
	Email VARCHAR(100) NULL,
	Address NVARCHAR(255) NULL,
	Description NVARCHAR(500) NULL
);
GO

/* 
  InstallmentContracts
  */
CREATE TABLE InstallmentContracts(
    InstallmentContractID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    CompanyID INT NOT NULL,
    PlanID INT NOT NULL,

    VehiclePrice DECIMAL(18,2) NOT NULL,
    DownPayment DECIMAL(18,2) NOT NULL,
    LoanAmount DECIMAL(18,2) NOT NULL,
    LoanTermMonths INT NOT NULL,
    InterestRate DECIMAL(5,2) NOT NULL,
    TotalInterest DECIMAL(18,2) NOT NULL,
    TotalInstallmentAmount DECIMAL(18,2) NOT NULL,
    PeriodicPayment DECIMAL(18,2) NOT NULL,

    ContractDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Status VARCHAR(30) NOT NULL,

    CONSTRAINT FK_InstallmentContracts_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_InstallmentContracts_FinancialCompanies
        FOREIGN KEY (CompanyID)
        REFERENCES FinancialCompanies(CompanyID),

    CONSTRAINT FK_InstallmentContracts_InstallmentPlans
        FOREIGN KEY (PlanID)
        REFERENCES InstallmentPlans(PlanID),

    CONSTRAINT UQ_InstallmentContracts_Order
        UNIQUE (OrderID)
);
GO

/* 
  InstallmentSchedules
  */
CREATE TABLE InstallmentSchedules(
    ScheduleID INT IDENTITY(1,1) PRIMARY KEY,
    InstallmentContractID INT NOT NULL,

    PeriodNumber INT NOT NULL,
    DueDate DATE NOT NULL,
    PrincipalAmount DECIMAL(18,2) NOT NULL,
    InterestAmount DECIMAL(18,2) NOT NULL,
    PaymentAmount DECIMAL(18,2) NOT NULL,
    PaidAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    Status VARCHAR(30) NOT NULL,

    CONSTRAINT FK_InstallmentSchedules_InstallmentContracts
        FOREIGN KEY (InstallmentContractID)
        REFERENCES InstallmentContracts(InstallmentContractID),

    CONSTRAINT UQ_InstallmentSchedules_Contract_Period
        UNIQUE (InstallmentContractID, PeriodNumber)
);
GO

/* 
  InstallmentPayments
  */
CREATE TABLE InstallmentPayments(
    InstallmentPaymentID INT IDENTITY(1,1) PRIMARY KEY,
    ScheduleID INT NOT NULL,
    PaymentAmount DECIMAL(18,2) NOT NULL,
    PaymentDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PaymentMethodID INT NOT NULL,
    PaymentStatusID INT NOT NULL,
    ReferenceCode VARCHAR(100) NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InstallmentPayments_Schedules
        FOREIGN KEY (ScheduleID)
        REFERENCES InstallmentSchedules(ScheduleID),

    CONSTRAINT FK_InstallmentPayments_PaymentMethods
        FOREIGN KEY (PaymentMethodID)
        REFERENCES PaymentMethods(PaymentMethodID),

    CONSTRAINT FK_InstallmentPayments_PaymentStatuses
        FOREIGN KEY (PaymentStatusID)
        REFERENCES PaymentStatuses(PaymentStatusID)
);
GO

/* 
  InstallmentPayments
  */
  CREATE TABLE InstallmentSettlements(
    SettlementID INT IDENTITY(1,1) PRIMARY KEY,
    InstallmentContractID INT NOT NULL,

    SettlementDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PrincipalRemaining DECIMAL(18,2) NOT NULL,
    AccruedInterest DECIMAL(18,2) NOT NULL,
    SettlementFee DECIMAL(18,2) NOT NULL,
    SettlementAmount DECIMAL(18,2) NOT NULL,

    PaymentMethodID INT NOT NULL,
    EmployeeID INT NOT NULL,
    ReferenceCode VARCHAR(100) NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InstallmentSettlements_Contracts
        FOREIGN KEY (InstallmentContractID)
        REFERENCES InstallmentContracts(InstallmentContractID),

    CONSTRAINT FK_InstallmentSettlements_PaymentMethods
        FOREIGN KEY (PaymentMethodID)
        REFERENCES PaymentMethods(PaymentMethodID),

    CONSTRAINT FK_InstallmentSettlements_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT UQ_InstallmentSettlements_Contract
        UNIQUE (InstallmentContractID)
);
GO

/* 
  Invoices
  */
CREATE TABLE Invoices(
    InvoiceID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    CustomerID INT NOT NULL,
    EmployeeID INT NOT NULL,

    InvoiceNumber VARCHAR(50) NOT NULL UNIQUE,
    InvoiceDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    SubTotal DECIMAL(18,2) NOT NULL,
    DiscountAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    TaxAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    TotalAmount DECIMAL(18,2) NOT NULL,

    Status VARCHAR(30) NOT NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_Invoices_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_Invoices_Customers
        FOREIGN KEY (CustomerID)
        REFERENCES Customers(CustomerID),

    CONSTRAINT FK_Invoices_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT UQ_Invoices_Order
        UNIQUE (OrderID)
);
GO

/* 
  Contracts
  */
 CREATE TABLE Contracts(
    ContractID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    CustomerID INT NOT NULL,
    EmployeeID INT NOT NULL,

    ContractNumber VARCHAR(50) NOT NULL UNIQUE,
    ContractDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ContractAmount DECIMAL(18,2) NOT NULL,
    Status VARCHAR(30) NOT NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_Contracts_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_Contracts_Customers
        FOREIGN KEY (CustomerID)
        REFERENCES Customers(CustomerID),

    CONSTRAINT FK_Contracts_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT UQ_Contracts_Order
        UNIQUE (OrderID)
);
GO

/* 
  Contracts
  */
CREATE TABLE Deliveries(
    DeliveryID INT IDENTITY(1,1) PRIMARY KEY,
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
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_Deliveries_Customers
        FOREIGN KEY (CustomerID)
        REFERENCES Customers(CustomerID),

    CONSTRAINT FK_Deliveries_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT FK_Deliveries_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID),

    CONSTRAINT UQ_Deliveries_Order
        UNIQUE (OrderID)
);
GO
/* 
  OTPVerificationsacts
  */
CREATE TABLE OTPVerifications(
    OTPVerificationID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,

    OTPCode VARCHAR(10) NOT NULL,
    Purpose VARCHAR(50) NOT NULL,
    CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ExpiredDate DATETIME2 NOT NULL,
    VerifiedDate DATETIME2 NULL,
    Status VARCHAR(30) NOT NULL,

    CONSTRAINT FK_OTPVerifications_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID)
);
GO

/* 
  InventoryTransactions
  */
CREATE TABLE InventoryTransactions(
    InventoryTransactionID INT IDENTITY(1,1) PRIMARY KEY,
    CarID INT NOT NULL,
    EmployeeID INT NOT NULL,
    SupplierID INT NULL,

    TransactionType VARCHAR(30) NOT NULL,
    TransactionDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InventoryTransactions_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID),

    CONSTRAINT FK_InventoryTransactions_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT FK_InventoryTransactions_Suppliers
        FOREIGN KEY (SupplierID)
        REFERENCES Suppliers(SupplierID)
);
GO

/* 
  CarImages
  */
CREATE TABLE CarImages(
	CarImageID INT IDENTITY(1,1) PRIMARY KEY,
	ModelID INT NOT NULL,
	ImageURL NVARCHAR(500) NOT NULL,
	ImageType VARCHAR(30) NULL,
	IsPrimary BIT NULL,
	DisplayOrder INT NULL,
	CreateDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

	CONSTRAINT FK_CarImages_CarModels
		FOREIGN KEY(ModelID)
		REFERENCES CarModels(ModelID)
);
GO
/* 
  Promotions
  */
CREATE TABLE Promotions(
	PromotionID INT IDENTITY(1,1) PRIMARY KEY,
	PromotionName NVARCHAR(150) NOT NULL,
	Description NVARCHAR(500) NULL,
	DiscountType VARCHAR(30) NULL,
	DiscountValue DECIMAL(18,2) NULL,
	StartDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
	EndDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
	Status VARCHAR(30)
);
GO 

/* 
  PromotionCars
  */
CREATE TABLE PromotionCars(
	PromotionCarID INT IDENTITY(1,1) PRIMARY KEY,
	PromotionID INT NOT NULL,
	ModelID INT NOT NULL,

	CONSTRAINT FK_PromotionCars_Promotions
		FOREIGN KEY(PromotionID)
		REFERENCES Promotions(PromotionID),

	CONSTRAINT FK_PromotionCars_CarModels
		FOREIGN KEY(ModelID)
		REFERENCES CarModels(ModelID),

	CONSTRAINT UQ_PromotionCars_Promotion_Model
		UNIQUE (PromotionID, ModelID)
);
GO
/* 
  Suppliers
  */
CREATE TABLE Suppliers(
	SupplierID INT IDENTITY(1,1) PRIMARY KEY,
	SupplierName NVARCHAR(150) NOT NULL UNIQUE,
	Phone VARCHAR(20) NULL,
	Email VARCHAR(100) NULL,
	Address NVARCHAR(255) NULL,
	Description NVARCHAR(500) NULL
);
GO 

/* 
  VehicleTCO
  */
CREATE TABLE VehicleTCO(
	TCOID INT IDENTITY(1,1) PRIMARY KEY,
	ModelID INT NOT NULL,
	VehiclePrice DECIMAL(18,2) NULL,
	EnergyCost DECIMAL(18,2) NULL,
	MaintenanceCost DECIMAL(18,2) NULL,
	InsuranceCost DECIMAL(18,2) NULL,
	RegistrationCost DECIMAL(18,2) NULL,
	IncentiveAmount DECIMAL(18,2) NULL,
	TotalCost DECIMAL(18,2) NULL,
	PeriodMonths INT NULL,
	Description NVARCHAR(500) NULL,
	CreateDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

	CONSTRAINT FK_VehicleTCO_CarModels
		FOREIGN KEY (ModelID)
		REFERENCES CarModels(ModelID)
);
GO 
/* 
  OrderStatusHistory
  */
 CREATE TABLE OrderStatusHistory(
    OrderStatusHistoryID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    OrderStatusID INT NOT NULL,
    EmployeeID INT NULL,
    ChangedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_OrderStatusHistory_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_OrderStatusHistory_OrderStatuses
        FOREIGN KEY (OrderStatusID)
        REFERENCES OrderStatuses(OrderStatusID),

    CONSTRAINT FK_OrderStatusHistory_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID)
);
GO

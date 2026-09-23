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


/* =========================================================
   1. USERS
   ========================================================= */

CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(100) NULL UNIQUE,
    Phone VARCHAR(20) NULL UNIQUE,
    Role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT CK_Users_Role
        CHECK (Role IN (
            'CUSTOMER',
            'SALES',
            'OPERATIONS',
            'ACCOUNTANT',
            'MANAGER',
            'ADMIN'
        )),

    CONSTRAINT CK_Users_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE',
            'LOCKED'
        ))
);
GO


/* =========================================================
   2. CUSTOMERS
   ========================================================= */

CREATE TABLE Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NULL,
    FullName NVARCHAR(100) NOT NULL,
    Phone VARCHAR(20) NOT NULL UNIQUE,
    Email VARCHAR(100) NULL,
    Address NVARCHAR(255) NULL,
    CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

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
    FullName NVARCHAR(100) NOT NULL,
    Phone VARCHAR(20) NULL UNIQUE,
    Email VARCHAR(100) NULL UNIQUE,
    Position VARCHAR(30) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    HireDate DATE NULL,

    CONSTRAINT FK_Employees_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    CONSTRAINT CK_Employees_Position
        CHECK (Position IN (
            'SALES',
            'OPERATIONS',
            'ACCOUNTANT',
            'MANAGER',
            'ADMIN'
        )),

    CONSTRAINT CK_Employees_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   4. OTP VERIFICATIONS
   ========================================================= */

CREATE TABLE OTPVerifications (
    OTPID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    OTPCode VARCHAR(10) NOT NULL,
    OTPType VARCHAR(30) NOT NULL,
    ExpiredAt DATETIME2 NOT NULL,
    IsVerified BIT NOT NULL DEFAULT 0,
    CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_OTPVerifications_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    CONSTRAINT CK_OTPVerifications_Type
        CHECK (OTPType IN (
            'REGISTER',
            'PAYMENT',
            'TRANSACTION_CONFIRM'
        ))
);
GO


/* =========================================================
   5. BRANDS
   ========================================================= */

CREATE TABLE Brands (
    BrandID INT IDENTITY(1,1) PRIMARY KEY,
    BrandName NVARCHAR(100) NOT NULL UNIQUE,
    Description NVARCHAR(500) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT CK_Brands_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   6. CAR LINES
   ========================================================= */

CREATE TABLE CarLines (
    LineID INT IDENTITY(1,1) PRIMARY KEY,
    BrandID INT NOT NULL,
    LineName NVARCHAR(100) NOT NULL,
    Description NVARCHAR(500) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT FK_CarLines_Brands
        FOREIGN KEY (BrandID)
        REFERENCES Brands(BrandID),

    CONSTRAINT UQ_CarLines_Brand_Line
        UNIQUE (BrandID, LineName),

    CONSTRAINT CK_CarLines_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   7. CAR MODELS
   ========================================================= */

CREATE TABLE CarModels (
    ModelID INT IDENTITY(1,1) PRIMARY KEY,
    BrandID INT NOT NULL,
    LineID INT NOT NULL,
    ModelName NVARCHAR(100) NOT NULL,
    Description NVARCHAR(1000) NULL,
    BasePrice DECIMAL(18,2) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT FK_CarModels_Brands
        FOREIGN KEY (BrandID)
        REFERENCES Brands(BrandID),

    CONSTRAINT FK_CarModels_CarLines
        FOREIGN KEY (LineID)
        REFERENCES CarLines(LineID),

    CONSTRAINT UQ_CarModels_Brand_Model
        UNIQUE (BrandID, ModelName),

    CONSTRAINT CK_CarModels_BasePrice
        CHECK (BasePrice >= 0),

    CONSTRAINT CK_CarModels_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   8. CAR VERSIONS
   ========================================================= */

CREATE TABLE CarVersions (
    VersionID INT IDENTITY(1,1) PRIMARY KEY,
    ModelID INT NOT NULL,
    VersionName NVARCHAR(100) NOT NULL,
    AdditionalPrice DECIMAL(18,2) NOT NULL DEFAULT 0,
    Description NVARCHAR(500) NULL,

    CONSTRAINT FK_CarVersions_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID),

    CONSTRAINT UQ_CarVersions_Model_Version
        UNIQUE (ModelID, VersionName),

    CONSTRAINT CK_CarVersions_AdditionalPrice
        CHECK (AdditionalPrice >= 0)
);
GO


/* =========================================================
   9. COLORS
   ========================================================= */

CREATE TABLE Colors (
    ColorID INT IDENTITY(1,1) PRIMARY KEY,
    ColorName NVARCHAR(50) NOT NULL UNIQUE
);
GO


/* =========================================================
   10. SUPPLIERS
   ========================================================= */

CREATE TABLE Suppliers (
    SupplierID INT IDENTITY(1,1) PRIMARY KEY,
    SupplierName NVARCHAR(150) NOT NULL UNIQUE,
    Phone VARCHAR(20) NULL,
    Email VARCHAR(100) NULL,
    Address NVARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT CK_Suppliers_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   11. CARS
   ========================================================= */

CREATE TABLE Cars (
    CarID INT IDENTITY(1,1) PRIMARY KEY,
    CarCode VARCHAR(50) NOT NULL UNIQUE,
    VersionID INT NOT NULL,
    ColorID INT NOT NULL,
    VIN VARCHAR(50) NOT NULL UNIQUE,
    EngineNumber VARCHAR(50) NOT NULL UNIQUE,
    Year INT NOT NULL,
    Price DECIMAL(18,2) NOT NULL,
    CarStatus VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    /* INVENTORY INFORMATION */
    Origin NVARCHAR(100) NULL,
    BatchCode VARCHAR(50) NULL,
    SupplierID INT NULL,
    ImportedDate DATETIME2 NULL,
    InventoryLocation NVARCHAR(50) NULL,

    CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Cars_CarVersions
        FOREIGN KEY (VersionID)
        REFERENCES CarVersions(VersionID),

    CONSTRAINT FK_Cars_Colors
        FOREIGN KEY (ColorID)
        REFERENCES Colors(ColorID),

    CONSTRAINT FK_Cars_Suppliers
        FOREIGN KEY (SupplierID)
        REFERENCES Suppliers(SupplierID),

    CONSTRAINT CK_Cars_Year
        CHECK (Year >= 2000 AND Year <= 2100),

    CONSTRAINT CK_Cars_Price
        CHECK (Price >= 0),

    CONSTRAINT CK_Cars_Status
        CHECK (CarStatus IN (
            'AVAILABLE',
            'RESERVED',
            'DEPOSITED',
            'PROCESSING',
            'SOLD',
            'DELIVERED'
        )),

    CONSTRAINT CK_Cars_InventoryLocation
        CHECK (
            InventoryLocation IS NULL
            OR InventoryLocation IN (
                'SHOWROOM',
                'WAREHOUSE',
                'PARKING'
            )
        )
);
GO


/* =========================================================
   12. CAR IMAGES
   ========================================================= */

CREATE TABLE CarImages (
    ImageID INT IDENTITY(1,1) PRIMARY KEY,
    ModelID INT NOT NULL,
    ImageURL VARCHAR(500) NOT NULL,
    IsMain BIT NOT NULL DEFAULT 0,

    CONSTRAINT FK_CarImages_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID)
);
GO


/* =========================================================
   13. SPECIFICATIONS
   ========================================================= */

CREATE TABLE Specifications (
    SpecificationID INT IDENTITY(1,1) PRIMARY KEY,
    ModelID INT NOT NULL UNIQUE,

    /* BASIC */
    BatteryCapacity DECIMAL(10,2) NULL,
    RangeKM INT NULL,
    MaxSpeed INT NULL,
    SeatingCapacity INT NULL,
    ChargingTime DECIMAL(10,2) NULL,

    /* MOTOR */
    MotorType NVARCHAR(100) NULL,
    MotorCount INT NULL,
    MotorPowerKW DECIMAL(10,2) NULL,
    TorqueNM DECIMAL(10,2) NULL,
    DriveType VARCHAR(20) NULL,
    DrivingModes NVARCHAR(255) NULL,
    Acceleration NVARCHAR(100) NULL,

    /* BATTERY */
    BatteryType VARCHAR(50) NULL,
    BatteryVoltage DECIMAL(10,2) NULL,
    BatteryManufacturer NVARCHAR(100) NULL,
    BatteryWarranty NVARCHAR(255) NULL,

    /* SAFETY */
    RegenerativeBraking BIT NULL,
    ADAS NVARCHAR(500) NULL,
    DashCamera BIT NULL,
    CollisionWarning BIT NULL,
    LaneKeepingAssist BIT NULL,
    AdaptiveCruiseControl BIT NULL,
    ABS BIT NULL,
    ESP BIT NULL,
    BlindSpotWarning BIT NULL,
    Airbags NVARCHAR(100) NULL,
    CruiseControl BIT NULL,

    /* INTERIOR */
    EntertainmentScreen NVARCHAR(255) NULL,
    Sensors NVARCHAR(255) NULL,
    Camera NVARCHAR(255) NULL,
    AudioSystem NVARCHAR(255) NULL,
    AppleCarPlay BIT NULL,
    AndroidAuto BIT NULL,
    SteeringWheel NVARCHAR(255) NULL,
    Seats NVARCHAR(255) NULL,
    AirConditioning NVARCHAR(255) NULL,

    Description NVARCHAR(1000) NULL,

    CONSTRAINT FK_Specifications_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID),

    CONSTRAINT CK_Specifications_Battery
        CHECK (
            BatteryCapacity IS NULL
            OR BatteryCapacity >= 0
        ),

    CONSTRAINT CK_Specifications_Range
        CHECK (
            RangeKM IS NULL
            OR RangeKM >= 0
        ),

    CONSTRAINT CK_Specifications_MaxSpeed
        CHECK (
            MaxSpeed IS NULL
            OR MaxSpeed >= 0
        ),

    CONSTRAINT CK_Specifications_Seats
        CHECK (
            SeatingCapacity IS NULL
            OR SeatingCapacity > 0
        ),

    CONSTRAINT CK_Specifications_Charging
        CHECK (
            ChargingTime IS NULL
            OR ChargingTime >= 0
        ),

    CONSTRAINT CK_Specifications_MotorCount
        CHECK (
            MotorCount IS NULL
            OR MotorCount > 0
        ),

    CONSTRAINT CK_Specifications_MotorPower
        CHECK (
            MotorPowerKW IS NULL
            OR MotorPowerKW >= 0
        ),

    CONSTRAINT CK_Specifications_Torque
        CHECK (
            TorqueNM IS NULL
            OR TorqueNM >= 0
        ),

    CONSTRAINT CK_Specifications_DriveType
        CHECK (
            DriveType IS NULL
            OR DriveType IN (
                'FWD',
                'RWD',
                'AWD'
            )
        )
);
GO


/* =========================================================
   14. CONSULTATION REQUESTS
   ========================================================= */

CREATE TABLE ConsultationRequests (
    RequestID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NULL,
    ModelID INT NOT NULL,
    Phone VARCHAR(20) NOT NULL,
    Message NVARCHAR(1000) NULL,
    AssignedEmployeeID INT NULL,
    RequestStatus VARCHAR(20) NOT NULL DEFAULT 'NEW',
    CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    HandledDate DATETIME2 NULL,

    CONSTRAINT FK_ConsultationRequests_Customers
        FOREIGN KEY (CustomerID)
        REFERENCES Customers(CustomerID),

    CONSTRAINT FK_ConsultationRequests_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID),

    CONSTRAINT FK_ConsultationRequests_Employees
        FOREIGN KEY (AssignedEmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_ConsultationRequests_Status
        CHECK (RequestStatus IN (
            'NEW',
            'CONTACTED',
            'IN_PROGRESS',
            'COMPLETED',
            'CANCELLED'
        ))
);
GO


/* =========================================================
   15. ORDERS
   ========================================================= */

CREATE TABLE Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    EmployeeID INT NULL,
    OrderDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    PaymentMethod VARCHAR(20) NOT NULL,
    OrderStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    TotalAmount DECIMAL(18,2) NOT NULL,
    DepositAmount DECIMAL(18,2) NOT NULL DEFAULT 0,

    /* Remaining amount after current payments */
    RemainingAmount DECIMAL(18,2) NOT NULL DEFAULT 0,

    Note NVARCHAR(1000) NULL,

    CONSTRAINT FK_Orders_Customers
        FOREIGN KEY (CustomerID)
        REFERENCES Customers(CustomerID),

    CONSTRAINT FK_Orders_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_Orders_PaymentMethod
        CHECK (PaymentMethod IN (
            'CASH',
            'BANK_TRANSFER',
            'INSTALLMENT'
        )),

    CONSTRAINT CK_Orders_Status
        CHECK (OrderStatus IN (
            'PENDING',
            'RESERVED',
            'DEPOSITED',
            'PROCESSING',
            'PAID',
            'COMPLETED',
            'CANCELLED'
        )),

    CONSTRAINT CK_Orders_Amounts
        CHECK (
            TotalAmount >= 0
            AND DepositAmount >= 0
            AND RemainingAmount >= 0
            AND DepositAmount <= TotalAmount
            AND RemainingAmount <= TotalAmount
        )
);
GO


/* =========================================================
   16. ORDER DETAILS
   ========================================================= */

CREATE TABLE OrderDetails (
    OrderDetailID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    CarID INT NOT NULL,
    UnitPrice DECIMAL(18,2) NOT NULL,
    Quantity INT NOT NULL DEFAULT 1,

    CONSTRAINT FK_OrderDetails_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_OrderDetails_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID),

    CONSTRAINT UQ_OrderDetails_Car
        UNIQUE (CarID),

    CONSTRAINT CK_OrderDetails_Price
        CHECK (UnitPrice >= 0),

    CONSTRAINT CK_OrderDetails_Quantity
        CHECK (Quantity = 1)
);
GO


/* =========================================================
   17. PAYMENTS
   ========================================================= */

CREATE TABLE Payments (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    PaymentType VARCHAR(20) NOT NULL,
    Amount DECIMAL(18,2) NOT NULL,
    PaymentDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PaymentMethod VARCHAR(20) NOT NULL,
    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ConfirmedBy INT NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_Payments_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_Payments_Employees
        FOREIGN KEY (ConfirmedBy)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_Payments_Type
        CHECK (PaymentType IN (
            'DEPOSIT',
            'REMAINING',
            'FULL_PAYMENT'
        )),

    CONSTRAINT CK_Payments_Amount
        CHECK (Amount > 0),

    CONSTRAINT CK_Payments_Method
        CHECK (PaymentMethod IN (
            'CASH',
            'BANK_TRANSFER'
        )),

    CONSTRAINT CK_Payments_Status
        CHECK (PaymentStatus IN (
            'PENDING',
            'CONFIRMED',
            'FAILED',
            'REFUNDED'
        ))
);
GO


/* =========================================================
   18. FINANCIAL COMPANIES
   ========================================================= */

CREATE TABLE FinancialCompanies (
    CompanyID INT IDENTITY(1,1) PRIMARY KEY,
    CompanyName NVARCHAR(150) NOT NULL UNIQUE,
    Phone VARCHAR(20) NULL,
    Address NVARCHAR(255) NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT CK_FinancialCompanies_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   19. INSTALLMENT PLANS
   ========================================================= */

CREATE TABLE InstallmentPlans (
    PlanID INT IDENTITY(1,1) PRIMARY KEY,
    CompanyID INT NOT NULL,
    TermMonths INT NOT NULL,
    DownPaymentPercent DECIMAL(5,2) NOT NULL,
    InterestRate DECIMAL(5,2) NOT NULL DEFAULT 0,
    DocumentRequirement NVARCHAR(255) NULL,
    ProcessingFee DECIMAL(18,2) NOT NULL DEFAULT 0,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT FK_InstallmentPlans_Companies
        FOREIGN KEY (CompanyID)
        REFERENCES FinancialCompanies(CompanyID),

    -- Kỳ hạn trả góp được hỗ trợ
    CONSTRAINT CK_InstallmentPlans_Term
        CHECK (
            TermMonths IN (12, 24, 36, 48, 60, 72)
        ),

    -- Tiền trả trước / đặt cọc
    CONSTRAINT CK_InstallmentPlans_DownPayment
        CHECK (
            DownPaymentPercent > 0
            AND DownPaymentPercent <= 100
        ),

    -- Lãi suất không âm
    CONSTRAINT CK_InstallmentPlans_Interest
        CHECK (
            InterestRate >= 0
        ),

    -- Phí xử lý không âm
    CONSTRAINT CK_InstallmentPlans_Fee
        CHECK (
            ProcessingFee >= 0
        ),

    CONSTRAINT CK_InstallmentPlans_Status
        CHECK (
            Status IN (
                'ACTIVE',
                'INACTIVE'
            )
        )
);
GO


/* =========================================================
   20. INSTALLMENT CONTRACTS
   ========================================================= */

CREATE TABLE InstallmentContracts (
    InstallmentID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL UNIQUE,

    CompanyID INT NOT NULL,
    PlanID INT NOT NULL,

    DownPayment DECIMAL(18,2) NOT NULL,
    LoanAmount DECIMAL(18,2) NOT NULL,
    LoanTermMonths INT NOT NULL,
    InterestRate DECIMAL(5,2) NOT NULL DEFAULT 0,

    Status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    CONSTRAINT FK_InstallmentContracts_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_InstallmentContracts_Companies
        FOREIGN KEY (CompanyID)
        REFERENCES FinancialCompanies(CompanyID),

    CONSTRAINT FK_InstallmentContracts_Plans
        FOREIGN KEY (PlanID)
        REFERENCES InstallmentPlans(PlanID),

    CONSTRAINT CK_Installment_DownPayment
        CHECK (DownPayment >= 0),

    CONSTRAINT CK_Installment_LoanAmount
        CHECK (LoanAmount >= 0),

    CONSTRAINT CK_Installment_Term
        CHECK (LoanTermMonths > 0),

    CONSTRAINT CK_Installment_Interest
        CHECK (InterestRate >= 0),

    CONSTRAINT CK_Installment_Status
        CHECK (
            Status IN (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'COMPLETED'
            )
        )
);
GO


/* =========================================================
   21. CONTRACTS
   ========================================================= */

CREATE TABLE Contracts (
    ContractID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL UNIQUE,

    ContractNumber VARCHAR(50) NOT NULL UNIQUE,
    ContractType VARCHAR(20) NOT NULL,
    ContractDate DATE NOT NULL DEFAULT GETDATE(),
    ContractStatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    FileURL VARCHAR(500) NULL,

    CONSTRAINT FK_Contracts_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT CK_Contracts_Type
        CHECK (
            ContractType IN (
                'SALE',
                'INSTALLMENT'
            )
        ),

    CONSTRAINT CK_Contracts_Status
        CHECK (
            ContractStatus IN (
                'DRAFT',
                'SIGNED',
                'CANCELLED'
            )
        )
);
GO


/* =========================================================
   22. INVOICES
   ========================================================= */

CREATE TABLE Invoices (
    InvoiceID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL UNIQUE,

    InvoiceNumber VARCHAR(50) NOT NULL UNIQUE,
    InvoiceDate DATE NOT NULL DEFAULT GETDATE(),
    TotalAmount DECIMAL(18,2) NOT NULL,
    TaxAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    InvoiceStatus VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    IssuedBy INT NULL,

    CONSTRAINT FK_Invoices_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_Invoices_Employees
        FOREIGN KEY (IssuedBy)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_Invoices_Total
        CHECK (TotalAmount >= 0),

    CONSTRAINT CK_Invoices_Tax
        CHECK (TaxAmount >= 0),

    CONSTRAINT CK_Invoices_Status
        CHECK (
            InvoiceStatus IN (
                'DRAFT',
                'ISSUED',
                'CANCELLED'
            )
        )
);
GO


/* =========================================================
   23. DELIVERIES
   ========================================================= */

CREATE TABLE Deliveries (
    DeliveryID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL UNIQUE,
    CarID INT NOT NULL UNIQUE,

    DeliveryDate DATETIME2 NULL,
    DeliveryLocation NVARCHAR(255) NULL,
    DeliveredBy INT NULL,
    ReceiverName NVARCHAR(100) NOT NULL,
    DeliveryStatus VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    Note NVARCHAR(1000) NULL,

    CONSTRAINT FK_Deliveries_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_Deliveries_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID),

    CONSTRAINT FK_Deliveries_Employees
        FOREIGN KEY (DeliveredBy)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_Deliveries_Status
        CHECK (
            DeliveryStatus IN (
                'PREPARING',
                'READY',
                'DELIVERED',
                'CANCELLED'
            )
        )
);
GO


/* =========================================================
   24. INVENTORY TRANSACTIONS
   ========================================================= */

CREATE TABLE InventoryTransactions (
    TransactionID INT IDENTITY(1,1) PRIMARY KEY,
    CarID INT NOT NULL,
    TransactionType VARCHAR(20) NOT NULL,
    TransactionDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    EmployeeID INT NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InventoryTransactions_Cars
        FOREIGN KEY (CarID)
        REFERENCES Cars(CarID),

    CONSTRAINT FK_InventoryTransactions_Employees
        FOREIGN KEY (EmployeeID)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_InventoryTransactions_Type
        CHECK (
            TransactionType IN (
                'IMPORT',
                'RESERVE',
                'RELEASE',
                'SOLD',
                'DELIVERED'
            )
        )
);
GO


/* =========================================================
   25. PROMOTIONS
   ========================================================= */

CREATE TABLE Promotions (
    PromotionID INT IDENTITY(1,1) PRIMARY KEY,
    PromotionName NVARCHAR(150) NOT NULL UNIQUE,
    Description NVARCHAR(1000) NULL,
    DiscountAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT CK_Promotions_Discount
        CHECK (DiscountAmount >= 0),

    CONSTRAINT CK_Promotions_Date
        CHECK (EndDate >= StartDate),

    CONSTRAINT CK_Promotions_Status
        CHECK (
            Status IN (
                'ACTIVE',
                'INACTIVE',
                'EXPIRED'
            )
        )
);
GO


/* =========================================================
   26. PROMOTION CARS
   ========================================================= */

CREATE TABLE PromotionCars (
    PromotionID INT NOT NULL,
    ModelID INT NOT NULL,

    CONSTRAINT PK_PromotionCars
        PRIMARY KEY (PromotionID, ModelID),

    CONSTRAINT FK_PromotionCars_Promotions
        FOREIGN KEY (PromotionID)
        REFERENCES Promotions(PromotionID),

    CONSTRAINT FK_PromotionCars_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID)
);
GO


/* =========================================================
   27. ORDER STATUS HISTORY
   ========================================================= */

CREATE TABLE OrderStatusHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    OldStatus VARCHAR(20) NULL,
    NewStatus VARCHAR(20) NOT NULL,
    ChangedBy INT NULL,
    ChangedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Note NVARCHAR(255) NULL,

    CONSTRAINT FK_OrderStatusHistory_Orders
        FOREIGN KEY (OrderID)
        REFERENCES Orders(OrderID),

    CONSTRAINT FK_OrderStatusHistory_Users
        FOREIGN KEY (ChangedBy)
        REFERENCES Users(UserID),

    CONSTRAINT CK_OrderStatusHistory_NewStatus
        CHECK (
            NewStatus IN (
                'PENDING',
                'RESERVED',
                'DEPOSITED',
                'PROCESSING',
                'PAID',
                'COMPLETED',
                'CANCELLED'
            )
        )
);
GO


/* =========================================================
   28. INSTALLMENT SCHEDULES
   ========================================================= */

CREATE TABLE InstallmentSchedules (
    ScheduleID INT IDENTITY(1,1) PRIMARY KEY,
    InstallmentID INT NOT NULL,
    PaymentNumber INT NOT NULL,
    DueDate DATE NOT NULL,

    AmountDue DECIMAL(18,2) NOT NULL,
    AmountPaid DECIMAL(18,2) NOT NULL DEFAULT 0,

    RemainingAmount AS
        (AmountDue - AmountPaid) PERSISTED,

    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    PaidDate DATE NULL,

    CONSTRAINT FK_InstallmentSchedules_Contracts
        FOREIGN KEY (InstallmentID)
        REFERENCES InstallmentContracts(InstallmentID),

    CONSTRAINT UQ_InstallmentSchedules_Number
        UNIQUE (InstallmentID, PaymentNumber),

    CONSTRAINT CK_InstallmentSchedules_PaymentNumber
        CHECK (PaymentNumber > 0),

    CONSTRAINT CK_InstallmentSchedules_Amount
        CHECK (
            AmountDue >= 0
            AND AmountPaid >= 0
            AND AmountPaid <= AmountDue
        ),

    CONSTRAINT CK_InstallmentSchedules_Status
        CHECK (
            PaymentStatus IN (
                'UNPAID',
                'PAYING',
                'OVERDUE',
                'COMPLETED'
            )
        )
);
GO


/* =========================================================
   29. INSTALLMENT PAYMENTS
   ========================================================= */

CREATE TABLE InstallmentPayments (
    InstallmentPaymentID INT IDENTITY(1,1) PRIMARY KEY,
    InstallmentID INT NOT NULL,
    ScheduleID INT NULL,

    PaymentDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Amount DECIMAL(18,2) NOT NULL,
    PaymentMethod VARCHAR(20) NOT NULL,
    PaymentStatus VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    ProcessedBy INT NULL,
    DocumentURL VARCHAR(500) NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InstallmentPayments_Contracts
        FOREIGN KEY (InstallmentID)
        REFERENCES InstallmentContracts(InstallmentID),

    CONSTRAINT FK_InstallmentPayments_Schedules
        FOREIGN KEY (ScheduleID)
        REFERENCES InstallmentSchedules(ScheduleID),

    CONSTRAINT FK_InstallmentPayments_Employees
        FOREIGN KEY (ProcessedBy)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_InstallmentPayments_Amount
        CHECK (Amount > 0),

    CONSTRAINT CK_InstallmentPayments_Method
        CHECK (
            PaymentMethod IN (
                'CASH',
                'BANK_TRANSFER'
            )
        ),

    CONSTRAINT CK_InstallmentPayments_Status
        CHECK (
            PaymentStatus IN (
                'PENDING',
                'CONFIRMED',
                'REJECTED'
            )
        )
);
GO


/* =========================================================
   30. INSTALLMENT SETTLEMENTS
   ========================================================= */

CREATE TABLE InstallmentSettlements (
    SettlementID INT IDENTITY(1,1) PRIMARY KEY,
    InstallmentID INT NOT NULL,

    SettlementDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    PrincipalRemaining DECIMAL(18,2) NOT NULL,
    InterestAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    SettlementFee DECIMAL(18,2) NOT NULL DEFAULT 0,
    SettlementAmount DECIMAL(18,2) NOT NULL,

    PaymentMethod VARCHAR(20) NOT NULL,
    ProcessedBy INT NULL,
    DocumentURL VARCHAR(500) NULL,
    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_InstallmentSettlements_Contracts
        FOREIGN KEY (InstallmentID)
        REFERENCES InstallmentContracts(InstallmentID),

    CONSTRAINT FK_InstallmentSettlements_Employees
        FOREIGN KEY (ProcessedBy)
        REFERENCES Employees(EmployeeID),

    CONSTRAINT CK_InstallmentSettlements_Amount
        CHECK (
            PrincipalRemaining >= 0
            AND InterestAmount >= 0
            AND SettlementFee >= 0
            AND SettlementAmount > 0
        ),

    CONSTRAINT CK_InstallmentSettlements_Method
        CHECK (
            PaymentMethod IN (
                'CASH',
                'BANK_TRANSFER'
            )
        )
);
GO


/* =========================================================
   31. SYSTEM LOGS
   ========================================================= */

CREATE TABLE SystemLogs (
    LogID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NULL,

    ActionName VARCHAR(100) NOT NULL,
    TableName VARCHAR(100) NULL,
    RecordID INT NULL,

    ActionDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Description NVARCHAR(1000) NULL,

    CONSTRAINT FK_SystemLogs_Users
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID)
);
GO


/* =========================================================
   32. VEHICLE TCO
   ========================================================= */

CREATE TABLE VehicleTCO (
    TCOID INT IDENTITY(1,1) PRIMARY KEY,
    ModelID INT NOT NULL UNIQUE,

    ElectricityCost DECIMAL(18,2) NOT NULL DEFAULT 0,
    MaintenanceCost DECIMAL(18,2) NOT NULL DEFAULT 0,
    InsuranceCost DECIMAL(18,2) NOT NULL DEFAULT 0,
    RegistrationFee DECIMAL(18,2) NOT NULL DEFAULT 0,
    DiscountAmount DECIMAL(18,2) NOT NULL DEFAULT 0,

    TotalCost AS
    (
        ElectricityCost
        + MaintenanceCost
        + InsuranceCost
        + RegistrationFee
        - DiscountAmount
    ) PERSISTED,

    CONSTRAINT FK_VehicleTCO_CarModels
        FOREIGN KEY (ModelID)
        REFERENCES CarModels(ModelID),

    CONSTRAINT CK_VehicleTCO_Electricity
        CHECK (ElectricityCost >= 0),

    CONSTRAINT CK_VehicleTCO_Maintenance
        CHECK (MaintenanceCost >= 0),

    CONSTRAINT CK_VehicleTCO_Insurance
        CHECK (InsuranceCost >= 0),

    CONSTRAINT CK_VehicleTCO_Registration
        CHECK (RegistrationFee >= 0),

    CONSTRAINT CK_VehicleTCO_Discount
        CHECK (DiscountAmount >= 0)
);
GO


/* =========================================================
   ======================= SAMPLE DATA ======================
   ========================================================= */


/* =========================================================
   USERS
   ========================================================= */

INSERT INTO Users
(
    Username,
    PasswordHash,
    Email,
    Phone,
    Role,
    Status
)
VALUES
('admin','admin123','admin@electriccar.com','0900000001','ADMIN','ACTIVE'),
('sales01','sales123','sales01@electriccar.com','0900000002','SALES','ACTIVE'),
('operation01','operation123','operation@electriccar.com','0900000003','OPERATIONS','ACTIVE'),
('accountant01','accountant123','accountant@electriccar.com','0900000004','ACCOUNTANT','ACTIVE'),
('manager01','manager123','manager@electriccar.com','0900000005','MANAGER','ACTIVE'),
('hoa','hoa123','hoa@gmail.com','0900000010','CUSTOMER','ACTIVE');
GO


/* =========================================================
   CUSTOMERS
   ========================================================= */

INSERT INTO Customers
(
    UserID,
    FullName,
    Phone,
    Email,
    Address
)
VALUES
(
    6,
    N'Đặng Thị Hà Hoa',
    '0900000010',
    'hoa@gmail.com',
    N'Cần Thơ'
);
GO


/* =========================================================
   EMPLOYEES
   ========================================================= */

INSERT INTO Employees
(
    UserID,
    FullName,
    Phone,
    Email,
    Position,
    Status,
    HireDate
)
VALUES
(1,N'Admin System','0900000001','admin@electriccar.com','ADMIN','ACTIVE','2025-01-01'),
(2,N'Nguyễn Văn Sales','0900000002','sales01@electriccar.com','SALES','ACTIVE','2025-01-01'),
(3,N'Trần Văn Operation','0900000003','operation@electriccar.com','OPERATIONS','ACTIVE','2025-01-01'),
(4,N'Lê Thị Accountant','0900000004','accountant@electriccar.com','ACCOUNTANT','ACTIVE','2025-01-01'),
(5,N'Phạm Văn Manager','0900000005','manager@electriccar.com','MANAGER','ACTIVE','2025-01-01');
GO


/* =========================================================
   OTP
   ========================================================= */

INSERT INTO OTPVerifications
(
    UserID,
    OTPCode,
    OTPType,
    ExpiredAt,
    IsVerified
)
VALUES
(
    6,
    '123456',
    'REGISTER',
    DATEADD(MINUTE,5,SYSDATETIME()),
    1
);
GO


/* =========================================================
   BRANDS
   ========================================================= */

INSERT INTO Brands
(
    BrandName,
    Description
)
VALUES
(N'VinFast',N'Vietnamese electric vehicle manufacturer'),
(N'Tesla',N'Electric vehicle manufacturer'),
(N'BYD',N'Chinese electric vehicle manufacturer');
GO


/* =========================================================
   CAR LINES
   ========================================================= */

INSERT INTO CarLines
(
    BrandID,
    LineName,
    Description
)
VALUES
(1,N'VF Series',N'VinFast VF electric vehicle series'),
(2,N'Model Series',N'Tesla Model electric vehicle series'),
(3,N'Atto Series',N'BYD Atto electric vehicle series');
GO


/* =========================================================
   CAR MODELS
   ========================================================= */

INSERT INTO CarModels
(
    BrandID,
    LineID,
    ModelName,
    Description,
    BasePrice
)
VALUES
(1,1,N'VF 3',N'Compact electric car',299000000),
(1,1,N'VF 5',N'Urban electric SUV',529000000),
(1,1,N'VF 6',N'Compact electric SUV',689000000),
(2,2,N'Model 3',N'Electric sedan',999000000),
(3,3,N'Atto 3',N'Compact electric SUV',759000000);
GO


/* =========================================================
   CAR VERSIONS
   ========================================================= */

INSERT INTO CarVersions
(
    ModelID,
    VersionName,
    AdditionalPrice,
    Description
)
VALUES
(1,N'Eco',0,N'VF 3 Eco version'),
(1,N'Plus',30000000,N'VF 3 Plus version'),
(2,N'Plus',0,N'VF 5 Plus version'),
(2,N'Plus Luxury',40000000,N'VF 5 Plus Luxury version'),
(3,N'Base',0,N'VF 6 Base version'),
(3,N'Plus',70000000,N'VF 6 Plus version'),
(4,N'Rear-Wheel Drive',0,N'Tesla Model 3 RWD'),
(5,N'Dynamic',0,N'BYD Atto 3 Dynamic');
GO


/* =========================================================
   COLORS
   ========================================================= */

INSERT INTO Colors
(
    ColorName
)
VALUES
(N'White'),
(N'Black'),
(N'Red'),
(N'Blue'),
(N'Green'),
(N'Gray');
GO


/* =========================================================
   SUPPLIERS
   ========================================================= */

INSERT INTO Suppliers
(
    SupplierName,
    Phone,
    Email,
    Address,
    Status
)
VALUES
(
    N'VinFast Factory',
    '0903000001',
    'vinfast@supplier.com',
    N'Hải Phòng',
    'ACTIVE'
),
(
    N'Tesla Supplier',
    '0903000002',
    'tesla@supplier.com',
    N'California',
    'ACTIVE'
),
(
    N'BYD Supplier',
    '0903000003',
    'byd@supplier.com',
    N'Shenzhen',
    'ACTIVE'
);
GO


/* =========================================================
   CARS
   ========================================================= */

INSERT INTO Cars
(
    CarCode,
    VersionID,
    ColorID,
    VIN,
    EngineNumber,
    Year,
    Price,
    CarStatus,
    Origin,
    BatchCode,
    SupplierID,
    ImportedDate,
    InventoryLocation
)
VALUES
(
    'VF3-DHJH78',
    1,
    1,
    'VINVF3DHJH780001',
    'ENGVF300001',
    2026,
    299000000,
    'DEPOSITED',
    N'Vietnam',
    'VF2026-01',
    1,
    '2026-01-10',
    'SHOWROOM'
),
(
    'VF3-37HDGJ8',
    1,
    2,
    'VINVF337HDGJ80002',
    'ENGVF300002',
    2026,
    299000000,
    'AVAILABLE',
    N'Vietnam',
    'VF2026-01',
    1,
    '2026-01-10',
    'WAREHOUSE'
),
(
    'VF3-98DKL23',
    2,
    3,
    'VINVF398DKL230003',
    'ENGVF300003',
    2026,
    329000000,
    'AVAILABLE',
    N'Vietnam',
    'VF2026-02',
    1,
    '2026-02-05',
    'SHOWROOM'
),
(
    'VF5-A001',
    3,
    1,
    'VINVF5A00100004',
    'ENGVF500004',
    2026,
    529000000,
    'AVAILABLE',
    N'Vietnam',
    'VF2026-02',
    1,
    '2026-02-05',
    'SHOWROOM'
),
(
    'VF5-A002',
    3,
    2,
    'VINVF5A00200005',
    'ENGVF500005',
    2026,
    529000000,
    'AVAILABLE',
    N'Vietnam',
    'VF2026-02',
    1,
    '2026-02-05',
    'WAREHOUSE'
),
(
    'VF6-A001',
    5,
    4,
    'VINVF6A00100006',
    'ENGVF600006',
    2026,
    689000000,
    'AVAILABLE',
    N'Vietnam',
    'VF2026-03',
    1,
    '2026-03-01',
    'SHOWROOM'
),
(
    'M3-T001',
    7,
    6,
    'VINTM3T00100007',
    'ENGM300007',
    2026,
    999000000,
    'AVAILABLE',
    N'United States',
    'TESLA2026-01',
    2,
    '2026-02-15',
    'SHOWROOM'
),
(
    'AT3-B001',
    8,
    1,
    'VINAT3B00100008',
    'ENGAT300008',
    2026,
    759000000,
    'AVAILABLE',
    N'China',
    'BYD2026-01',
    3,
    '2026-03-05',
    'WAREHOUSE'
);
GO


/* =========================================================
   CAR IMAGES
   ========================================================= */

INSERT INTO CarImages
(
    ModelID,
    ImageURL,
    IsMain
)
VALUES
(1,'images/cars/vf3-front.jpg',1),
(1,'images/cars/vf3-side.jpg',0),
(2,'images/cars/vf5-front.jpg',1),
(2,'images/cars/vf5-side.jpg',0),
(3,'images/cars/vf6-front.jpg',1),
(4,'images/cars/model3-front.jpg',1),
(5,'images/cars/atto3-front.jpg',1);
GO


/* =========================================================
   SPECIFICATIONS
   ========================================================= */

INSERT INTO Specifications
(
    ModelID,
    BatteryCapacity,
    RangeKM,
    MaxSpeed,
    SeatingCapacity,
    ChargingTime,

    MotorType,
    MotorCount,
    MotorPowerKW,
    TorqueNM,
    DriveType,
    DrivingModes,
    Acceleration,

    BatteryType,
    BatteryVoltage,
    BatteryManufacturer,
    BatteryWarranty,

    RegenerativeBraking,
    ADAS,
    DashCamera,
    CollisionWarning,
    LaneKeepingAssist,
    AdaptiveCruiseControl,
    ABS,
    ESP,
    BlindSpotWarning,
    Airbags,
    CruiseControl,

    EntertainmentScreen,
    Sensors,
    Camera,
    AudioSystem,
    AppleCarPlay,
    AndroidAuto,
    SteeringWheel,
    Seats,
    AirConditioning,

    Description
)
VALUES

(
    1,
    18.64,
    215,
    100,
    4,
    5.5,

    N'Electric Motor',
    1,
    30,
    110,
    'RWD',
    N'Eco, Normal',
    N'5.3 seconds',

    'LFP',
    200,
    N'VinFast',
    N'8 years',

    1,
    N'Basic ADAS',
    1,
    1,
    1,
    0,
    1,
    1,
    0,
    N'4 airbags',
    1,

    N'10-inch touchscreen',
    N'Rear sensors',
    N'Rear camera',
    N'4 speakers',
    0,
    0,
    N'Multi-function steering wheel',
    N'Fabric seats',
    N'Manual air conditioning',

    N'Compact city electric car'
),

(
    2,
    37.23,
    326,
    130,
    5,
    6.0,

    N'Electric Motor',
    1,
    100,
    135,
    'FWD',
    N'Eco, Normal, Sport',
    N'10.7 seconds',

    'LFP',
    400,
    N'VinFast',
    N'8 years',

    1,
    N'ADAS',
    1,
    1,
    1,
    0,
    1,
    1,
    1,
    N'6 airbags',
    1,

    N'8-inch touchscreen',
    N'Front and rear sensors',
    N'360-degree camera',
    N'6 speakers',
    1,
    1,
    N'Multi-function steering wheel',
    N'Leather seats',
    N'Automatic air conditioning',

    N'Urban electric SUV'
),

(
    3,
    59.60,
    480,
    175,
    5,
    8.0,

    N'Electric Motor',
    1,
    150,
    310,
    'FWD',
    N'Eco, Comfort, Sport',
    N'8.9 seconds',

    'LFP',
    400,
    N'VinFast',
    N'8 years',

    1,
    N'Advanced ADAS',
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    N'8 airbags',
    1,

    N'12.9-inch touchscreen',
    N'Front and rear sensors',
    N'360-degree camera',
    N'8 speakers',
    1,
    1,
    N'Multi-function steering wheel',
    N'Leather seats',
    N'Dual-zone automatic air conditioning',

    N'Compact electric SUV'
),

(
    4,
    57.50,
    513,
    201,
    5,
    7.0,

    N'Permanent Magnet Motor',
    1,
    208,
    420,
    'RWD',
    N'Comfort, Sport',
    N'6.1 seconds',

    'Lithium-ion',
    400,
    N'Panasonic',
    N'8 years',

    1,
    N'Full Self-Driving compatible ADAS',
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    N'8 airbags',
    1,

    N'15-inch touchscreen',
    N'Parking sensors',
    N'360-degree camera',
    N'Premium audio',
    1,
    1,
    N'Multi-function steering wheel',
    N'Leather seats',
    N'Dual-zone automatic air conditioning',

    N'Electric sedan'
),

(
    5,
    50.12,
    410,
    160,
    5,
    7.5,

    N'Permanent Magnet Motor',
    1,
    150,
    310,
    'FWD',
    N'Eco, Normal, Sport',
    N'7.3 seconds',

    'LFP',
    400,
    N'BYD',
    N'8 years',

    1,
    N'ADAS',
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    N'6 airbags',
    1,

    N'12.8-inch touchscreen',
    N'Front and rear sensors',
    N'360-degree camera',
    N'8 speakers',
    1,
    1,
    N'Multi-function steering wheel',
    N'Leather seats',
    N'Dual-zone automatic air conditioning',

    N'Compact electric SUV'
);
GO


/* =========================================================
   CONSULTATION REQUEST
   ========================================================= */

INSERT INTO ConsultationRequests
(
    CustomerID,
    ModelID,
    Phone,
    Message,
    AssignedEmployeeID,
    RequestStatus
)
VALUES
(
    1,
    1,
    '0900000010',
    N'Tôi muốn được tư vấn về VF 3.',
    2,
    'NEW'
);
GO


/* =========================================================
   FINANCIAL COMPANIES
   ========================================================= */

INSERT INTO FinancialCompanies
(
    CompanyName,
    Phone,
    Address
)
VALUES
(
    N'ABC Finance',
    '0901000001',
    N'Hồ Chí Minh'
),
(
    N'XYZ Finance',
    '0901000002',
    N'Hà Nội'
);
GO


/* =========================================================
   INSTALLMENT PLANS
   ========================================================= */

INSERT INTO InstallmentPlans
(
    CompanyID,
    TermMonths,
    DownPaymentPercent,
    InterestRate,
    DocumentRequirement,
    ProcessingFee,
    Status
)
VALUES
-- ABC Finance
(1,12,30,0,N'CCCD/Thẻ CC',0,'ACTIVE'),
(1,24,50,0,N'CCCD/Thẻ CC',0,'ACTIVE'),
(1,36,50,0,N'CCCD/Thẻ CC',0,'ACTIVE'),
(1,48,20,0,N'CCCD/Thẻ CC',0,'ACTIVE'),

-- XYZ Finance
(2,12,30,0,N'CCCD/Thẻ CC',0,'ACTIVE'),
(2,24,10,0,N'CCCD/Thẻ CC',0,'ACTIVE'),
(2,60,30,0,N'CCCD/Thẻ CC',0,'ACTIVE'),
(2,72,20,0,N'CCCD/Thẻ CC',0,'ACTIVE');
GO


/* =========================================================
   PROMOTIONS
   ========================================================= */

INSERT INTO Promotions
(
    PromotionName,
    Description,
    DiscountAmount,
    StartDate,
    EndDate,
    Status
)
VALUES
(
    N'EV Summer Promotion',
    N'Promotion for selected electric car models.',
    10000000,
    '2026-06-01',
    '2026-09-30',
    'ACTIVE'
),
(
    N'New Customer Promotion',
    N'Promotion for first-time customers.',
    5000000,
    '2026-01-01',
    '2026-12-31',
    'ACTIVE'
);
GO


/* =========================================================
   PROMOTION - CAR MODEL
   ========================================================= */

INSERT INTO PromotionCars
(
    PromotionID,
    ModelID
)
VALUES
(1,1),
(1,2),
(1,3),
(2,1),
(2,5);
GO


/* =========================================================
   SAMPLE ORDER
   299M vehicle
   30% deposit = 89.7M
   Remaining = 209.3M
   ========================================================= */

INSERT INTO Orders
(
    CustomerID,
    EmployeeID,
    PaymentMethod,
    OrderStatus,
    TotalAmount,
    DepositAmount,
    RemainingAmount,
    Note
)
VALUES
(
    1,
    2,
    'CASH',
    'DEPOSITED',
    299000000,
    89700000,
    209300000,
    N'Sample order - 30% deposit'
);
GO


/* =========================================================
   ORDER DETAILS
   ========================================================= */

INSERT INTO OrderDetails
(
    OrderID,
    CarID,
    UnitPrice,
    Quantity
)
VALUES
(
    1,
    1,
    299000000,
    1
);
GO


/* =========================================================
   PAYMENT
   ========================================================= */

INSERT INTO Payments
(
    OrderID,
    PaymentType,
    Amount,
    PaymentMethod,
    PaymentStatus,
    ConfirmedBy,
    Note
)
VALUES
(
    1,
    'DEPOSIT',
    89700000,
    'BANK_TRANSFER',
    'CONFIRMED',
    4,
    N'30% deposit'
);
GO


/* =========================================================
   INVENTORY TRANSACTIONS
   ========================================================= */

INSERT INTO InventoryTransactions
(
    CarID,
    TransactionType,
    EmployeeID,
    Note
)
VALUES
(
    1,
    'IMPORT',
    3,
    N'Initial vehicle inventory'
),
(
    1,
    'RESERVE',
    3,
    N'Vehicle reserved for customer order'
),
(
    1,
    'SOLD',
    3,
    N'Sample sales transaction'
);
GO


/* =========================================================
   ORDER STATUS HISTORY
   ========================================================= */

INSERT INTO OrderStatusHistory
(
    OrderID,
    OldStatus,
    NewStatus,
    ChangedBy,
    Note
)
VALUES
(
    1,
    'PENDING',
    'DEPOSITED',
    2,
    N'Customer completed 30% deposit'
);
GO


/* =========================================================
   VEHICLE TCO
   ========================================================= */

INSERT INTO VehicleTCO
(
    ModelID,
    ElectricityCost,
    MaintenanceCost,
    InsuranceCost,
    RegistrationFee,
    DiscountAmount
)
VALUES
(1,15000000,5000000,8000000,3000000,5000000),
(2,20000000,7000000,12000000,5000000,5000000),
(3,25000000,8000000,15000000,6000000,10000000),
(4,30000000,10000000,20000000,8000000,10000000),
(5,28000000,9000000,17000000,7000000,5000000);
GO


/* =========================================================
   SYSTEM LOGS
   ========================================================= */

INSERT INTO SystemLogs
(
    UserID,
    ActionName,
    TableName,
    RecordID,
    Description
)
VALUES
(
    2,
    'CREATE_ORDER',
    'Orders',
    1,
    N'Sales employee created sample order'
),
(
    4,
    'CONFIRM_PAYMENT',
    'Payments',
    1,
    N'Accountant confirmed customer deposit'
),
(
    2,
    'CHANGE_ORDER_STATUS',
    'Orders',
    1,
    N'Order status changed from PENDING to DEPOSITED'
);
GO


/* =========================================================
   INSTALLMENT CONTRACT SAMPLE
   =========================================================
   Plan 1:
   ABC Finance
   12 months
   30% down payment
   ========================================================= */

/*
   This is a sample installment contract.
   It is kept separate from Order 1 because Order 1 is CASH.
*/

INSERT INTO Orders
(
    CustomerID,
    EmployeeID,
    PaymentMethod,
    OrderStatus,
    TotalAmount,
    DepositAmount,
    RemainingAmount,
    Note
)
VALUES
(
    1,
    2,
    'INSTALLMENT',
    'DEPOSITED',
    299000000,
    89700000,
    209300000,
    N'Sample installment order'
);
GO


INSERT INTO OrderDetails
(
    OrderID,
    CarID,
    UnitPrice,
    Quantity
)
VALUES
(
    2,
    2,
    299000000,
    1
);
GO


INSERT INTO InstallmentContracts
(
    OrderID,
    CompanyID,
    PlanID,
    DownPayment,
    LoanAmount,
    LoanTermMonths,
    InterestRate,
    Status
)
SELECT
    2,
    CompanyID,
    PlanID,
    89700000,
    209300000,
    TermMonths,
    InterestRate,
    'PENDING'
FROM InstallmentPlans
WHERE PlanID = 1;
GO


/* =========================================================
   TEST QUERIES
   ========================================================= */


/* 1. COUNT TABLES */

SELECT COUNT(*) AS TotalTables
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE';
GO


/* 2. LIST ALL TABLES */

SELECT
    ROW_NUMBER() OVER (ORDER BY TABLE_NAME) AS No,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO


/* 3. VEHICLE INFORMATION */

SELECT
    c.CarID,
    c.CarCode,
    b.BrandName,
    cl.LineName,
    cm.ModelName,
    cv.VersionName,
    co.ColorName,
    c.VIN,
    c.EngineNumber,
    c.Year,
    c.Price,
    c.CarStatus,
    c.Origin,
    c.BatchCode,
    s.SupplierName,
    c.InventoryLocation
FROM Cars c
JOIN CarVersions cv
    ON c.VersionID = cv.VersionID
JOIN CarModels cm
    ON cv.ModelID = cm.ModelID
JOIN CarLines cl
    ON cm.LineID = cl.LineID
JOIN Brands b
    ON cm.BrandID = b.BrandID
JOIN Colors co
    ON c.ColorID = co.ColorID
LEFT JOIN Suppliers s
    ON c.SupplierID = s.SupplierID
ORDER BY c.CarID;
GO


/* 4. AVAILABLE CARS */

SELECT *
FROM Cars
WHERE CarStatus = 'AVAILABLE';
GO


/* 5. ORDER INFORMATION */

SELECT
    o.OrderID,
    cu.FullName AS CustomerName,
    e.FullName AS EmployeeName,
    o.OrderDate,
    o.PaymentMethod,
    o.OrderStatus,
    o.TotalAmount,
    o.DepositAmount,
    o.RemainingAmount
FROM Orders o
JOIN Customers cu
    ON o.CustomerID = cu.CustomerID
LEFT JOIN Employees e
    ON o.EmployeeID = e.EmployeeID
ORDER BY o.OrderID DESC;
GO


/* 6. PAYMENT */

SELECT *
FROM Payments
ORDER BY PaymentID DESC;
GO


/* 7. CAR STATUS COUNT */

SELECT
    CarStatus,
    COUNT(*) AS TotalCars
FROM Cars
GROUP BY CarStatus;
GO


/* 8. TOTAL CONFIRMED PAYMENT */

SELECT
    SUM(Amount) AS TotalConfirmedPayment
FROM Payments
WHERE PaymentStatus = 'CONFIRMED';
GO


/* 9. INSTALLMENT PLANS */

SELECT
    ip.PlanID,
    fc.CompanyName,
    ip.TermMonths,
    ip.DownPaymentPercent,
    ip.InterestRate,
    ip.DocumentRequirement,
    ip.ProcessingFee,
    ip.Status
FROM InstallmentPlans ip
JOIN FinancialCompanies fc
    ON ip.CompanyID = fc.CompanyID
ORDER BY fc.CompanyName, ip.TermMonths;
GO


/* 10. INSTALLMENT CONTRACT */

SELECT
    ic.InstallmentID,
    ic.OrderID,
    fc.CompanyName,
    ip.PlanID,
    ip.TermMonths,
    ic.DownPayment,
    ic.LoanAmount,
    ic.LoanTermMonths,
    ic.InterestRate,
    ic.Status
FROM InstallmentContracts ic
JOIN FinancialCompanies fc
    ON ic.CompanyID = fc.CompanyID
JOIN InstallmentPlans ip
    ON ic.PlanID = ip.PlanID;
GO


/* 11. VEHICLE TCO */

SELECT
    vt.TCOID,
    cm.ModelName,
    vt.ElectricityCost,
    vt.MaintenanceCost,
    vt.InsuranceCost,
    vt.RegistrationFee,
    vt.DiscountAmount,
    vt.TotalCost
FROM VehicleTCO vt
JOIN CarModels cm
    ON vt.ModelID = cm.ModelID
ORDER BY cm.ModelName;
GO


/* 12. ORDER STATUS HISTORY */

SELECT
    h.HistoryID,
    h.OrderID,
    h.OldStatus,
    h.NewStatus,
    u.Username AS ChangedBy,
    h.ChangedDate,
    h.Note
FROM OrderStatusHistory h
LEFT JOIN Users u
    ON h.ChangedBy = u.UserID
ORDER BY h.HistoryID;
GO


/* 13. SYSTEM LOG */

SELECT
    sl.LogID,
    u.Username,
    sl.ActionName,
    sl.TableName,
    sl.RecordID,
    sl.ActionDate,
    sl.Description
FROM SystemLogs sl
LEFT JOIN Users u
    ON sl.UserID = u.UserID
ORDER BY sl.LogID;
GO


PRINT '=====================================================';
PRINT 'ElectricCarSalesDB FINAL DATABASE CREATED SUCCESSFULLY';
PRINT 'Total tables: 32';
PRINT '=====================================================';
GO
-- Run after ElectricCarSalesDB.sql. Adds schema used by administration, promotions and reports.
USE ElectricCarSalesDB;
GO

IF DB_NAME() <> N'ElectricCarSalesDB'
BEGIN
    PRINT N'Apply ElectricCarSalesDB.sql first.';
    SET NOEXEC ON;
END;
GO

SET XACT_ABORT ON;

INSERT INTO dbo.PaymentTypes(TypeName, Description)
SELECT 'INSTALLMENT', N'Installment payment'
WHERE NOT EXISTS (SELECT 1 FROM dbo.PaymentTypes WHERE TypeName = 'INSTALLMENT');
GO

IF OBJECT_ID(N'dbo.InstallmentPlans', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InstallmentPlans (
        PlanID  INT IDENTITY(1, 1) PRIMARY KEY,
        CompanyID INT NULL,
        PlanName NVARCHAR(100) NOT NULL UNIQUE,
        LoanTermMonths INT NOT NULL,
        InterestRate DECIMAL(5, 2) NOT NULL,
        Description NVARCHAR(500) NULL
    );
END;
GO

IF COL_LENGTH(N'dbo.Orders', N'CompanyID') IS NULL
    ALTER TABLE dbo.Orders ADD CompanyID INT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.Orders')
      AND name = N'FK_Orders_FinancialCompanies'
)
    ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT FK_Orders_FinancialCompanies
        FOREIGN KEY (CompanyID) REFERENCES dbo.FinancialCompanies(CompanyID);
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
  InstallmentSettlements
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

-- Keep audit records immutable when writing directly through SQL, too.
IF OBJECT_ID(N'dbo.TR_SystemLogs_Immutable', N'TR') IS NULL
    EXEC(N'CREATE TRIGGER dbo.TR_SystemLogs_Immutable
        ON dbo.SystemLogs
        INSTEAD OF UPDATE, DELETE
        AS
        BEGIN
            THROW 51001, N''System logs are immutable.'', 1;
        END;');
GO

-- Finance integration: add optional policy fields without changing existing prices or rates.
-- A legacy plan is assigned only when its existing contracts identify one lender.
BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH(N'dbo.InstallmentPlans', N'CompanyID') IS NULL
        EXEC(N'ALTER TABLE dbo.InstallmentPlans ADD CompanyID INT NULL');
    IF COL_LENGTH(N'dbo.FinancialCompanies', N'PaymentDueDay') IS NULL
        EXEC(N'ALTER TABLE dbo.FinancialCompanies ADD PaymentDueDay TINYINT NULL');
    IF COL_LENGTH(N'dbo.FinancialCompanies', N'PaymentCutoffTime') IS NULL
        EXEC(N'ALTER TABLE dbo.FinancialCompanies ADD PaymentCutoffTime TIME(0) NULL');
    IF COL_LENGTH(N'dbo.FinancialCompanies', N'LateFeeDailyPercent') IS NULL
        EXEC(N'ALTER TABLE dbo.FinancialCompanies ADD LateFeeDailyPercent DECIMAL(5, 2) NULL');
    IF COL_LENGTH(N'dbo.FinancialCompanies', N'EarlySettlementFeePercent') IS NULL
        EXEC(N'ALTER TABLE dbo.FinancialCompanies ADD EarlySettlementFeePercent DECIMAL(5, 2) NULL');

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.FinancialCompanies') AND name = N'CK_FinancialCompanies_DueDay')
        EXEC(N'ALTER TABLE dbo.FinancialCompanies WITH CHECK ADD CONSTRAINT CK_FinancialCompanies_DueDay
            CHECK (PaymentDueDay IS NULL OR PaymentDueDay BETWEEN 1 AND 31)');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.FinancialCompanies') AND name = N'CK_FinancialCompanies_LateFee')
        EXEC(N'ALTER TABLE dbo.FinancialCompanies WITH CHECK ADD CONSTRAINT CK_FinancialCompanies_LateFee
            CHECK (LateFeeDailyPercent IS NULL OR LateFeeDailyPercent >= 0)');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.FinancialCompanies') AND name = N'CK_FinancialCompanies_SettlementFee')
        EXEC(N'ALTER TABLE dbo.FinancialCompanies WITH CHECK ADD CONSTRAINT CK_FinancialCompanies_SettlementFee
            CHECK (EarlySettlementFeePercent IS NULL OR EarlySettlementFeePercent >= 0)');

    EXEC(N'UPDATE p SET CompanyID = lenders.CompanyID
        FROM dbo.InstallmentPlans p
        JOIN (
            SELECT PlanID, MIN(CompanyID) AS CompanyID
            FROM dbo.InstallmentContracts
            GROUP BY PlanID
            HAVING COUNT(DISTINCT CompanyID) = 1
        ) lenders ON lenders.PlanID = p.PlanID
        JOIN dbo.FinancialCompanies fc ON fc.CompanyID = lenders.CompanyID
        WHERE p.CompanyID IS NULL');

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
        WHERE parent_object_id = OBJECT_ID(N'dbo.InstallmentPlans')
          AND name = N'FK_InstallmentPlans_FinancialCompanies')
        EXEC(N'ALTER TABLE dbo.InstallmentPlans WITH CHECK ADD CONSTRAINT FK_InstallmentPlans_FinancialCompanies
            FOREIGN KEY (CompanyID) REFERENCES dbo.FinancialCompanies(CompanyID)');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

SET NOEXEC OFF;
GO

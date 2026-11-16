-- Optional feature tables for disposable H2 tests.
CREATE TABLE Promotions (
        PromotionID INT IDENTITY(1, 1) PRIMARY KEY,
        PromotionName NVARCHAR(150) NOT NULL,
        Description NVARCHAR(500) NULL,
        DiscountType VARCHAR(30) NULL,
        DiscountValue DECIMAL(18, 2) NULL,
        StartDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        EndDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Status VARCHAR(30)
    );

CREATE TABLE PromotionCars (
        PromotionCarID INT IDENTITY(1, 1) PRIMARY KEY,
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

CREATE TABLE SystemLogs (
        LogID BIGINT IDENTITY(1, 1) PRIMARY KEY,
        UserID INT NULL,
        Action VARCHAR(50) NOT NULL,
        TargetType VARCHAR(50) NOT NULL,
        TargetID VARCHAR(100) NULL,
        Detail NVARCHAR(MAX) NOT NULL,
        CreatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_SystemLogs_Users
            FOREIGN KEY(UserID) REFERENCES Users(UserID)
    );

CREATE TABLE RolePermissions (
        RoleID INT NOT NULL,
        PermissionCode VARCHAR(50) NOT NULL,
        CONSTRAINT PK_RolePermissions
            PRIMARY KEY(RoleID, PermissionCode),
        CONSTRAINT FK_RolePermissions_Roles
            FOREIGN KEY(RoleID) REFERENCES Roles(RoleID),
        CONSTRAINT CK_RolePermissions_Code
            CHECK(PermissionCode IN ('MANAGE_USERS', 'MANAGE_PROMOTIONS', 'VIEW_REPORTS', 'VIEW_LOGS'))
    );

CREATE TABLE SystemConfigurations (
        ConfigKey VARCHAR(50) PRIMARY KEY,
        ConfigValue VARCHAR(100) NOT NULL,
        UpdatedBy INT NULL REFERENCES Users(UserID),
        UpdatedDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT CK_SystemConfigurations_Key
            CHECK(ConfigKey IN ('SESSION_TIMEOUT_MINUTES', 'REPORT_MAX_DAYS'))
    );

INSERT INTO RolePermissions(RoleID, PermissionCode)
        SELECT RoleID, 'VIEW_REPORTS' FROM Roles WHERE RoleName IN ('MANAGER', 'ACCOUNTANT');

INSERT INTO SystemConfigurations(ConfigKey, ConfigValue) VALUES('SESSION_TIMEOUT_MINUTES', '30'), ('REPORT_MAX_DAYS', '3660');

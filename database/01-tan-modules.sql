-- Additive migration. Run against the existing ElectricCarSalesDB; never drops business data.
USE ElectricCarSalesDB;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;

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
COMMIT TRANSACTION;
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

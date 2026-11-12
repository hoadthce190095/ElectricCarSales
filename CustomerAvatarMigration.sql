USE ElectricCarSalesDB;
GO

IF COL_LENGTH(N'dbo.Customers', N'AvatarData') IS NULL
    ALTER TABLE dbo.Customers ADD AvatarData VARBINARY(MAX) NULL;
GO

IF COL_LENGTH(N'dbo.Customers', N'AvatarContentType') IS NULL
    ALTER TABLE dbo.Customers ADD AvatarContentType VARCHAR(30) NULL;
GO

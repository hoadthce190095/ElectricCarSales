/*
  Apply to an existing ElectricCarSalesDB without dropping application data.
  Rates below are demo APRs interpolated from the ranges in the RDS discussion;
  replace them with lender-approved rates before creating real customer contracts.
*/
USE ElectricCarSalesDB;
GO

IF COL_LENGTH(N'dbo.FinancialCompanies', N'PaymentDueDay') IS NULL
    ALTER TABLE dbo.FinancialCompanies ADD PaymentDueDay TINYINT NULL;
IF COL_LENGTH(N'dbo.FinancialCompanies', N'PaymentCutoffTime') IS NULL
    ALTER TABLE dbo.FinancialCompanies ADD PaymentCutoffTime TIME(0) NULL;
IF COL_LENGTH(N'dbo.FinancialCompanies', N'LateFeeDailyPercent') IS NULL
    ALTER TABLE dbo.FinancialCompanies ADD LateFeeDailyPercent DECIMAL(5,2) NULL;
IF COL_LENGTH(N'dbo.FinancialCompanies', N'EarlySettlementFeePercent') IS NULL
    ALTER TABLE dbo.FinancialCompanies ADD EarlySettlementFeePercent DECIMAL(5,2) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_FinancialCompanies_DueDay')
    ALTER TABLE dbo.FinancialCompanies ADD CONSTRAINT CK_FinancialCompanies_DueDay
        CHECK (PaymentDueDay IS NULL OR PaymentDueDay BETWEEN 1 AND 31);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_FinancialCompanies_LateFee')
    ALTER TABLE dbo.FinancialCompanies ADD CONSTRAINT CK_FinancialCompanies_LateFee
        CHECK (LateFeeDailyPercent IS NULL OR LateFeeDailyPercent >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_FinancialCompanies_SettlementFee')
    ALTER TABLE dbo.FinancialCompanies ADD CONSTRAINT CK_FinancialCompanies_SettlementFee
        CHECK (EarlySettlementFeePercent IS NULL OR EarlySettlementFeePercent >= 0);
GO

MERGE dbo.FinancialCompanies AS target
USING (VALUES
    (N'Shinhan Finance', 5, CAST('18:00' AS TIME(0)), CAST(10.00 AS DECIMAL(5,2))),
    (N'Techcombank', 7, CAST('18:00' AS TIME(0)), CAST(3.00 AS DECIMAL(5,2))),
    (N'MB Bank', 9, CAST('18:00' AS TIME(0)), CAST(5.00 AS DECIMAL(5,2))),
    (N'VPBank', 10, CAST('18:00' AS TIME(0)), CAST(7.00 AS DECIMAL(5,2)))
) AS source(CompanyName, PaymentDueDay, PaymentCutoffTime, LateFeeDailyPercent)
ON target.CompanyName = source.CompanyName
WHEN MATCHED THEN UPDATE SET PaymentDueDay = source.PaymentDueDay,
    PaymentCutoffTime = source.PaymentCutoffTime, LateFeeDailyPercent = source.LateFeeDailyPercent
WHEN NOT MATCHED THEN INSERT (CompanyName, Description, PaymentDueDay, PaymentCutoffTime, LateFeeDailyPercent)
    VALUES (source.CompanyName, N'Chính sách trả góp demo; cần đối chiếu hợp đồng tài chính.',
            source.PaymentDueDay, source.PaymentCutoffTime, source.LateFeeDailyPercent);
GO

IF COL_LENGTH(N'dbo.InstallmentPlans', N'CompanyID') IS NULL
    ALTER TABLE dbo.InstallmentPlans ADD CompanyID INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_InstallmentPlans_FinancialCompanies')
    ALTER TABLE dbo.InstallmentPlans ADD CONSTRAINT FK_InstallmentPlans_FinancialCompanies
        FOREIGN KEY (CompanyID) REFERENCES dbo.FinancialCompanies(CompanyID);
GO

/* Flat-interest quote: principal × annual APR × term months / 12. */
WITH RateRows AS (
    SELECT * FROM (VALUES
        (N'Shinhan Finance', N'Shinhan Finance - 12 tháng', 12, 7.00),
        (N'Shinhan Finance', N'Shinhan Finance - 24 tháng', 24, 8.25),
        (N'Shinhan Finance', N'Shinhan Finance - 36 tháng', 36, 9.50),
        (N'Shinhan Finance', N'Shinhan Finance - 48 tháng', 48, 10.75),
        (N'Shinhan Finance', N'Shinhan Finance - 60 tháng', 60, 12.00),
        (N'Techcombank', N'Techcombank - 24 tháng', 24, 5.00),
        (N'Techcombank', N'Techcombank - 36 tháng', 36, 5.67),
        (N'Techcombank', N'Techcombank - 48 tháng', 48, 6.33),
        (N'Techcombank', N'Techcombank - 60 tháng', 60, 7.00),
        (N'MB Bank', N'MB Bank - 12 tháng', 12, 5.00),
        (N'MB Bank', N'MB Bank - 24 tháng', 24, 5.88),
        (N'MB Bank', N'MB Bank - 36 tháng', 36, 6.75),
        (N'MB Bank', N'MB Bank - 48 tháng', 48, 7.63),
        (N'MB Bank', N'MB Bank - 60 tháng', 60, 8.50),
        (N'VPBank', N'VPBank - 36 tháng', 36, 6.00),
        (N'VPBank', N'VPBank - 48 tháng', 48, 7.75),
        (N'VPBank', N'VPBank - 60 tháng', 60, 9.50)
    ) v(CompanyName, PlanName, LoanTermMonths, InterestRate)
)
INSERT INTO dbo.InstallmentPlans (CompanyID, PlanName, LoanTermMonths, InterestRate, Description)
SELECT fc.CompanyID, r.PlanName, r.LoanTermMonths, r.InterestRate,
       N'APR demo; thay bằng biểu lãi suất được đơn vị tài chính xác nhận.'
FROM RateRows r
JOIN dbo.FinancialCompanies fc ON fc.CompanyName = r.CompanyName
WHERE NOT EXISTS (SELECT 1 FROM dbo.InstallmentPlans p WHERE p.PlanName = r.PlanName);
GO

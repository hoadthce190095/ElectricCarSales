/*
 * Du lieu MO PHONG cho bao cao cua Pham Tran Phuc Tan.
 * Khong phai giao dich, khach hang, nhan vien hay khuyen mai that cua VinFast.
 * Chay sau ElectricCarSalesDB.sql o thu muc goc va 02-vinfast-catalogue.sql.
 * Script chi them ban ghi DEMO va doi trang thai cac xe DEMO do script 02 tao.
 * Khong tao Admin. Tai khoan DEMO INACTIVE va khong co mat khau dang nhap hop le.
 */
-- Nap du lieu mo phong vao database chung cua nhom khi can trinh bay.
USE ElectricCarSalesDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    -- Khong tron giao dich mo phong vao mot showroom da co giao dich that.
    IF EXISTS (SELECT 1 FROM Orders WHERE Note IS NULL OR Note NOT LIKE N'DEMO-REPORT-%')
       OR EXISTS (SELECT 1 FROM Payments WHERE ReferenceCode IS NULL OR ReferenceCode NOT LIKE 'DEMO-PAY-%')
       OR EXISTS (
           SELECT 1 FROM InstallmentContracts ic JOIN Orders o ON o.OrderID = ic.OrderID
           WHERE o.Note IS NULL OR o.Note NOT LIKE N'DEMO-REPORT-%'
       )
        THROW 51020, N'Database da co giao dich khong phai DEMO. Khong nap du lieu bao cao mo phong.', 1;

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
        THROW 51025, N'Thieu xe DEMO. Hay chay 02-vinfast-catalogue.sql truoc.', 1;

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

-- Danh mục VinFast dùng cho bài báo cáo Phạm Trần Phúc Tấn.
-- Kiểm tra nguồn chính thức ngày 05/10/2026, chi tiết ở docs/NGUON_DU_LIEU_VINFAST.md.
-- Chọn database trước khi chạy. Không đặt USE để có thể dùng database demo riêng.
-- Script chỉ thêm dữ liệu còn thiếu, không sửa/xóa danh mục hoặc giao dịch sẵn có.
-- Xe có mã DEMO/VIN giả lập phục vụ học tập, không phải tồn kho của VinFast.
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.CarModels', N'U') IS NULL OR OBJECT_ID(N'dbo.Cars', N'U') IS NULL
    THROW 51000, N'Chưa có schema xe. Chạy 00-create-database.sql trước.', 1;

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

    INSERT INTO CarVersions(ModelID, VersionName, Description)
    SELECT m.ModelID, c.VersionName, N'Giá MSRP kèm pin và VAT, chính sách 15/07/2026. Trang bị tùy cấu hình.'
    FROM @Catalogue c
    INNER JOIN CarLines l ON l.BrandID = @BrandID AND l.CarLineName = c.CarLineName
    INNER JOIN CarModels m ON m.CarLineID = l.CarLineID AND m.ModelName = c.ModelName
    WHERE NOT EXISTS (
        SELECT 1 FROM CarVersions v WHERE v.ModelID = m.ModelID AND v.VersionName = c.VersionName
    );

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
        INNER JOIN CarVersions v ON v.ModelID = m.ModelID AND v.VersionName = c.VersionName
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

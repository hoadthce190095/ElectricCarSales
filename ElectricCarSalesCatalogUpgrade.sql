USE ElectricCarSalesDB;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.CarVersions', 'CatalogPrice') IS NULL
    ALTER TABLE dbo.CarVersions ADD CatalogPrice DECIMAL(18,2) NULL;
IF COL_LENGTH('dbo.CarVersions', 'RangeStandard') IS NULL
    ALTER TABLE dbo.CarVersions ADD RangeStandard NVARCHAR(20) NULL;
IF COL_LENGTH('dbo.CarVersions', 'SourceURL') IS NULL
    ALTER TABLE dbo.CarVersions ADD SourceURL NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.CarVersions', 'PriceSourceURL') IS NULL
    ALTER TABLE dbo.CarVersions ADD PriceSourceURL NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.CarVersions', 'PriceNote') IS NULL
    ALTER TABLE dbo.CarVersions ADD PriceNote NVARCHAR(1000) NULL;
IF COL_LENGTH('dbo.CarVersions', 'RapidChargeMinutes') IS NULL
    ALTER TABLE dbo.CarVersions ADD RapidChargeMinutes INT NULL;
IF COL_LENGTH('dbo.ModelColors', 'ColorImageURL') IS NULL
    ALTER TABLE dbo.ModelColors ADD ColorImageURL NVARCHAR(500) NULL;
GO

UPDATE dbo.Colors SET ColorName = NCHAR(272) + NCHAR(7887)
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0xC4009000E100BB008F00;
UPDATE dbo.Colors SET ColorName = NCHAR(272) + N'en'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0xC400900065006E00;
UPDATE dbo.Colors SET ColorName = N'B' + NCHAR(7841) + N'c'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x4200E100BA00A1006300;
UPDATE dbo.Colors SET ColorName = N'H' + NCHAR(7891) + N'ng'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x4800E100BB001C206E006700;
UPDATE dbo.Colors SET ColorName = N'Tr' + NCHAR(7855) + N'ng'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x54007200E100BA00AF006E006700;
UPDATE dbo.Colors SET ColorName = N'V' + NCHAR(224) + N'ng'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x5600C300A0006E006700;
UPDATE dbo.Colors SET ColorName = N'X' + NCHAR(225) + N'm'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x5800C300A1006D00;
UPDATE dbo.Colors SET ColorName = N'Xanh d' + NCHAR(432) + NCHAR(417) + N'ng'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x580061006E00680020006400C600B000C600A1006E006700;
UPDATE dbo.Colors SET ColorName = N'Xanh d' + NCHAR(432) + NCHAR(417) + N'ng'
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x580061006E00680020006400B0016F006E006700;
UPDATE dbo.Colors SET ColorName = N'Xanh l' + NCHAR(225)
WHERE CONVERT(VARBINARY(MAX), ColorName) = 0x580061006E00680020006C00C300A100;

DECLARE @DuplicateColors TABLE (
    DuplicateColorID INT NOT NULL PRIMARY KEY,
    CanonicalColorID INT NOT NULL
);
INSERT INTO @DuplicateColors (DuplicateColorID, CanonicalColorID)
SELECT duplicate.ColorID, MIN(canonical.ColorID)
FROM dbo.Colors duplicate
INNER JOIN dbo.Colors canonical ON canonical.ColorName = duplicate.ColorName
WHERE canonical.ColorID < duplicate.ColorID
GROUP BY duplicate.ColorID;

DELETE duplicateModelColor
FROM dbo.ModelColors duplicateModelColor
INNER JOIN @DuplicateColors duplicate ON duplicate.DuplicateColorID = duplicateModelColor.ColorID
WHERE EXISTS (
    SELECT 1
    FROM dbo.ModelColors canonicalModelColor
    WHERE canonicalModelColor.ModelID = duplicateModelColor.ModelID
      AND canonicalModelColor.ColorID = duplicate.CanonicalColorID
);

UPDATE modelColor
SET ColorID = duplicate.CanonicalColorID
FROM dbo.ModelColors modelColor
INNER JOIN @DuplicateColors duplicate ON duplicate.DuplicateColorID = modelColor.ColorID;

DELETE color
FROM dbo.Colors color
INNER JOIN @DuplicateColors duplicate ON duplicate.DuplicateColorID = color.ColorID;

UPDATE cv
SET CatalogPrice = prices.CatalogPrice,
    RangeStandard = prices.RangeStandard,
    SourceURL = prices.SourceURL,
    PriceSourceURL = N'https://static-cms-prod.vinfastauto.com/202607015_thong-bao-chinh-sach-gia-ban-cac-dong-xe-vinfast-thang-07.2026_0.pdf',
    PriceNote = N'Giá tham khảo theo dữ liệu đối chiếu 05/10/2026; chưa bao gồm chi phí lăn bánh và ưu đãi riêng.'
FROM dbo.CarVersions cv
INNER JOIN dbo.CarModels model ON model.ModelID = cv.ModelID
INNER JOIN dbo.CarLines line ON line.CarLineID = model.CarLineID
CROSS APPLY (
    SELECT
        CASE model.ModelName
            WHEN N'VF 3' THEN 285000000
            WHEN N'VF 5 Plus' THEN 496000000
            WHEN N'VF 6 Eco' THEN 646000000
            WHEN N'VF 6 Plus' THEN 699000000
            WHEN N'VF 7 Eco' THEN 740000000
            WHEN N'VF 7 Plus' THEN 830000000
            WHEN N'VF 8 Eco' THEN 898000000
            WHEN N'VF 8 Plus' THEN 1079000000
            WHEN N'VF 9 Eco' THEN 1348000000
            WHEN N'VF 9 Plus' THEN 1529000000
        END AS CatalogPrice,
        CASE WHEN line.CarLineName = N'VF 9' THEN N'WLTP' ELSE N'NEDC' END AS RangeStandard,
        CASE line.CarLineName
            WHEN N'VF 3' THEN N'https://vinfastauto.com/vn_vi/dat-coc-xe-dien-vf3'
            WHEN N'VF 5' THEN N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf5'
            WHEN N'VF 6' THEN N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf6'
            WHEN N'VF 7' THEN N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf7'
            WHEN N'VF 8' THEN N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf8'
            WHEN N'VF 9' THEN N'https://vinfastauto.com/vn_vi/dat-coc-xe-vf9'
        END AS SourceURL
) prices
WHERE prices.CatalogPrice IS NOT NULL;

DECLARE @CatalogColors TABLE (
    ModelName NVARCHAR(100) NOT NULL,
    ColorName NVARCHAR(50) NOT NULL
);
INSERT INTO @CatalogColors (ModelName, ColorName)
VALUES
    (N'VF 3', N'Trắng'), (N'VF 3', N'Xám'), (N'VF 3', N'Đỏ'), (N'VF 3', N'Vàng'),
    (N'VF 3', N'Xanh dương'), (N'VF 3', N'Hồng'), (N'VF 3', N'Xanh lá'),
    (N'VF 5 Plus', N'Trắng'), (N'VF 5 Plus', N'Xám'), (N'VF 5 Plus', N'Đỏ'),
    (N'VF 5 Plus', N'Vàng'), (N'VF 5 Plus', N'Xanh dương'), (N'VF 5 Plus', N'Xanh lá'),
    (N'VF 6 Eco', N'Trắng'), (N'VF 6 Eco', N'Xám'), (N'VF 6 Eco', N'Đen'),
    (N'VF 6 Eco', N'Đỏ'), (N'VF 6 Eco', N'Xanh lá'),
    (N'VF 6 Plus', N'Trắng'), (N'VF 6 Plus', N'Xám'), (N'VF 6 Plus', N'Đen'),
    (N'VF 6 Plus', N'Đỏ'), (N'VF 6 Plus', N'Xanh lá'),
    (N'VF 7 Eco', N'Trắng'), (N'VF 7 Eco', N'Xám'), (N'VF 7 Eco', N'Đen'),
    (N'VF 7 Eco', N'Đỏ'), (N'VF 7 Eco', N'Xanh lá'),
    (N'VF 7 Plus', N'Trắng'), (N'VF 7 Plus', N'Xám'), (N'VF 7 Plus', N'Đen'),
    (N'VF 7 Plus', N'Đỏ'), (N'VF 7 Plus', N'Xanh lá'),
    (N'VF 8 Eco', N'Trắng'), (N'VF 8 Eco', N'Đen'), (N'VF 8 Eco', N'Đỏ'), (N'VF 8 Eco', N'Xanh lá'),
    (N'VF 8 Plus', N'Trắng'), (N'VF 8 Plus', N'Đen'), (N'VF 8 Plus', N'Đỏ'), (N'VF 8 Plus', N'Xanh lá'),
    (N'VF 9 Eco', N'Trắng'), (N'VF 9 Eco', N'Xám'), (N'VF 9 Eco', N'Đen'),
    (N'VF 9 Eco', N'Đỏ'), (N'VF 9 Eco', N'Xanh lá'), (N'VF 9 Eco', N'Bạc'),
    (N'VF 9 Plus', N'Trắng'), (N'VF 9 Plus', N'Xám'), (N'VF 9 Plus', N'Đen'),
    (N'VF 9 Plus', N'Đỏ'), (N'VF 9 Plus', N'Xanh lá'), (N'VF 9 Plus', N'Bạc');

INSERT INTO dbo.Colors (ColorName)
SELECT DISTINCT catalogColors.ColorName
FROM @CatalogColors catalogColors
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Colors existing WHERE existing.ColorName = catalogColors.ColorName
);

INSERT INTO dbo.ModelColors (ModelID, ColorID)
SELECT model.ModelID, color.ColorID
FROM @CatalogColors catalogColors
INNER JOIN dbo.CarModels model ON model.ModelName = catalogColors.ModelName
INNER JOIN dbo.CarLines line ON line.CarLineID = model.CarLineID
INNER JOIN dbo.Brands brand ON brand.BrandID = line.BrandID AND brand.BrandName = N'VinFast'
INNER JOIN dbo.Colors color ON color.ColorName = catalogColors.ColorName
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.ModelColors existing
    WHERE existing.ModelID = model.ModelID AND existing.ColorID = color.ColorID
);

DECLARE @CatalogColorImages TABLE (
    ModelName NVARCHAR(100) NOT NULL,
    ColorName NVARCHAR(50) NOT NULL,
    ImageURL NVARCHAR(500) NOT NULL
);
INSERT INTO @CatalogColorImages (ModelName, ColorName, ImageURL)
VALUES
    (N'VF 3', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-trang-mobile.webp'),
    (N'VF 3', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-xam-mobile.webp'),
    (N'VF 3', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-do-mobile.webp'),
    (N'VF 3', N'Vàng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-vang-mobile.webp'),
    (N'VF 3', N'Xanh dương', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-xanh-duong-mobile.webp'),
    (N'VF 3', N'Hồng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-hong-mobile.webp'),
    (N'VF 3', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf3/hinh-anh-vinfast-vf3-ngoai-that-mau-xanh-urban-mobile.webp'),
    (N'VF 5 Plus', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/hinh-anh-vinfast-vf5-ngoai-that-mau-trang-mobile.webp'),
    (N'VF 5 Plus', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/hinh-anh-vinfast-vf5-ngoai-that-mau-xam-mobile.webp'),
    (N'VF 5 Plus', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/hinh-anh-vinfast-vf5-ngoai-that-mau-do-mobile.webp'),
    (N'VF 5 Plus', N'Vàng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/hinh-anh-vinfast-vf5-ngoai-that-mau-vang-mobile.webp'),
    (N'VF 5 Plus', N'Xanh dương', N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/hinh-anh-vinfast-vf5-ngoai-that-mau-xanh-duong-mobile.webp'),
    (N'VF 5 Plus', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf5/hinh-anh-vinfast-vf5-ngoai-that-mau-xanh-urban-mobile.webp'),
    (N'VF 6 Eco', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-trang-mobile.webp'),
    (N'VF 6 Eco', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-bac-mobile.webp'),
    (N'VF 6 Eco', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-den-mobile.webp'),
    (N'VF 6 Eco', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-do-mobile.webp'),
    (N'VF 6 Eco', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-xanh-reu-mobile.webp'),
    (N'VF 6 Plus', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-trang-mobile.webp'),
    (N'VF 6 Plus', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-bac-mobile.webp'),
    (N'VF 6 Plus', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-den-mobile.webp'),
    (N'VF 6 Plus', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-do-mobile.webp'),
    (N'VF 6 Plus', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf6/hinh-anh-xe-vinfast-vf6-plus-mau-xanh-reu-mobile.webp'),
    (N'VF 7 Eco', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-trang-mobile.webp'),
    (N'VF 7 Eco', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-xam-mobile.webp'),
    (N'VF 7 Eco', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-den-mobile.webp'),
    (N'VF 7 Eco', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-do-mobile.webp'),
    (N'VF 7 Eco', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-xanh-mint-mobile.webp'),
    (N'VF 7 Plus', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-trang-mobile.webp'),
    (N'VF 7 Plus', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-xam-mobile.webp'),
    (N'VF 7 Plus', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-den-mobile.webp'),
    (N'VF 7 Plus', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-do-mobile.webp'),
    (N'VF 7 Plus', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf7/hinh-anh-vinfast-vf7-mau-xanh-mint-mobile.webp'),
    (N'VF 8 Eco', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-trang-mobile-01.webp'),
    (N'VF 8 Eco', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-den-mobile-01.webp'),
    (N'VF 8 Eco', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-do-mobile-01.webp'),
    (N'VF 8 Eco', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-xanh-reu-mobile-01.webp'),
    (N'VF 8 Plus', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-trang-mobile-01.webp'),
    (N'VF 8 Plus', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-den-mobile-01.webp'),
    (N'VF 8 Plus', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-do-mobile-01.webp'),
    (N'VF 8 Plus', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/VF-8/hinh-anh-xe-VinFast-vf8-ngoai-that-mau-xanh-reu-mobile-01.webp'),
    (N'VF 9 Eco', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-infinity-blanc-mobile-20260915.webp'),
    (N'VF 9 Eco', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-zenith-grey-mobile.webp'),
    (N'VF 9 Eco', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-jet-black-mobile.webp'),
    (N'VF 9 Eco', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-solar-ruby-mobile.webp'),
    (N'VF 9 Eco', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-urban-mint-mobile.webp'),
    (N'VF 9 Eco', N'Bạc', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-desat-silver-mobile-20260915.webp'),
    (N'VF 9 Plus', N'Trắng', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-infinity-blanc-mobile-20260915.webp'),
    (N'VF 9 Plus', N'Xám', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-zenith-grey-mobile.webp'),
    (N'VF 9 Plus', N'Đen', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-jet-black-mobile.webp'),
    (N'VF 9 Plus', N'Đỏ', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-solar-ruby-mobile.webp'),
    (N'VF 9 Plus', N'Xanh lá', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-urban-mint-mobile.webp'),
    (N'VF 9 Plus', N'Bạc', N'https://static-cms-prod.vinfastauto.com/statics/car/vf9/hinh-anh-vinfast-vf9-desat-silver-mobile-20260915.webp');

UPDATE modelColor
SET ColorImageURL = catalogImage.ImageURL
FROM dbo.ModelColors modelColor
INNER JOIN dbo.CarModels model ON model.ModelID = modelColor.ModelID
INNER JOIN dbo.Colors color ON color.ColorID = modelColor.ColorID
INNER JOIN @CatalogColorImages catalogImage
    ON catalogImage.ModelName = model.ModelName AND catalogImage.ColorName = color.ColorName
WHERE modelColor.ColorImageURL IS NULL OR modelColor.ColorImageURL <> catalogImage.ImageURL;

COMMIT TRANSACTION;
GO

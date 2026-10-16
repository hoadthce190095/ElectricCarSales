# ElectricCarSales
Group project for SWP391 - Electric Car Sales System.

## Vehicle catalogue database update

The public vehicle catalogue, comparison page, and vehicle details read catalogue data from the existing `ElectricCarSalesDB`.
For an existing database, run `ElectricCarSalesCatalogUpgrade.sql` in SQL Server Management Studio before starting the application. It is safe to rerun, repairs color names affected by a legacy SQLCMD encoding mismatch, and fills the official VinFast image URL for each supported model/color combination in `ModelColors.ColorImageURL`. When running the UTF-8 script with `sqlcmd`, specify `-f i:65001,o:65001`.
This additive script updates the existing database and does not create or drop a database.

`ElectricCarSalesDB.sql` includes the same catalogue columns and seed data for a fresh local setup. It recreates the database, so do not use it to update an existing database.

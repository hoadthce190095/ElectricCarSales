# setup-electriccarsales-structure.ps1
# Run this script from: D:\SWP391 Project\ElectricCarSales
# It ONLY creates missing folders/files. It does not overwrite existing files.

$ErrorActionPreference = "Stop"

function New-DirIfMissing($path) {
    if (-not (Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path | Out-Null
        Write-Host "Created folder: $path"
    }
}

function New-FileIfMissing($path, $content = "") {
    if (-not (Test-Path -LiteralPath $path)) {
        $parent = Split-Path -Parent $path
        if ($parent) { New-DirIfMissing $parent }
        Set-Content -LiteralPath $path -Value $content -Encoding UTF8
        Write-Host "Created file: $path"
    } else {
        Write-Host "Skipped existing: $path"
    }
}

# =========================================================
# 1. Java packages
# =========================================================
$java = "src\main\java\com\electriccarsales"

$packages = @(
    "$java\controller\auth",
    "$java\controller\vehicle",
    "$java\controller\comparison",
    "$java\controller\consultation",
    "$java\controller\customer",
    "$java\controller\order",
    "$java\controller\payment",
    "$java\controller\installment",
    "$java\controller\contract",
    "$java\controller\invoice",
    "$java\controller\inventory",
    "$java\controller\staff",
    "$java\controller\manager",
    "$java\controller\admin",

    "$java\service\auth",
    "$java\service\vehicle",
    "$java\service\comparison",
    "$java\service\consultation",
    "$java\service\customer",
    "$java\service\order",
    "$java\service\payment",
    "$java\service\installment",
    "$java\service\contract",
    "$java\service\invoice",
    "$java\service\inventory",
    "$java\service\report",
    "$java\service\admin",
    "$java\service\ai",
    "$java\service\logging",

    "$java\repository\auth",
    "$java\repository\vehicle",
    "$java\repository\comparison",
    "$java\repository\consultation",
    "$java\repository\customer",
    "$java\repository\order",
    "$java\repository\payment",
    "$java\repository\installment",
    "$java\repository\contract",
    "$java\repository\invoice",
    "$java\repository\inventory",
    "$java\repository\report",
    "$java\repository\admin",

    "$java\model\auth",
    "$java\model\vehicle",
    "$java\model\comparison",
    "$java\model\consultation",
    "$java\model\customer",
    "$java\model\order",
    "$java\model\payment",
    "$java\model\installment",
    "$java\model\contract",
    "$java\model\invoice",
    "$java\model\inventory",
    "$java\model\report",
    "$java\model\admin",

    "$java\configuration"
)

foreach ($p in $packages) { New-DirIfMissing $p }

# =========================================================
# 2. Web folders
# =========================================================
$web = "src\main\webapp"

$webFolders = @(
    "$web\assets\css",
    "$web\assets\js",
    "$web\assets\images",
    "$web\common",

    "$web\auth",

    "$web\vehicle",
    "$web\vehicle\comparison",

    "$web\consultation",

    "$web\customer",
    "$web\customer\orders",
    "$web\customer\payment",
    "$web\customer\installment",
    "$web\customer\contract",
    "$web\customer\invoice",
    "$web\customer\ai",

    "$web\staff",
    "$web\staff\customers",
    "$web\staff\consultation",
    "$web\staff\orders",
    "$web\staff\vehicles",
    "$web\staff\inventory",
    "$web\staff\contracts",
    "$web\staff\chat",

    "$web\manager",
    "$web\manager\vehicles",
    "$web\manager\inventory",
    "$web\manager\sales",
    "$web\manager\customers",
    "$web\manager\reports",

    "$web\admin",
    "$web\admin\users",
    "$web\admin\roles",
    "$web\admin\configuration",
    "$web\admin\logs",

    "$web\WEB-INF"
)

foreach ($p in $webFolders) { New-DirIfMissing $p }

# =========================================================
# 3. Common JSP placeholders
# =========================================================
New-FileIfMissing "$web\common\navbar.jsp" '<%-- Shared navigation placeholder. Implement after authentication/session is ready. --%>'
New-FileIfMissing "$web\common\footer.jsp" '<%-- Shared footer placeholder. --%>'
New-FileIfMissing "$web\common\customer-sidebar.jsp" '<%-- Customer sidebar placeholder. --%>'
New-FileIfMissing "$web\common\staff-sidebar.jsp" '<%-- Staff sidebar placeholder. --%>'
New-FileIfMissing "$web\common\manager-sidebar.jsp" '<%-- Manager sidebar placeholder. --%>'
New-FileIfMissing "$web\common\admin-sidebar.jsp" '<%-- Admin sidebar placeholder. --%>'

# =========================================================
# 4. Public/Auth JSP placeholders
# =========================================================
New-FileIfMissing "$web\index.jsp" '<%-- Existing index.jsp is preserved if present. --%>'
New-FileIfMissing "$web\auth\login.jsp" '<%-- Login screen --%>'
New-FileIfMissing "$web\auth\register.jsp" '<%-- Customer registration screen --%>'
New-FileIfMissing "$web\auth\otp.jsp" '<%-- OTP verification screen --%>'
New-FileIfMissing "$web\auth\forgot-password.jsp" '<%-- Password reset screen --%>'

# =========================================================
# 5. Public vehicle/consultation JSP placeholders
# =========================================================
New-FileIfMissing "$web\vehicle\vehicle-list.jsp" '<%-- Public vehicle list/search/filter --%>'
New-FileIfMissing "$web\vehicle\vehicle-detail.jsp" '<%-- Vehicle detail --%>'
New-FileIfMissing "$web\vehicle\vehicle-comparison.jsp" '<%-- Vehicle comparison --%>'
New-FileIfMissing "$web\consultation\consultation.jsp" '<%-- Consultation request --%>'

# =========================================================
# 6. Customer JSP placeholders
# =========================================================
New-FileIfMissing "$web\customer\dashboard.jsp" '<%-- Customer dashboard --%>'
New-FileIfMissing "$web\customer\profile.jsp" '<%-- Customer profile --%>'
New-FileIfMissing "$web\customer\consultation.jsp" '<%-- Customer consultation history/detail --%>'
New-FileIfMissing "$web\customer\orders.jsp" '<%-- Customer orders --%>'
New-FileIfMissing "$web\customer\order-detail.jsp" '<%-- Customer order detail --%>'
New-FileIfMissing "$web\customer\payment\payment.jsp" '<%-- Customer payment --%>'
New-FileIfMissing "$web\customer\installment\installment.jsp" '<%-- Customer installment --%>'
New-FileIfMissing "$web\customer\contract\contract.jsp" '<%-- Customer contract --%>'
New-FileIfMissing "$web\customer\invoice\invoice.jsp" '<%-- Customer invoice --%>'
New-FileIfMissing "$web\customer\ai\ai-chat.jsp" '<%-- Customer AI Assistant --%>'

# =========================================================
# 7. Staff JSP placeholders
# =========================================================
New-FileIfMissing "$web\staff\dashboard.jsp" '<%-- Staff dashboard --%>'
New-FileIfMissing "$web\staff\customers\customer-list.jsp" '<%-- Staff customer management list --%>'
New-FileIfMissing "$web\staff\customers\customer-detail.jsp" '<%-- Staff customer detail --%>'
New-FileIfMissing "$web\staff\consultation\consultation-list.jsp" '<%-- Staff consultation list --%>'
New-FileIfMissing "$web\staff\consultation\consultation-detail.jsp" '<%-- Staff consultation detail --%>'
New-FileIfMissing "$web\staff\orders\order-list.jsp" '<%-- Staff transaction/order management --%>'
New-FileIfMissing "$web\staff\orders\order-detail.jsp" '<%-- Staff transaction detail --%>'
New-FileIfMissing "$web\staff\vehicles\vehicle-list.jsp" '<%-- Staff vehicle view --%>'
New-FileIfMissing "$web\staff\inventory\inventory-list.jsp" '<%-- Staff inventory view/operations according to permission --%>'
New-FileIfMissing "$web\staff\contracts\contract-list.jsp" '<%-- Staff contract management --%>'
New-FileIfMissing "$web\staff\chat\chat.jsp" '<%-- Staff/customer chat UI placeholder. Realtime chat implementation is not assumed yet. --%>'

# =========================================================
# 8. Manager JSP placeholders
# =========================================================
New-FileIfMissing "$web\manager\dashboard.jsp" '<%-- Manager dashboard --%>'
New-FileIfMissing "$web\manager\vehicles\vehicle-list.jsp" '<%-- Manager vehicle management list --%>'
New-FileIfMissing "$web\manager\vehicles\vehicle-create.jsp" '<%-- Manager create vehicle --%>'
New-FileIfMissing "$web\manager\vehicles\vehicle-edit.jsp" '<%-- Manager edit vehicle --%>'
New-FileIfMissing "$web\manager\vehicles\vehicle-detail.jsp" '<%-- Manager vehicle detail --%>'
New-FileIfMissing "$web\manager\inventory\inventory-list.jsp" '<%-- Manager inventory --%>'
New-FileIfMissing "$web\manager\sales\sales-list.jsp" '<%-- Manager sales/transactions --%>'
New-FileIfMissing "$web\manager\customers\customer-list.jsp" '<%-- Manager customer overview --%>'
New-FileIfMissing "$web\manager\reports\revenue-report.jsp" '<%-- Revenue Report --%>'
New-FileIfMissing "$web\manager\reports\sales-report.jsp" '<%-- Sales Report --%>'
New-FileIfMissing "$web\manager\reports\vehicle-sales-report.jsp" '<%-- Vehicle Sales Report --%>'
New-FileIfMissing "$web\manager\reports\inventory-report.jsp" '<%-- Inventory Report --%>'
New-FileIfMissing "$web\manager\reports\transaction-status-report.jsp" '<%-- Transaction Status Report --%>'

# =========================================================
# 9. Admin JSP placeholders
# =========================================================
New-FileIfMissing "$web\admin\dashboard.jsp" '<%-- Admin dashboard --%>'
New-FileIfMissing "$web\admin\users\user-list.jsp" '<%-- Admin user management list --%>'
New-FileIfMissing "$web\admin\users\user-detail.jsp" '<%-- Admin user detail --%>'
New-FileIfMissing "$web\admin\users\user-create.jsp" '<%-- Admin create user --%>'
New-FileIfMissing "$web\admin\roles\role-permission.jsp" '<%-- Admin role/permission management --%>'
New-FileIfMissing "$web\admin\configuration\system-configuration.jsp" '<%-- Admin system configuration --%>'
New-FileIfMissing "$web\admin\logs\system-log.jsp" '<%-- Admin system logs --%>'
New-FileIfMissing "$web\admin\logs\activity-log-detail.jsp" '<%-- Admin activity log detail --%>'

# =========================================================
# 10. CSS/JS placeholders
# =========================================================
New-FileIfMissing "$web\assets\css\style.css" '/* Global Electric Car Sales styles */'
New-FileIfMissing "$web\assets\css\home.css" '/* Home page styles */'
New-FileIfMissing "$web\assets\css\auth.css" '/* Authentication page styles */'
New-FileIfMissing "$web\assets\css\vehicle.css" '/* Vehicle browsing styles */'
New-FileIfMissing "$web\assets\css\dashboard.css" '/* Dashboard/sidebar styles */'
New-FileIfMissing "$web\assets\css\customer.css" '/* Customer UI styles */'
New-FileIfMissing "$web\assets\css\staff.css" '/* Staff UI styles */'
New-FileIfMissing "$web\assets\css\manager.css" '/* Manager UI styles */'
New-FileIfMissing "$web\assets\css\admin.css" '/* Admin UI styles */'
New-FileIfMissing "$web\assets\css\comparison.css" '/* Vehicle comparison styles */'

New-FileIfMissing "$web\assets\js\main.js" '// Global JavaScript'
New-FileIfMissing "$web\assets\js\vehicle.js" '// Vehicle list/detail JavaScript'
New-FileIfMissing "$web\assets\js\comparison.js" '// Vehicle comparison JavaScript'

# =========================================================
# 11. Safe Java controller/service/repository skeletons
#     Only create files if they do not already exist.
# =========================================================
$javaClasses = @{
    "$java\controller\auth\LoginController.java" = "package com.electriccarsales.controller.auth;`n`npublic class LoginController {`n}"
    "$java\controller\auth\RegisterController.java" = "package com.electriccarsales.controller.auth;`n`npublic class RegisterController {`n}"
    "$java\controller\auth\LogoutController.java" = "package com.electriccarsales.controller.auth;`n`npublic class LogoutController {`n}"
    "$java\controller\auth\OtpController.java" = "package com.electriccarsales.controller.auth;`n`npublic class OtpController {`n}"

    "$java\controller\vehicle\VehicleController.java" = "package com.electriccarsales.controller.vehicle;`n`npublic class VehicleController {`n}"
    "$java\controller\vehicle\VehicleDetailController.java" = "package com.electriccarsales.controller.vehicle;`n`npublic class VehicleDetailController {`n}"
    "$java\controller\vehicle\VehicleSearchController.java" = "package com.electriccarsales.controller.vehicle;`n`npublic class VehicleSearchController {`n}"
    "$java\controller\comparison\ComparisonController.java" = "package com.electriccarsales.controller.comparison;`n`npublic class ComparisonController {`n}"
    "$java\controller\consultation\ConsultationController.java" = "package com.electriccarsales.controller.consultation;`n`npublic class ConsultationController {`n}"
    "$java\controller\customer\CustomerController.java" = "package com.electriccarsales.controller.customer;`n`npublic class CustomerController {`n}"
    "$java\controller\order\OrderController.java" = "package com.electriccarsales.controller.order;`n`npublic class OrderController {`n}"
    "$java\controller\payment\PaymentController.java" = "package com.electriccarsales.controller.payment;`n`npublic class PaymentController {`n}"
    "$java\controller\installment\InstallmentController.java" = "package com.electriccarsales.controller.installment;`n`npublic class InstallmentController {`n}"
    "$java\controller\contract\ContractController.java" = "package com.electriccarsales.controller.contract;`n`npublic class ContractController {`n}"
    "$java\controller\invoice\InvoiceController.java" = "package com.electriccarsales.controller.invoice;`n`npublic class InvoiceController {`n}"
    "$java\controller\inventory\InventoryController.java" = "package com.electriccarsales.controller.inventory;`n`npublic class InventoryController {`n}"
    "$java\controller\staff\StaffController.java" = "package com.electriccarsales.controller.staff;`n`npublic class StaffController {`n}"
    "$java\controller\manager\ManagerController.java" = "package com.electriccarsales.controller.manager;`n`npublic class ManagerController {`n}"
    "$java\controller\admin\AdminController.java" = "package com.electriccarsales.controller.admin;`n`npublic class AdminController {`n}"

    "$java\configuration\AuthorizationConfig.java" = "package com.electriccarsales.configuration;`n`npublic class AuthorizationConfig {`n}"
}

foreach ($entry in $javaClasses.GetEnumerator()) {
    New-FileIfMissing $entry.Key $entry.Value
}

Write-Host ""
Write-Host "========================================================="
Write-Host "ElectricCarSales structure setup completed."
Write-Host "Existing files were preserved."
Write-Host "IMPORTANT: The created Java classes are skeletons only."
Write-Host "Do not implement business logic until the existing team code is reviewed."
Write-Host "========================================================="

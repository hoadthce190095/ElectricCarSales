package vn.edu.fpt.electriccarsales.services;

import vn.edu.fpt.electriccarsales.dto.EmployeeSalesReport;
import vn.edu.fpt.electriccarsales.dto.ModelSalesReport;
import vn.edu.fpt.electriccarsales.dto.MonthlyReport;
import vn.edu.fpt.electriccarsales.dto.ReportData;
import vn.edu.fpt.electriccarsales.dto.ReportFilter;
import vn.edu.fpt.electriccarsales.dto.ReportPeriod;
import vn.edu.fpt.electriccarsales.dto.ReportSummary;
import vn.edu.fpt.electriccarsales.dto.SaleReport;
import vn.edu.fpt.electriccarsales.dto.SaleVehicle;
import vn.edu.fpt.electriccarsales.dto.StatusTotal;
import vn.edu.fpt.electriccarsales.repositories.ReportRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ReportService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final ReportRepository reportRepository;
    private final Clock clock;

    public ReportService() {
        this(new ReportRepository(), Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public ReportService(ReportRepository reportRepository, Clock clock) {
        this.reportRepository = reportRepository;
        this.clock = clock;
    }

    public ReportData generate(ReportFilter filter) throws SQLException {
        filter.validate();
        LocalDate today = LocalDate.now(clock);
        ReportRepository.Snapshot snapshot = reportRepository.read(filter, today);
        Map<Integer, List<SaleVehicle>> vehiclesByOrder = groupVehiclesByOrder(snapshot.getVehicles());

        Map<YearMonth, Totals> monthlyTotals = new TreeMap<>();
        Map<String, Totals> orderStatusTotals = new TreeMap<>();
        Map<String, Totals> paymentStatusTotals = new TreeMap<>();
        Map<Integer, Totals> modelTotals = new TreeMap<>();
        Map<Integer, String> modelNames = new HashMap<>();
        Map<Integer, Totals> employeeTotals = new TreeMap<>();
        Map<Integer, String> employeeNames = new HashMap<>();
        List<SaleReport> sales = new ArrayList<>();
        Set<Integer> soldCarIds = new HashSet<>();

        BigDecimal revenue = ZERO;
        BigDecimal salesAmount = ZERO;
        BigDecimal outstandingAmount = ZERO;
        BigDecimal discountAmount = ZERO;
        BigDecimal taxAmount = ZERO;
        long completedOrders = 0;
        long cancelledOrders = 0;
        long completedPayments = 0;
        long contracts = 0;
        long inconsistentOrders = 0;

        for (ReportRepository.OrderRow order : snapshot.getOrders()) {
            List<SaleVehicle> vehicles = vehiclesByOrder.get(order.getOrderId());
            if (vehicles == null) {
                vehicles = List.of();
            }

            // Tiền chưa thu không được âm, kể cả khi khách hàng đã trả dư.
            BigDecimal remainingAmount = order.getTotalAmount().subtract(order.getPaidAmount()).max(ZERO);
            sales.add(createSaleReport(order, vehicles, remainingAmount));

            // Danh sách trạng thái vẫn hiển thị đơn bị hủy hoặc có chứng từ sai.
            Totals orderStatus = getOrCreateTotals(orderStatusTotals, order.getStatus());
            orderStatus.count++;
            orderStatus.amount = orderStatus.amount.add(order.getTotalAmount());
            YearMonth orderMonth = YearMonth.from(order.getOrderDate());
            Totals month = getOrCreateTotals(monthlyTotals, orderMonth);
            month.count++;

            if ("CANCELLED".equals(order.getStatus())) {
                cancelledOrders++;
                continue;
            }
            if (!order.isEligible()) {
                inconsistentOrders++;
                continue;
            }

            outstandingAmount = outstandingAmount.add(remainingAmount);
            if (order.getContractNumber() != null) {
                contracts++;
            }
            if ("COMPLETED".equals(order.getStatus())) {
                completedOrders++;
            }

            // Giá trị bán hàng chỉ tính đơn đã thanh toán hoặc đã hoàn tất.
            boolean paidOrder = "PAID".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus());
            if (!paidOrder) {
                continue;
            }
            salesAmount = salesAmount.add(order.getTotalAmount());
            discountAmount = discountAmount.add(order.getDiscountAmount());
            taxAmount = taxAmount.add(order.getTaxAmount());
            month.salesAmount = month.salesAmount.add(order.getTotalAmount());

            int employeeId = 0;
            if (order.getEmployeeId() != null) {
                employeeId = order.getEmployeeId();
            }
            Totals employee = getOrCreateTotals(employeeTotals, employeeId);
            employeeNames.put(employeeId, getEmployeeName(order));
            employee.count++;
            employee.salesAmount = employee.salesAmount.add(order.getTotalAmount());

            for (SaleVehicle vehicle : vehicles) {
                // Dùng mã xe để mỗi xe chỉ được đếm một lần trong báo cáo.
                if (soldCarIds.contains(vehicle.getCarId())) {
                    continue;
                }
                soldCarIds.add(vehicle.getCarId());
                month.vehiclesSold++;
                employee.vehiclesSold++;

                Totals model = getOrCreateTotals(modelTotals, vehicle.getModelId());
                modelNames.put(vehicle.getModelId(), vehicle.getModelName());
                model.vehiclesSold++;
                // Giá bán lấy từ chi tiết đơn, không lấy giá niêm yết hiện tại của xe.
                model.salesAmount = model.salesAmount.add(vehicle.getSalesPrice());
            }
        }

        // Tiền thực thu tính theo ngày thanh toán, khác với kỳ bán hàng theo ngày tạo đơn.
        for (ReportRepository.PaymentRow payment : snapshot.getPayments()) {
            Totals paymentStatus = getOrCreateTotals(paymentStatusTotals, payment.getStatus());
            paymentStatus.count++;
            paymentStatus.amount = paymentStatus.amount.add(payment.getAmount());

            if (payment.isEligible() && "COMPLETED".equals(payment.getStatus())) {
                revenue = revenue.add(payment.getAmount());
                completedPayments++;
                YearMonth paymentMonth = YearMonth.from(payment.getPaymentDate());
                Totals month = getOrCreateTotals(monthlyTotals, paymentMonth);
                month.amount = month.amount.add(payment.getAmount());
            }
        }

        // Khoản thu trả góp được trình bày riêng để tránh cộng trùng với Payments.
        ReportRepository.ExtraTotals extras = snapshot.getExtras();
        ReportSummary summary = new ReportSummary(revenue, salesAmount, soldCarIds.size(), snapshot.getOrders().size(),
            completedOrders, cancelledOrders, outstandingAmount, completedPayments, contracts, discountAmount,
            taxAmount, extras.getInstallmentContracts(), extras.getOverdueAmount(), extras.getInstallmentReceipts(),
            inconsistentOrders);

        return new ReportData(new ReportPeriod(filter.getFrom(), filter.getTo()), LocalDateTime.now(clock),
            createDefinitions(sales, snapshot.isContainsDemo()), summary, createMonthlyReports(monthlyTotals),
            createModelSalesReports(modelTotals, modelNames), createEmployeeSalesReports(employeeTotals, employeeNames),
            createStatusTotals(orderStatusTotals), createStatusTotals(paymentStatusTotals),
            List.copyOf(snapshot.getInventory()), List.copyOf(sales));
    }

    private Map<Integer, List<SaleVehicle>> groupVehiclesByOrder(List<ReportRepository.VehicleRow> rows) {
        Map<Integer, List<SaleVehicle>> vehiclesByOrder = new HashMap<>();
        for (ReportRepository.VehicleRow row : rows) {
            List<SaleVehicle> vehicles = vehiclesByOrder.get(row.getOrderId());
            if (vehicles == null) {
                vehicles = new ArrayList<>();
                vehiclesByOrder.put(row.getOrderId(), vehicles);
            }
            vehicles.add(row.getVehicle());
        }
        return vehiclesByOrder;
    }

    private SaleReport createSaleReport(ReportRepository.OrderRow order, List<SaleVehicle> vehicles,
        BigDecimal remainingAmount) {
        return new SaleReport(order.getOrderId(), order.getOrderDate(), order.getStatus(), order.getCustomerName(),
            getEmployeeName(order), order.getTotalAmount(), order.getPaidAmount(), remainingAmount,
            order.getContractNumber(), order.getInvoiceNumber(), order.getDeliveryStatus(), order.isEligible(),
            List.copyOf(vehicles));
    }

    private String getEmployeeName(ReportRepository.OrderRow order) {
        if (order.getEmployeeName() == null) {
            return "Chưa phân công";
        }
        return order.getEmployeeName();
    }

    private <K> Totals getOrCreateTotals(Map<K, Totals> groups, K key) {
        Totals totals = groups.get(key);
        if (totals == null) {
            totals = new Totals();
            groups.put(key, totals);
        }
        return totals;
    }

    private List<MonthlyReport> createMonthlyReports(Map<YearMonth, Totals> monthlyTotals) {
        List<MonthlyReport> reports = new ArrayList<>();
        for (Map.Entry<YearMonth, Totals> entry : monthlyTotals.entrySet()) {
            Totals totals = entry.getValue();
            reports.add(new MonthlyReport(entry.getKey().toString(), totals.amount, totals.salesAmount,
                totals.vehiclesSold, totals.count));
        }
        return List.copyOf(reports);
    }

    private List<ModelSalesReport> createModelSalesReports(Map<Integer, Totals> modelTotals,
        Map<Integer, String> modelNames) {
        List<ModelSalesReport> reports = new ArrayList<>();
        for (Map.Entry<Integer, Totals> entry : modelTotals.entrySet()) {
            int modelId = entry.getKey();
            Totals totals = entry.getValue();
            reports
                .add(new ModelSalesReport(modelId, modelNames.get(modelId), totals.vehiclesSold, totals.salesAmount));
        }
        return List.copyOf(reports);
    }

    private List<EmployeeSalesReport> createEmployeeSalesReports(Map<Integer, Totals> employeeTotals,
        Map<Integer, String> employeeNames) {
        List<EmployeeSalesReport> reports = new ArrayList<>();
        for (Map.Entry<Integer, Totals> entry : employeeTotals.entrySet()) {
            int employeeKey = entry.getKey();
            Integer employeeId = null;
            if (employeeKey != 0) {
                employeeId = employeeKey;
            }
            Totals totals = entry.getValue();
            reports.add(new EmployeeSalesReport(employeeId, employeeNames.get(employeeKey), totals.count,
                totals.vehiclesSold, totals.salesAmount));
        }
        return List.copyOf(reports);
    }

    private List<StatusTotal> createStatusTotals(Map<String, Totals> statusTotals) {
        List<StatusTotal> reports = new ArrayList<>();
        for (Map.Entry<String, Totals> entry : statusTotals.entrySet()) {
            Totals totals = entry.getValue();
            reports.add(new StatusTotal(entry.getKey(), totals.count, totals.amount));
        }
        return List.copyOf(reports);
    }

    private Map<String, String> createDefinitions(List<SaleReport> sales, boolean containsDemo) {
        Map<String, String> definitions = new LinkedHashMap<>();
        // Khoản thu của đơn ngoài kỳ và tồn kho hiện tại cũng có thể chứa dữ liệu DEMO.
        for (SaleReport sale : sales) {
            for (SaleVehicle vehicle : sale.getVehicles()) {
                if (vehicle.getVin() != null && vehicle.getVin().startsWith("DEMO")) {
                    containsDemo = true;
                    break;
                }
            }
            if (containsDemo) {
                break;
            }
        }
        if (containsDemo) {
            definitions.put("demo", "Báo cáo có dữ liệu DEMO mô phỏng phục vụ bài tập. Các giao dịch DEMO không"
                + " phải doanh thu thực tế của VinFast.");
        }
        definitions.put("revenue", "Tiền thực thu: chỉ Payments COMPLETED trong khoảng PaymentDate, của đơn hợp lệ và"
            + " chưa hủy. Đây là chỉ tiêu thu tiền, không phải chính sách ghi nhận doanh" + " thu kế toán.");
        definitions.put("salesAmount",
            "Tổng Orders.TotalAmount của đơn PAID/COMPLETED theo OrderDate; mỗi đơn tính một"
                + " lần. Không tính đơn có hợp đồng/hóa đơn DRAFT, VOID, CANCELLED hoặc sai"
                + " khách hàng/tổng tiền. Đơn chưa có hợp đồng/hóa đơn vẫn hiển thị theo dữ" + " liệu Orders.");
        definitions.put("vehiclesSold",
            "CarID phân biệt trong OrderDetails của đơn PAID/COMPLETED hợp lệ; mỗi xe tính một" + " lần.");
        definitions.put("modelSalesAmount",
            "Tổng giá bán lịch sử OrderDetails.SalesPrice theo mẫu xe, trước điều chỉnh giảm"
                + " giá/thuế ở cấp đơn; có thể khác tổng giá trị đơn.");
        definitions.put("outstandingAmount",
            "Tổng phần chưa thu của đơn hợp lệ theo OrderDate đã chọn, trừ toàn bộ Payments"
                + " COMPLETED hiện có, kể cả ngoài kỳ; số âm được chặn về 0.");
        definitions.put("installmentReceipts",
            "Tiền InstallmentPayments COMPLETED theo PaymentDate được trình bày riêng, không"
                + " cộng vào revenue/paidAmount để tránh trùng với Payments khi module khác ghi"
                + " nhận cùng khoản thu ở cả hai bảng.");
        definitions.put("overdueAmount", "Phần lịch trả góp đến hạn trước ngày hiện tại chưa được trả bằng"
            + " InstallmentPayments COMPLETED; dùng trạng thái hiện tại cho các đơn trong" + " kỳ đã chọn.");
        definitions.put("inventory",
            "Ảnh chụp tồn kho hiện tại theo CarStatuses, không phải tồn kho lịch sử cuối kỳ;" + " lọc modelId nếu có.");
        definitions.put("filters",
            "employeeId/paymentMethodId lọc đơn; modelId chọn toàn bộ đơn chứa mẫu xe đó."
                + " PaymentDate quyết định kỳ thu tiền, OrderDate quyết định kỳ bán hàng. Tất"
                + " cả ngày bắt đầu/kết thúc đều được tính.");
        return Map.copyOf(definitions);
    }

    private static final class Totals {
        private long count;
        private long vehiclesSold;
        private BigDecimal amount = ZERO;
        private BigDecimal salesAmount = ZERO;
    }
}

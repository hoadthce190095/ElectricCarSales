package vn.edu.fpt.electriccarsales.services;

import vn.edu.fpt.electriccarsales.dto.AIConstraints;
import vn.edu.fpt.electriccarsales.dto.AIRequest;
import vn.edu.fpt.electriccarsales.dto.AIResponse;
import vn.edu.fpt.electriccarsales.dto.VehicleRecommendation;
import vn.edu.fpt.electriccarsales.entities.VehicleModel;
import vn.edu.fpt.electriccarsales.entities.VehicleSpecifications;
import vn.edu.fpt.electriccarsales.repositories.AICatalogue;
import vn.edu.fpt.electriccarsales.repositories.AIRepository;
import vn.edu.fpt.electriccarsales.util.ApiException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tư vấn bằng quy tắc dựa trên danh mục xe. Chưa kết nối dịch vụ AI bên ngoài. Service chỉ đọc
 * thông tin sản phẩm, không xử lý giao dịch hoặc dữ liệu tài chính.
 */
public final class AIService {
    public static final String MODE = "CATALOG_RULES";
    private static final Set<String> INTENTS = Set.of("AUTO", "PRODUCT_QA", "COMPARE", "RECOMMEND", "NEEDS_ANALYSIS");
    private static final String NUMBER = "(-?\\d+(?:[.,]\\d+)*)";
    private static final Pattern MONEY = Pattern
        .compile("(?<![a-z0-9])" + NUMBER + "\\s*(ty|ti|trieu|tr|million|billion|vnd|dong)(?![a-z])");
    private static final Pattern BUDGET = Pattern
        .compile("(?:ngan sach|budget|toi da|duoi)\\s*(?:la|khoang)?\\s*" + NUMBER + "(?![a-z0-9.,])");
    private static final Pattern RANGE = Pattern
        .compile("(?:(?:tam hoat dong|quang duong moi lan sac|range)[^0-9-]{0,25}|(?:it nhat|toi" + " thieu|tren)\\s*)"
            + NUMBER + "\\s*km");
    private static final Pattern SEATS = Pattern.compile("(?<![a-z0-9])" + NUMBER + "\\s*(?:cho|ghe|seats?)(?![a-z])");
    private static final Pattern PEOPLE = Pattern.compile("(?<![a-z0-9])" + NUMBER + "\\s*nguoi(?![a-z])");
    private static final Pattern DAILY = Pattern.compile(
        "(?:moi ngay|hang ngay)\\D{0,12}" + NUMBER + "\\s*km|" + NUMBER + "\\s*km\\s*(?:/\\s*ngay|moi ngay|hang ngay)");
    private static final List<String> FORBIDDEN = List.of("thanh toan", "payment", "paymentservice", "payments",
        "don hang", "order", "orders", "hop dong", "contract", "contracts", "tra gop", "installment", "dat coc",
        "deposit", "chuyen tien", "cong no", "debt", "doanh thu", "revenue", "loi nhuan", "profit", "tai chinh",
        "financial", "tien vay", "loan", "hoa don", "invoice", "invoices", "tai khoan", "mat khau", "password", "users",
        "customers", "employees", "otp", "du lieu noi bo", "xem khach hang", "thong tin khach hang", "xac nhan mua",
        "dat mua", "dat xe", "giu cho", "reserve", "reservation", "purchase", "mua ngay", "mua ho", "chot don",
        "hoan tat mua", "buy now", "checkout", "book", "bank transfer", "giao dich", "transaction", "update", "delete",
        "insert", "cap nhat", "xoa du lieu", "xoa xe", "sua du lieu", "bo qua quy dinh", "bo qua huong dan",
        "ignore instructions", "ignore previous", "system prompt", "developer message", "override");
    private static final String REFUSAL = "Tôi chỉ tư vấn sản phẩm từ danh mục xe. Tôi không truy cập dữ liệu nội bộ, tài chính,"
        + " đơn hàng; không xử lý thanh toán, giữ chỗ, tạo hợp đồng hoặc xác nhận mua xe."
        + " Bạn hãy liên hệ nhân viên phụ trách để xử lý yêu cầu này.";
    private final AICatalogue catalogue;

    public AIService() {
        this(new AIRepository());
    }

    public AIService(AICatalogue catalogue) {
        this.catalogue = Objects.requireNonNull(catalogue);
    }

    public List<VehicleModel> models() throws SQLException {
        return List.copyOf(catalogue.findModels());
    }

    public AIResponse respond(AIRequest request) throws SQLException {
        return respond(request, "", null, List.of());
    }

    public AIResponse respond(AIRequest request, String conversationId, AIConstraints previousConstraints,
        List<Integer> priorModelIds) throws SQLException {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ApiException(400, "Vui lòng nhập câu hỏi hoặc nhu cầu tư vấn xe.");
        }
        if (request.getMessage().length() > 2000) {
            throw new ApiException(400, "Câu hỏi tối đa 2.000 ký tự.");
        }
        String message = normalize(request.getMessage());
        // Từ chối yêu cầu ngoài phạm vi trước khi đọc danh mục xe.
        if (forbidden(message) || forbidden(normalize(request.getIntent()))) {
            return result("REFUSED", true, REFUSAL, emptyConstraints(), List.of(), List.of(), conversationId);
        }

        String intent = "AUTO";
        if (request.getIntent() != null && !request.getIntent().isBlank()) {
            intent = request.getIntent().trim().toUpperCase(Locale.ROOT);
        }
        if (!INTENTS.contains(intent)) {
            throw new ApiException(400, "Ý định hợp lệ: AUTO, PRODUCT_QA, COMPARE, RECOMMEND, NEEDS_ANALYSIS.");
        }
        validateIds(request.getModelIds());
        validateNumbers(request.getBudget(), request.getMinRange(), request.getMinSeats());

        List<VehicleModel> catalogueModels = models();
        List<Integer> modelIds = resolveIds(request.getModelIds(), message, catalogueModels);
        boolean followUp = contains(message, "xe do", "mau do", "hai xe do", "chung", "those models", "that car");
        if (modelIds.isEmpty() && followUp && priorModelIds != null) {
            modelIds = List.copyOf(priorModelIds);
        }
        List<VehicleModel> selectedModels = select(modelIds, catalogueModels);
        AIConstraints constraints = parseConstraints(request, message, catalogueModels,
            followUp ? previousConstraints : null);
        if ("AUTO".equals(intent)) {
            intent = detectIntent(message, constraints);
        }

        if ("COMPARE".equals(intent)) {
            if (selectedModels.size() < 2) {
                return result(intent, false,
                    "Vui lòng chọn từ 2 đến 8 mẫu xe khác nhau để so sánh, hoặc nêu tên các mẫu" + " xe trong câu hỏi.",
                    constraints, List.of(), List.of(), conversationId);
            }
            StringBuilder answer = new StringBuilder("So sánh theo dữ liệu danh mục hiện tại:\n");
            for (VehicleModel model : selectedModels) {
                answer.append(describe(model, message)).append('\n');
            }
            answer.append("Giá là khoảng giá của xe đang có thể bán; mẫu hết xe có sẵn không có giá chào"
                + " bán hiện tại. Thông số thiếu được ghi rõ, không suy đoán.");
            return result(intent, false, answer.toString(), constraints, List.of(), selectedModels, conversationId);
        }
        if ("RECOMMEND".equals(intent) || "NEEDS_ANALYSIS".equals(intent)) {
            if (!hasRequirements(constraints) && selectedModels.isEmpty()) {
                return result(intent, false,
                    "Bạn hãy cung cấp ngân sách (VND), số chỗ, tầm hoạt động mong muốn hoặc mục"
                        + " đích sử dụng để tôi gợi ý xe phù hợp.",
                    constraints, List.of(), List.of(), conversationId);
            }
            List<VehicleModel> candidates = catalogueModels;
            if (!selectedModels.isEmpty()) {
                candidates = selectedModels;
            }
            List<VehicleRecommendation> recommendations = findRecommendations(candidates, constraints);
            String analysis = analyze(constraints);
            if (recommendations.isEmpty()) {
                return result(intent, false,
                    analysis + "\n" + "Không có xe đang có thể bán đáp ứng tất cả tiêu chí với dữ liệu"
                        + " hiện có. Xe thiếu giá hoặc thông số cần kiểm tra được loại khỏi"
                        + " kết quả; bạn có thể điều chỉnh tiêu chí.",
                    constraints, List.of(), List.of(), conversationId);
            }
            StringBuilder answer = new StringBuilder(analysis)
                .append("\n" + "Các mẫu phù hợp (xếp hạng theo mục đích sử dụng, sau đó" + " giá thấp nhất):\n");
            for (VehicleRecommendation recommendation : recommendations) {
                VehicleModel model = recommendation.getVehicle();
                answer.append(model.getBrandName()).append(' ').append(model.getModelName()).append(": ")
                    .append(String.join("; ", recommendation.getReasons())).append(".\n");
            }
            answer.append("Giá thấp nhất áp dụng cho ít nhất một xe có sẵn của mẫu; các phiên bản/màu"
                + " khác có thể có giá khác. Đây là gợi ý tham khảo từ danh mục.");
            return result(intent, false, answer.toString(), constraints, recommendations, List.of(), conversationId);
        }
        if (!selectedModels.isEmpty()) {
            StringBuilder answer = new StringBuilder();
            for (VehicleModel model : selectedModels) {
                answer.append(describe(model, message)).append('\n');
            }
            return result(intent, false, answer.toString().trim(), constraints, List.of(), selectedModels,
                conversationId);
        }
        if (catalogueModels.isEmpty()) {
            return result(intent, false,
                "Danh mục chưa có mẫu xe hoạt động. Vui lòng thử lại khi dữ liệu xe đã được bổ" + " sung.", constraints,
                List.of(), List.of(), conversationId);
        }
        List<String> modelNames = new ArrayList<>();
        for (VehicleModel model : catalogueModels) {
            if (modelNames.size() == 15) {
                break;
            }
            modelNames.add(model.getBrandName() + " " + model.getModelName());
        }
        String answer = "Tôi có thể trả lời về thông số xe, so sánh mẫu xe và gợi ý theo ngân sách, số chỗ,"
            + " tầm hoạt động. Hãy chọn mẫu xe hoặc nêu nhu cầu cụ thể. Danh mục hiện có: "
            + String.join(", ", modelNames)
            + (catalogueModels.size() > 15 ? " (và các mẫu khác trong danh sách)." : ".");
        return result(intent, false, answer, constraints, List.of(), List.of(), conversationId);
    }

    private static List<VehicleRecommendation> findRecommendations(List<VehicleModel> candidates,
        AIConstraints constraints) {
        // Chỉ gợi ý xe có thể bán và đáp ứng đủ các tiêu chí có dữ liệu.
        List<VehicleModel> matchingModels = new ArrayList<>();
        for (VehicleModel model : candidates) {
            if (matches(model, constraints)) {
                matchingModels.add(model);
            }
        }
        matchingModels.sort(ranking(constraints));

        List<VehicleRecommendation> recommendations = new ArrayList<>();
        for (VehicleModel model : matchingModels) {
            if (recommendations.size() == 5) {
                break;
            }
            recommendations.add(new VehicleRecommendation(model, reasons(model, constraints)));
        }
        return List.copyOf(recommendations);
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd')
            .replace('Đ', 'd').replaceAll("\\p{Cf}", "").toLowerCase(Locale.ROOT).replaceAll("[_\\-]+(?=[a-z])", " ")
            .replaceAll("\\s+", " ").trim();
    }

    private static boolean forbidden(String text) {
        for (String term : FORBIDDEN) {
            String expression = "(?<![a-z0-9])" + Pattern.quote(term).replace(" ", "\\E[\\s\\p{Punct}]*\\Q")
                + "(?![a-z0-9])";
            if (Pattern.compile(expression).matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static void validateIds(List<Integer> modelIds) {
        if (modelIds == null) {
            return;
        }
        if (modelIds.size() > 8) {
            throw new ApiException(400, "Chọn tối đa 8 mã mẫu xe hợp lệ, không trùng lặp.");
        }
        Set<Integer> checkedIds = new LinkedHashSet<>();
        for (Integer modelId : modelIds) {
            if (modelId == null || modelId <= 0 || !checkedIds.add(modelId)) {
                throw new ApiException(400, "Chọn tối đa 8 mã mẫu xe hợp lệ, không trùng lặp.");
            }
        }
    }

    private static void validateNumbers(BigDecimal budget, BigDecimal range, Integer seats) {
        if (budget != null && (budget.signum() <= 0 || budget.compareTo(new BigDecimal("9999999999999999.99")) > 0
            || budget.scale() > 2)) {
            throw new ApiException(400,
                "Ngân sách phải lớn hơn 0, có tối đa 2 chữ số thập phân và nằm trong giới hạn" + " VND hợp lệ.");
        }
        if (range != null
            && (range.signum() <= 0 || range.compareTo(new BigDecimal("100000")) > 0 || range.scale() > 2)) {
            throw new ApiException(400,
                "Tầm hoạt động phải lớn hơn 0 và không vượt quá 100.000 km, tối đa 2 chữ số" + " thập phân.");
        }
        if (seats != null && (seats <= 0 || seats > 100)) {
            throw new ApiException(400, "Số chỗ phải là số nguyên từ 1 đến 100.");
        }
    }

    private static List<Integer> resolveIds(List<Integer> suppliedModelIds, String message, List<VehicleModel> models) {
        if (suppliedModelIds != null && !suppliedModelIds.isEmpty()) {
            return List.copyOf(suppliedModelIds);
        }
        List<Integer> detectedModelIds = new ArrayList<>();
        Set<String> specificLines = new LinkedHashSet<>();
        // Nêu rõ Eco/Plus thì chỉ lấy phiên bản đó, không tự chọn cả dòng xe.
        for (VehicleModel model : models) {
            if (mentionsName(message, model.getModelName())) {
                detectedModelIds.add(model.getModelId());
                specificLines.add(normalize(model.getCarLineName()));
            }
        }
        // Khi hỏi chung "VF 9", lấy các mẫu Eco/Plus thuộc dòng VF 9.
        for (VehicleModel model : models) {
            String line = normalize(model.getCarLineName());
            if (!specificLines.contains(line) && mentionsName(message, model.getCarLineName())
                && !detectedModelIds.contains(model.getModelId())) {
                detectedModelIds.add(model.getModelId());
            }
        }
        if (detectedModelIds.size() > 8) {
            throw new ApiException(400, "Vui lòng chọn tối đa 8 mẫu xe cho mỗi câu hỏi.");
        }
        return detectedModelIds;
    }

    private static boolean mentionsName(String message, String name) {
        String normalizedName = normalize(name).replaceAll("(?<=[a-z])(?=\\d)|(?<=\\d)(?=[a-z])", " ");
        if (normalizedName.length() < 2) {
            return false;
        }
        String expression = "(?<![a-z0-9])" + Pattern.quote(normalizedName).replace(" ", "\\E\\s*\\Q") + "(?![a-z0-9])";
        return Pattern.compile(expression).matcher(message).find();
    }

    private static List<VehicleModel> select(List<Integer> modelIds, List<VehicleModel> models) {
        List<VehicleModel> selectedModels = new ArrayList<>();
        for (Integer modelId : modelIds) {
            VehicleModel selectedModel = null;
            for (VehicleModel model : models) {
                if (model.getModelId() == modelId) {
                    selectedModel = model;
                    break;
                }
            }
            if (selectedModel == null) {
                throw new ApiException(400, "Mẫu xe đã chọn không còn trong danh mục hoạt động: " + modelId + ".");
            }
            selectedModels.add(selectedModel);
        }
        return selectedModels;
    }

    private static AIConstraints parseConstraints(AIRequest request, String message, List<VehicleModel> models,
        AIConstraints previousConstraints) {
        List<String> inferredFields = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        BigDecimal budget = request.getBudget();
        if (budget == null) {
            Matcher moneyMatcher = MONEY.matcher(message);
            if (moneyMatcher.find()) {
                budget = moneyAmount(moneyMatcher.group(1), moneyMatcher.group(2));
                int firstAmountEnd = moneyMatcher.end();
                // Ghép cách viết như "1 tỷ 200 triệu" thành một ngân sách.
                if (moneyMatcher.find() && message.substring(firstAmountEnd, moneyMatcher.start()).trim().isEmpty()) {
                    budget = budget.add(moneyAmount(moneyMatcher.group(1), moneyMatcher.group(2)));
                }
                inferredFields.add("budget");
            } else {
                Matcher budgetMatcher = BUDGET.matcher(message);
                if (budgetMatcher.find()) {
                    budget = number(budgetMatcher.group(1));
                    inferredFields.add("budget");
                }
            }
        }
        BigDecimal range = request.getMinRange();
        Matcher rangeMatcher = RANGE.matcher(message);
        if (range == null && rangeMatcher.find()) {
            range = number(rangeMatcher.group(1));
            inferredFields.add("minRange");
        }
        Integer seats = request.getMinSeats();
        Matcher seatsMatcher = SEATS.matcher(message);
        if (seats == null && seatsMatcher.find()) {
            seats = wholeSeats(seatsMatcher.group(1));
            inferredFields.add("minSeats");
        }
        Matcher peopleMatcher = PEOPLE.matcher(message);
        if (seats == null && peopleMatcher.find()) {
            seats = wholeSeats(peopleMatcher.group(1));
            inferredFields.add("minSeats");
            notes.add("Số người trong câu hỏi được dùng làm số chỗ tối thiểu cần tìm.");
        }
        String purpose = detectUsagePurpose(message);
        if (purpose != null) {
            inferredFields.add("usagePurpose");
        }
        String brand = findPreferredBrand(message, models);
        if (brand != null) {
            inferredFields.add("preferredBrand");
        }
        BigDecimal dailyDistance = null;
        Matcher dailyMatcher = DAILY.matcher(message);
        if (dailyMatcher.find()) {
            String distanceText = dailyMatcher.group(1);
            if (distanceText == null) {
                distanceText = dailyMatcher.group(2);
            }
            dailyDistance = number(distanceText);
            inferredFields.add("dailyDistanceKm");
        }
        // Câu hỏi tiếp nối chỉ dùng lại tiêu chí còn thiếu trong cùng phiên.
        if (previousConstraints != null) {
            if (budget == null) {
                budget = previousConstraints.getBudget();
            }
            if (range == null) {
                range = previousConstraints.getMinRange();
            }
            if (seats == null) {
                seats = previousConstraints.getMinSeats();
            }
            if (purpose == null) {
                purpose = previousConstraints.getUsagePurpose();
            }
            if (brand == null) {
                brand = previousConstraints.getPreferredBrand();
            }
            if (dailyDistance == null) {
                dailyDistance = previousConstraints.getDailyDistanceKm();
            }
            notes.add("Dùng lại tiêu chí của lượt tư vấn trước trong cùng phiên.");
        }
        validateNumbers(budget, range, seats);
        if (dailyDistance != null
            && (dailyDistance.signum() <= 0 || dailyDistance.compareTo(new BigDecimal("100000")) > 0)) {
            throw new ApiException(400, "Quãng đường mỗi ngày phải lớn hơn 0 và không vượt quá 100.000 km.");
        }
        if (dailyDistance != null) {
            notes.add("Quãng đường mỗi ngày không được tự coi là tầm hoạt động tối thiểu mỗi lần sạc;"
                + " hãy nêu yêu cầu tầm hoạt động nếu cần lọc.");
        }
        if (contains(message, "sac tai nha", "cho sac", "khong co sac", "nhu cau sac")) {
            notes.add("Hãy xác định nơi sạc và thời gian sạc mong muốn; danh mục chưa mô tả chuẩn sạc"
                + " hoặc điều kiện lắp sạc tại nhà.");
        }
        if (contains(message, "adas", "camera", "bao hanh", "loai pin", "apple carplay", "android auto")) {
            notes.add("Danh mục chưa có trường xác thực riêng cho tính năng/bảo hành bạn yêu cầu. Gợi"
                + " ý chỉ đánh giá các tiêu chí có dữ liệu; không xác nhận xe đáp ứng tính"
                + " năng còn thiếu thông tin.");
        }
        return new AIConstraints(budget, range, seats, purpose, brand, dailyDistance, List.copyOf(notes),
            List.copyOf(inferredFields));
    }

    private static String detectUsagePurpose(String message) {
        if (contains(message, "duong dai", "lien tinh", "di xa", "long distance")) {
            return "LONG_DISTANCE";
        }
        if (contains(message, "gia dinh", "family")) {
            return "FAMILY";
        }
        if (contains(message, "thanh pho", "do thi", "di lam", "city")) {
            return "CITY";
        }
        return null;
    }

    private static String findPreferredBrand(String message, List<VehicleModel> models) {
        for (VehicleModel model : models) {
            String brandName = model.getBrandName();
            if (brandName == null) {
                continue;
            }
            String expression = "(?<![a-z0-9])" + Pattern.quote(normalize(brandName)) + "(?![a-z0-9])";
            if (Pattern.compile(expression).matcher(message).find()) {
                return brandName;
            }
        }
        return null;
    }

    private static BigDecimal moneyAmount(String amount, String unit) {
        BigDecimal multiplier = BigDecimal.ONE;
        if (unit.equals("ty") || unit.equals("ti") || unit.equals("billion")) {
            multiplier = new BigDecimal("1000000000");
        } else if (unit.equals("trieu") || unit.equals("tr") || unit.equals("million")) {
            multiplier = new BigDecimal("1000000");
        }
        return number(amount).multiply(multiplier).stripTrailingZeros();
    }

    private static BigDecimal number(String text) {
        try {
            String numberText;
            if (text.matches("-?\\d{1,3}(?:[.,]\\d{3})+")) {
                numberText = text.replaceAll("[.,]", "");
            } else {
                numberText = text.replace(',', '.');
            }
            return new BigDecimal(numberText);
        } catch (NumberFormatException exception) {
            throw new ApiException(400, "Giá trị số trong câu hỏi chưa rõ. Vui lòng dùng các ô tiêu chí để nhập số.");
        }
    }

    private static String detectIntent(String message, AIConstraints constraints) {
        if (contains(message, "so sanh", "compare", "versus", " vs ")) {
            return "COMPARE";
        }
        if (contains(message, "phan tich nhu cau", "nhu cau", "moi ngay", "hang ngay", "sac tai nha")) {
            return "NEEDS_ANALYSIS";
        }
        if (contains(message, "goi y", "de xuat", "tu van", "phu hop", "recommend")) {
            return "RECOMMEND";
        }
        if (contains(message, "bao nhieu", "bao lau", "thong so", "chi tiet", "bao hanh", "cong suat", "toc do",
            "tui khi")) {
            return "PRODUCT_QA";
        }
        if (hasRequirements(constraints)) {
            return "RECOMMEND";
        }
        return "PRODUCT_QA";
    }

    private static boolean hasRequirements(AIConstraints constraints) {
        return constraints.getBudget() != null || constraints.getMinRange() != null || constraints.getMinSeats() != null
            || constraints.getUsagePurpose() != null || constraints.getPreferredBrand() != null
            || constraints.getDailyDistanceKm() != null;
    }

    private static boolean matches(VehicleModel vehicle, AIConstraints constraints) {
        if (vehicle.getAvailableCars() <= 0 || vehicle.getMinPrice() == null || vehicle.getMinPrice().signum() <= 0) {
            return false;
        }
        if (constraints.getBudget() != null && vehicle.getMinPrice().compareTo(constraints.getBudget()) > 0) {
            return false;
        }

        // Thiếu thông số thì không được coi là đáp ứng tiêu chí khách yêu cầu.
        VehicleSpecifications specifications = vehicle.getSpecifications();
        if (constraints.getMinRange() != null) {
            if (specifications.getRangeKM() == null
                || specifications.getRangeKM().compareTo(constraints.getMinRange()) < 0) {
                return false;
            }
        }
        if (constraints.getMinSeats() != null) {
            if (specifications.getSeatingCapacity() == null
                || specifications.getSeatingCapacity() < constraints.getMinSeats()) {
                return false;
            }
        }
        return constraints.getPreferredBrand() == null
            || constraints.getPreferredBrand().equals(vehicle.getBrandName());
    }

    private static Comparator<VehicleModel> ranking(AIConstraints constraints) {
        Comparator<VehicleModel> priceOrder = Comparator.comparing(VehicleModel::getMinPrice);
        Comparator<VehicleModel> recommendationOrder = priceOrder;
        // Đi xa ưu tiên tầm hoạt động; gia đình ưu tiên số chỗ. Giá dùng để phân hạng tiếp.
        if ("LONG_DISTANCE".equals(constraints.getUsagePurpose())) {
            recommendationOrder = Comparator
                .comparing((VehicleModel vehicle) -> vehicle.getSpecifications().getRangeKM(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(priceOrder);
        }
        if ("FAMILY".equals(constraints.getUsagePurpose())) {
            recommendationOrder = Comparator
                .comparing((VehicleModel vehicle) -> vehicle.getSpecifications().getSeatingCapacity(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(priceOrder);
        }
        return recommendationOrder.thenComparingInt(VehicleModel::getModelId);
    }

    private static Integer wholeSeats(String text) {
        try {
            return number(text).intValueExact();
        } catch (ArithmeticException exception) {
            throw new ApiException(400, "Số chỗ hoặc số người phải là số nguyên.");
        }
    }

    private static List<String> reasons(VehicleModel vehicle, AIConstraints constraints) {
        List<String> reasons = new ArrayList<>();
        VehicleSpecifications specifications = vehicle.getSpecifications();
        reasons.add("Giá xe có sẵn từ " + money(vehicle.getMinPrice()));
        if (constraints.getBudget() != null) {
            reasons.add("không vượt ngân sách " + money(constraints.getBudget()));
        }
        if (specifications.getRangeKM() != null) {
            String rangeReason = "tầm hoạt động danh mục " + value(specifications.getRangeKM()) + " km";
            String description = specifications.getDescription();
            if (description != null) {
                Matcher standard = Pattern.compile("\\b(NEDC|WLTP)\\b").matcher(description);
                if (standard.find()) {
                    rangeReason += " (" + standard.group(1) + ")";
                }
            }
            if (constraints.getMinRange() != null) {
                rangeReason += " đáp ứng mức tối thiểu " + value(constraints.getMinRange()) + " km";
            }
            reasons.add(rangeReason);
        } else {
            reasons.add("chưa có dữ liệu tầm hoạt động");
        }
        if (specifications.getSeatingCapacity() != null) {
            String seatsReason = specifications.getSeatingCapacity() + " chỗ";
            if (constraints.getMinSeats() != null) {
                seatsReason += " đáp ứng số chỗ yêu cầu";
            }
            reasons.add(seatsReason);
        } else {
            reasons.add("chưa có dữ liệu số chỗ");
        }
        if (constraints.getPreferredBrand() != null) {
            reasons.add("đúng hãng " + constraints.getPreferredBrand());
        }
        reasons.add("có xe ở trạng thái có thể bán");
        return List.copyOf(reasons);
    }

    private static String analyze(AIConstraints constraints) {
        List<String> criteria = new ArrayList<>();
        if (constraints.getBudget() != null) {
            criteria.add("ngân sách tối đa " + money(constraints.getBudget()));
        }
        if (constraints.getMinRange() != null) {
            criteria.add("tầm hoạt động tối thiểu " + value(constraints.getMinRange()) + " km mỗi lần sạc");
        }
        if (constraints.getMinSeats() != null) {
            criteria.add("ít nhất " + constraints.getMinSeats() + " chỗ");
        }
        if (constraints.getPreferredBrand() != null) {
            criteria.add("hãng " + constraints.getPreferredBrand());
        }
        if (constraints.getUsagePurpose() != null) {
            criteria.add("mục đích " + usageDescription(constraints.getUsagePurpose()));
        }
        if (constraints.getDailyDistanceKm() != null) {
            criteria.add("di chuyển " + value(constraints.getDailyDistanceKm()) + " km mỗi ngày");
        }
        String analysis = "Tôi hiểu nhu cầu của bạn: " + String.join("; ", criteria) + ".";
        if (!constraints.getNotes().isEmpty()) {
            analysis += "\n" + String.join("\n", constraints.getNotes());
        }
        return analysis;
    }

    private static String usageDescription(String usagePurpose) {
        if ("FAMILY".equals(usagePurpose)) {
            return "gia đình (ưu tiên số chỗ)";
        }
        if ("LONG_DISTANCE".equals(usagePurpose)) {
            return "đường dài (ưu tiên tầm hoạt động danh mục)";
        }
        return "đô thị/đi làm (ưu tiên giá phù hợp)";
    }

    private static String describe(VehicleModel vehicle, String message) {
        VehicleSpecifications specifications = vehicle.getSpecifications();
        StringBuilder answer = new StringBuilder();
        answer.append(vehicle.getBrandName()).append(' ').append(vehicle.getModelName()).append(" (")
            .append(vehicle.getCarLineName()).append("): ").append(priceDescription(vehicle));

        // Hiển thị đúng dữ liệu trong danh mục; giá trị null phải báo chưa có dữ liệu.
        answer.append("; pin ").append(quantity(specifications.getBatteryCapacity(), "kWh"));
        answer.append("; tầm hoạt động ").append(quantity(specifications.getRangeKM(), "km"));
        answer.append("; số chỗ ").append(value(specifications.getSeatingCapacity()));
        answer.append("; công suất ").append(quantity(specifications.getMaxPower(), "kW"));
        answer.append("; tốc độ tối đa ").append(quantity(specifications.getMaxSpeed(), "km/h"));
        answer.append("; kích thước ").append(value(specifications.getDimensions()));
        answer.append("; túi khí ").append(value(specifications.getAirbagCount()));
        answer.append("; thời gian sạc ").append(value(specifications.getChargingTime()))
            .append(" (danh mục chưa ghi đơn vị/điều kiện sạc)");
        answer.append("; tiêu thụ năng lượng ").append(value(specifications.getEnergyConsumption()))
            .append(" (danh mục chưa ghi đơn vị)");
        answer.append("; loại bánh ").append(value(specifications.getWheelType()));
        answer.append("; phiên bản có sẵn ").append(listDescription(vehicle.getVersions()));
        answer.append("; màu có sẵn ").append(listDescription(vehicle.getColors())).append('.');

        if (vehicle.getDescription() != null && !vehicle.getDescription().isBlank()) {
            answer.append(" Mô tả danh mục: ").append(vehicle.getDescription());
        }
        if (specifications.getDescription() != null && !specifications.getDescription().isBlank()) {
            answer.append(" Mô tả thông số: ").append(specifications.getDescription());
        }
        if (contains(message, "bao hanh", "adas", "camera", "loai pin", "tram sac", "apple carplay", "android auto")) {
            answer.append(" Danh mục không có trường xác thực riêng cho thông tin bổ sung bạn hỏi (bảo"
                + " hành/tính năng/chuẩn sạc); tôi không tự suy đoán thông tin này.");
        }
        return answer.toString();
    }

    private static String priceDescription(VehicleModel vehicle) {
        if (vehicle.getAvailableCars() <= 0 || vehicle.getMinPrice() == null) {
            return "chưa có xe có thể bán/giá chào bán hiện tại";
        }
        String description = "giá xe có sẵn từ " + money(vehicle.getMinPrice());
        if (vehicle.getMaxPrice() != null && vehicle.getMaxPrice().compareTo(vehicle.getMinPrice()) > 0) {
            description += " đến " + money(vehicle.getMaxPrice());
        }
        return description;
    }

    private static String listDescription(List<String> values) {
        if (values.isEmpty()) {
            return "chưa có dữ liệu";
        }
        return String.join(", ", values);
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(value) + " VND";
    }

    private static String quantity(BigDecimal amount, String unit) {
        return amount == null ? "chưa có dữ liệu" : value(amount) + " " + unit;
    }

    private static String value(Object value) {
        if (value == null) {
            return "chưa có dữ liệu";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private static boolean contains(String message, String... terms) {
        for (String term : terms) {
            if (message.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static AIConstraints emptyConstraints() {
        return new AIConstraints(null, null, null, null, null, null, List.of(), List.of());
    }

    private static AIResponse result(String intent, boolean refused, String answer, AIConstraints constraints,
        List<VehicleRecommendation> recommendations, List<VehicleModel> comparison, String conversationId) {
        return new AIResponse(MODE, intent, refused, answer, constraints, recommendations, comparison, conversationId,
            Instant.now());
    }
}

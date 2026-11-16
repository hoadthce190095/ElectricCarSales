<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f3f6fb">
    <title>So sánh xe | VinFast - ElectricCarSale</title>
    <style>
        :root { color-scheme: light; font-family: Arial, sans-serif; color: #26364d; background: #f3f6fb; }
        * { box-sizing: border-box; }
        body { margin: 0; }
        a { color: #315c99; text-decoration: none; }
        .page-header { padding: 16px max(20px, calc((100% - 1180px) / 2)); border-bottom: 1px solid #e2e8f0; background: #fff; }
        .page-header a { font-size: 14px; font-weight: 700; }
        main { width: min(1180px, calc(100% - 32px)); margin: 30px auto 48px; }
        .intro { display: flex; justify-content: space-between; align-items: end; gap: 20px; margin-bottom: 20px; }
        .eyebrow { color: #74839a; font-size: 12px; font-weight: 700; letter-spacing: 1.4px; }
        h1 { margin: 6px 0; color: #213653; font-size: clamp(24px, 4vw, 34px); }
        .intro p { margin: 0; color: #778398; font-size: 14px; line-height: 1.5; }
        .back-link { flex: 0 0 auto; padding: 10px 14px; border: 1px solid #d7e0ec; border-radius: 6px; background: #fff; font-size: 13px; font-weight: 700; }
        .back-link:hover { border-color: #7897c2; background: #f7faff; }
        .notice { margin: 0 0 14px; padding: 12px 15px; border: 1px solid #d8e5f5; border-radius: 8px; background: #f6f9fe; color: #536984; font-size: 13px; }
        .comparison-panel { overflow: hidden; border: 1px solid #e0e7f0; border-radius: 10px; background: #fff; box-shadow: 0 8px 28px rgba(37, 57, 86, .07); }
        .table-scroll { overflow-x: auto; }
        .comparison-table { width: 100%; min-width: 680px; border-collapse: collapse; }
        .comparison-table th, .comparison-table td { min-width: 175px; padding: 14px 16px; border-bottom: 1px solid #e9eef4; text-align: left; vertical-align: top; }
        .comparison-table thead th { background: #fbfcfe; }
        .comparison-table thead th:first-child { width: 185px; min-width: 145px; color: #7a8799; font-size: 13px; }
        .comparison-table tbody th { position: sticky; left: 0; z-index: 1; background: #fff; color: #738197; font-size: 13px; font-weight: 700; }
        .comparison-table td { color: #394b65; font-size: 14px; line-height: 1.55; }
        .vehicle-heading { display: grid; gap: 10px; }
        .vehicle-heading img { width: 100%; height: 130px; object-fit: contain; border-radius: 6px; background: #f5f7fa; }
        .vehicle-heading img[hidden] { display: none; }
        .vehicle-name { color: #244b80; font-size: 17px; font-weight: 800; }
        .vehicle-version { margin-top: -6px; color: #7a8799; font-size: 12px; font-weight: 400; }
        .vehicle-heading a { justify-self: start; font-size: 12px; font-weight: 700; }
        .empty-state { padding: 34px 18px; color: #63728a; text-align: center; }
        .page-footer { padding: 18px; color: #8792a3; font-size: 12px; text-align: center; }
        @media (max-width: 600px) {
            main { margin-top: 20px; }
            .intro { align-items: start; flex-direction: column; }
            .back-link { align-self: start; }
            .comparison-table th, .comparison-table td { padding: 11px; }
            .vehicle-heading img { height: 100px; }
        }
    </style>
</head>
<body>
<header class="page-header">
    <a href="<c:url value='/index.jsp'/>">VinFast · ElectricCarSale</a>
</header>
<main>
    <div class="intro">
        <div>
            <span class="eyebrow">ĐỐI CHIẾU THÔNG SỐ</span>
            <h1>So sánh xe</h1>
            <p aria-live="polite">
                <c:choose>
                    <c:when test="${not empty comparisonSummary}"><c:out value="${comparisonSummary}"/></c:when>
                    <c:otherwise><c:out value="${comparisonError}"/></c:otherwise>
                </c:choose>
            </p>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
            <c:if test="${not empty comparisonColumns and comparisonColumns.size() lt 3}">
                <a class="back-link" style="background:#f0fdf4;border-color:#86efac;color:#166534;" href="<c:url value='/index.jsp#models'/>">➕ Thêm xe khác để so sánh</a>
            </c:if>
            <a class="back-link" href="<c:url value='/index.jsp#models'/>">← Quay lại danh sách xe</a>
        </div>
    </div>

    <c:if test="${not empty comparisonNotice}">
        <p class="notice" role="status"><c:out value="${comparisonNotice}"/></p>
    </c:if>

    <section class="comparison-panel" aria-label="Bảng so sánh các mẫu xe">
        <c:choose>
            <c:when test="${not empty comparisonColumns}">
                <div class="table-scroll">
                    <table class="comparison-table">
                        <thead>
                        <tr>
                            <th scope="col">Thông tin</th>
                            <c:forEach var="vehicle" items="${comparisonColumns}">
                                <th scope="col">
                                    <div class="vehicle-heading">
                                        <c:if test="${not empty vehicle.imageUrl}">
                                            <img src="<c:out value='${vehicle.imageUrl}'/>"
                                                 alt="Xe <c:out value='${vehicle.modelName}'/>" loading="lazy"
                                                 onerror="this.hidden=true">
                                        </c:if>
                                        <span class="vehicle-name"><c:out value="${vehicle.modelName}"/></span>
                                        <span class="vehicle-version"><c:out value="${vehicle.specificationVersion}"/></span>
                                        <div style="display:flex;justify-content:space-between;align-items:center;gap:6px;margin-top:4px;">
                                            <a href="<c:url value='/detail'><c:param name='id' value='${vehicle.modelID}'/></c:url>">Xem chi tiết ↗</a>
                                            <button type="button" class="btn-remove-compare" data-key="<c:out value='${vehicle.comparisonKey}'/>" title="Bỏ xe này khỏi bảng so sánh" style="background:#fee2e2;color:#991b1b;border:1px solid #fca5a5;border-radius:4px;padding:3px 8px;font-size:11px;cursor:pointer;font-weight:600;">✕ Bỏ</button>
                                        </div>
                                    </div>
                                </th>
                            </c:forEach>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="row" items="${comparisonRows}">
                            <tr>
                                <th scope="row"><c:out value="${row.label}"/></th>
                                <c:forEach var="value" items="${row.values}">
                                    <td><c:out value="${value}"/></td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <p><c:out value="${comparisonError}"/></p>
                    <a class="back-link" href="<c:url value='/index.jsp#models'/>">Chọn xe để so sánh</a>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>
<footer class="page-footer">Thông tin sản phẩm mang tính tham khảo. Vui lòng đối chiếu cấu hình với VinFast trước khi giao dịch.</footer>
<script>
(function() {
    const removeButtons = document.querySelectorAll(".btn-remove-compare");
    const activeKeys = Array.from(removeButtons).map(b => b.dataset.key).filter(Boolean);
    if (activeKeys.length > 0) {
        try {
            sessionStorage.setItem("electriccarsale.compareSelections", JSON.stringify(activeKeys));
        } catch (_) {}
    }
    removeButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            const targetKey = btn.dataset.key;
            const remaining = activeKeys.filter(k => k !== targetKey);
            if (remaining.length < 2) {
                if (confirm("Nếu bỏ xe này thì chỉ còn 1 xe (cần ít nhất 2 xe để so sánh). Quay lại danh mục chọn xe?")) {
                    try { sessionStorage.setItem("electriccarsale.compareSelections", JSON.stringify(remaining)); } catch (_) {}
                    window.location.href = "<c:url value='/index.jsp#models'/>";
                }
                return;
            }
            try { sessionStorage.setItem("electriccarsale.compareSelections", JSON.stringify(remaining)); } catch (_) {}
            const params = new URLSearchParams();
            remaining.forEach(k => params.append("key", k));
            window.location.href = "<c:url value='/comparison'/>?" + params.toString();
        });
    });
})();
</script>
</body>
</html>

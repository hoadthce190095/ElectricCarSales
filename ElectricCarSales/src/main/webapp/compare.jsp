<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.electriccarsales.model.SessionUser" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
Object authenticatedUser = session.getAttribute("user");
Object legacyUserId = session.getAttribute("userID");
boolean isAuthenticated = authenticatedUser instanceof SessionUser
    || (legacyUserId instanceof Number && ((Number) legacyUserId).intValue() > 0);
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f3f6fb">
    <meta name="user-authenticated" content="<%= isAuthenticated %>">
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
        .back-link { flex: 0 0 auto; padding: 10px 14px; border: 1px solid #d7e0ec; border-radius: 6px; background: #fff; font-size: 13px; font-weight: 700; cursor: pointer; transition: all .15s; }
        .back-link:hover { border-color: #7897c2; background: #f7faff; }
        .btn-saved { background: #ecfdf5 !important; border-color: #6ee7b7 !important; color: #065f46 !important; }
        .history-badge { background: #e0ecff; color: #1d4ed8; padding: 1px 7px; border-radius: 99px; font-size: 11px; margin-left: 5px; font-weight: 800; }
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

        /* Comparison History Dialog */
        .history-dialog { width: min(720px, calc(100% - 24px)); max-height: min(85vh, 680px); padding: 0; border: 1px solid #e1e7f0; border-radius: 12px; background: #fff; color: #26364d; box-shadow: 0 24px 80px rgba(18,36,64,.25); }
        .history-dialog::backdrop { background: rgba(17,30,49,.48); backdrop-filter: blur(3px); }
        .dialog-heading { display: flex; align-items: center; justify-content: space-between; padding: 18px 22px; border-bottom: 1px solid #e9edf2; }
        .dialog-heading h2 { margin: 3px 0 0; font-size: 20px; font-weight: 800; }
        .dialog-close { width: 31px; height: 31px; border: 1px solid #e5e9ef; border-radius: 50%; background: #fff; color: #69778c; font-size: 20px; line-height: 1; cursor: pointer; }
        .dialog-close:hover { background: #f3f5f8; }
        .history-content-wrap { max-height: calc(85vh - 140px); overflow-y: auto; padding: 18px 22px; }
        .history-card { border: 1px solid #e2e8f0; border-radius: 9px; padding: 13px 15px; margin-bottom: 12px; background: #fbfcfe; transition: all .18s; }
        .history-card:hover { background: #fff; border-color: #b9cee8; box-shadow: 0 4px 16px rgba(28,52,86,.07); }
        .history-card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px dashed #e3e8ef; }
        .history-date { color: #728096; font-size: 11px; font-weight: 500; }
        .history-count-tag { font-size: 10px; font-weight: 700; padding: 2px 7px; border-radius: 4px; background: #f0f4f9; color: #475569; }
        .history-vehicles-grid { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }
        .history-vehicle-item { display: flex; align-items: center; gap: 9px; flex: 1; min-width: 140px; background: #fff; padding: 7px 9px; border: 1px solid #edf0f5; border-radius: 7px; }
        .history-vehicle-item img { width: 50px; height: 35px; object-fit: contain; background: #f3f5f8; border-radius: 4px; flex-shrink: 0; }
        .history-vehicle-info { min-width: 0; }
        .history-vehicle-info strong { display: block; font-size: 12px; color: #213547; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .history-vehicle-info small { display: block; font-size: 10px; color: #718096; }
        .history-vs { font-weight: 800; font-size: 10px; color: #94a3b8; text-transform: uppercase; }
        .history-card-bottom { display: flex; justify-content: flex-end; align-items: center; gap: 8px; }
        .btn-view-compare { padding: 6px 12px; border-radius: 5px; background: #1e40af; color: #fff; font-size: 11px; font-weight: 700; text-decoration: none; cursor: pointer; border: 0; transition: background .15s; }
        .btn-view-compare:hover { background: #1e3a8a; }
        .btn-delete-history { padding: 5px 8px; border-radius: 5px; background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; font-size: 11px; font-weight: 600; cursor: pointer; transition: background .15s; }
        .btn-delete-history:hover { background: #fee2e2; }
        .history-empty { text-align: center; padding: 36px 16px; color: #718096; }
        .history-empty-icon { font-size: 34px; margin-bottom: 8px; opacity: .7; }
        .history-empty h3 { margin: 0 0 6px; font-size: 15px; color: #334155; }
        .history-empty p { margin: 0; font-size: 12px; }
        .history-dialog-actions { display: flex; justify-content: space-between; align-items: center; padding: 12px 22px; border-top: 1px solid #e9edf2; background: #fbfcfe; }
        .btn-clear-history { background: transparent; border: 0; color: #dc2626; font-size: 11px; font-weight: 600; cursor: pointer; padding: 6px 8px; border-radius: 4px; }
        .btn-clear-history:hover { background: #fee2e2; }
        .btn-secondary { padding: 7px 14px; border: 1px solid #d1d5db; border-radius: 5px; background: #fff; color: #374151; font-size: 11px; font-weight: 600; cursor: pointer; }
        .btn-secondary:hover { background: #f3f4f6; }

        @media (max-width: 600px) {
            main { margin-top: 20px; }
            .intro { align-items: start; flex-direction: column; }
            .back-link { align-self: start; }
            .comparison-table th, .comparison-table td { padding: 11px; }
            .vehicle-heading img { height: 100px; }
        }
    </style>
</head>
<body data-authenticated="<%= isAuthenticated %>">
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
            <% if (isAuthenticated) { %>
            <c:if test="${not empty comparisonColumns}">
                <button type="button" class="back-link" id="btn-save-comparison" style="background:#eff6ff;border-color:#93c5fd;color:#1e40af;">💾 Lưu so sánh này</button>
            </c:if>
            <button type="button" class="back-link" id="btn-open-history">🕒 Lịch sử so sánh <span id="history-badge" class="history-badge" hidden>0</span></button>
            <% } %>
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

<% if (isAuthenticated) { %>
<dialog class="history-dialog" id="compare-history-dialog" aria-labelledby="history-dialog-title">
    <div class="dialog-heading">
        <div>
            <span class="eyebrow">DỮ LIỆU ĐÃ LƯU</span>
            <h2 id="history-dialog-title">Lịch sử so sánh xe</h2>
        </div>
        <button type="button" class="dialog-close" id="history-close-btn" aria-label="Đóng">×</button>
    </div>
    <div class="history-content-wrap">
        <div id="history-list"></div>
    </div>
    <div class="history-dialog-actions">
        <button type="button" class="btn-clear-history" id="btn-clear-all-history">Xóa tất cả lịch sử</button>
        <button type="button" class="btn-secondary" id="history-done-btn">Đóng</button>
    </div>
</dialog>
<% } %>

<footer class="page-footer">Thông tin sản phẩm mang tính tham khảo. Vui lòng đối chiếu cấu hình với VinFast trước khi giao dịch.</footer>

<c:if test="${not empty comparisonColumns}">
<script id="current-comparison-vehicles" type="application/json">
[
    <c:forEach var="v" items="${comparisonColumns}" varStatus="loop">
    {
        "id": <c:out value="${v.modelID != null ? v.modelID : 0}"/>,
        "modelName": "<c:out value='${v.modelName}'/>",
        "specificationVersion": "<c:out value='${v.specificationVersion}'/>",
        "imageUrl": "<c:out value='${v.imageUrl}'/>",
        "comparisonKey": "<c:out value='${v.comparisonKey}'/>"
    }<c:if test="${not loop.last}">,</c:if>
    </c:forEach>
]
</script>
</c:if>

<script>
(function() {
    const contextPath = "<c:out value='${pageContext.request.contextPath}'/>";
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

    // =========================================================================
    // Comparison History Management (Authenticated Users Only)
    // =========================================================================
    const isAuthenticated = document.body.dataset.authenticated === "true";
    if (!isAuthenticated) {
        try { localStorage.removeItem("electriccarsale.compareHistory"); } catch (_) {}
        return;
    }

    const CompareHistory = {
        STORAGE_KEY: "electriccarsale.compareHistory",
        API_URL: contextPath + "/api/comparison/history",

        getAll() {
            try {
                const raw = localStorage.getItem(this.STORAGE_KEY);
                return raw ? JSON.parse(raw) : [];
            } catch (_) {
                return [];
            }
        },

        save(vehicles) {
            if (!vehicles || vehicles.length < 2) return null;
            const history = this.getAll();
            const keys = vehicles.map(v => v.comparisonKey || (v.modelName + "-" + v.specificationVersion));

            const filtered = history.filter(item => {
                if (!item.keys || item.keys.length !== keys.length) return true;
                return !keys.every(k => item.keys.includes(k));
            });

            const now = new Date();
            const dateStr = now.toLocaleDateString("vi-VN", {
                day: "2-digit", month: "2-digit", year: "numeric",
                hour: "2-digit", minute: "2-digit"
            });

            const newItem = {
                id: "comp_" + Date.now(),
                timestamp: Date.now(),
                dateStr: dateStr,
                keys: keys,
                vehicles: vehicles.map(v => ({
                    id: v.modelID || v.id,
                    modelName: v.modelName || "",
                    specificationVersion: v.specificationVersion || "",
                    imageUrl: v.imageUrl || "",
                    comparisonKey: v.comparisonKey || (v.modelName + "-" + v.specificationVersion)
                }))
            };

            filtered.unshift(newItem);
            if (filtered.length > 20) filtered.length = 20;

            try {
                localStorage.setItem(this.STORAGE_KEY, JSON.stringify(filtered));
            } catch (_) {}

            const modelIds = vehicles.map(v => Number(v.modelID || v.id)).filter(id => Number.isInteger(id) && id > 0);
            fetch(this.API_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json", "Accept": "application/json" },
                body: JSON.stringify({ modelIds: modelIds, keys: keys })
            }).then(r => r.json()).catch(() => {});

            return newItem;
        },

        remove(id) {
            const history = this.getAll();
            const itemToRemove = history.find(item => item.id === id);
            const next = history.filter(item => item.id !== id);
            try {
                localStorage.setItem(this.STORAGE_KEY, JSON.stringify(next));
            } catch (_) {}

            if (itemToRemove && itemToRemove.sessionId) {
                fetch(this.API_URL + "?id=" + itemToRemove.sessionId, { method: "DELETE" }).catch(() => {});
            }
            return next;
        },

        clear() {
            try {
                localStorage.removeItem(this.STORAGE_KEY);
            } catch (_) {}
        }
    };

    const historyBadge = document.getElementById("history-badge");
    const historyDialog = document.getElementById("compare-history-dialog");
    const historyList = document.getElementById("history-list");
    const btnOpenHistory = document.getElementById("btn-open-history");
    const btnSaveComparison = document.getElementById("btn-save-comparison");
    const btnClearAllHistory = document.getElementById("btn-clear-all-history");
    const historyCloseBtn = document.getElementById("history-close-btn");
    const historyDoneBtn = document.getElementById("history-done-btn");

    function updateBadge() {
        const count = CompareHistory.getAll().length;
        if (historyBadge) {
            historyBadge.textContent = String(count);
            historyBadge.hidden = count === 0;
        }
    }

    // Auto-save current comparison if loaded
    const currentVehiclesEl = document.getElementById("current-comparison-vehicles");
    if (currentVehiclesEl) {
        try {
            const currentVehicles = JSON.parse(currentVehiclesEl.textContent);
            if (Array.isArray(currentVehicles) && currentVehicles.length >= 2) {
                CompareHistory.save(currentVehicles);
                if (btnSaveComparison) {
                    btnSaveComparison.classList.add("btn-saved");
                    btnSaveComparison.textContent = "✓ Đã lưu vào lịch sử";
                }
            }
        } catch (e) {
            console.error("Lỗi khi lưu dữ liệu so sánh vào lịch sử:", e);
        }
    }

    if (btnSaveComparison) {
        btnSaveComparison.addEventListener("click", () => {
            if (currentVehiclesEl) {
                try {
                    const currentVehicles = JSON.parse(currentVehiclesEl.textContent);
                    CompareHistory.save(currentVehicles);
                    btnSaveComparison.classList.add("btn-saved");
                    btnSaveComparison.textContent = "✓ Đã lưu vào lịch sử";
                    updateBadge();
                    alert("Đã lưu bảng so sánh này vào lịch sử so sánh xe của bạn!");
                } catch (_) {}
            }
        });
    }

    function renderHistory() {
        if (!historyList) return;
        const history = CompareHistory.getAll();
        historyList.innerHTML = "";

        if (history.length === 0) {
            historyList.innerHTML = '<div class="history-empty"><div class="history-empty-icon">⚖</div><h3>Chưa có lịch sử so sánh</h3><p>Hãy chọn từ 2 đến 3 xe để bắt đầu so sánh.</p></div>';
            if (btnClearAllHistory) btnClearAllHistory.hidden = true;
            return;
        }

        if (btnClearAllHistory) btnClearAllHistory.hidden = false;

        history.forEach(item => {
            const card = document.createElement("div");
            card.className = "history-card";

            const top = document.createElement("div");
            top.className = "history-card-top";
            const date = document.createElement("span");
            date.className = "history-date";
            date.textContent = item.dateStr || "Gần đây";
            const tag = document.createElement("span");
            tag.className = "history-count-tag";
            tag.textContent = (item.vehicles ? item.vehicles.length : 0) + " mẫu xe";
            top.appendChild(date);
            top.appendChild(tag);

            const gridWrap = document.createElement("div");
            gridWrap.className = "history-vehicles-grid";
            (item.vehicles || []).forEach((v, idx) => {
                if (idx > 0) {
                    const vs = document.createElement("span");
                    vs.className = "history-vs";
                    vs.textContent = "vs";
                    gridWrap.appendChild(vs);
                }
                const vItem = document.createElement("div");
                vItem.className = "history-vehicle-item";
                if (v.imageUrl) {
                    const img = document.createElement("img");
                    img.src = v.imageUrl;
                    img.alt = v.modelName;
                    img.loading = "lazy";
                    img.onerror = function() { this.remove(); };
                    vItem.appendChild(img);
                }
                const info = document.createElement("div");
                info.className = "history-vehicle-info";
                const name = document.createElement("strong");
                name.textContent = v.modelName;
                const ver = document.createElement("small");
                ver.textContent = v.specificationVersion || "";
                info.appendChild(name);
                info.appendChild(ver);
                vItem.appendChild(info);
                gridWrap.appendChild(vItem);
            });

            const bottom = document.createElement("div");
            bottom.className = "history-card-bottom";

            const viewLink = document.createElement("a");
            viewLink.className = "btn-view-compare";
            viewLink.textContent = "Xem bảng so sánh →";
            const params = new URLSearchParams();
            (item.keys || []).forEach(k => params.append("key", k));
            viewLink.href = "<c:url value='/comparison'/>?" + params.toString();

            const deleteBtn = document.createElement("button");
            deleteBtn.type = "button";
            deleteBtn.className = "btn-delete-history";
            deleteBtn.textContent = "Xóa";
            deleteBtn.addEventListener("click", () => {
                CompareHistory.remove(item.id);
                updateBadge();
                renderHistory();
            });

            bottom.appendChild(viewLink);
            bottom.appendChild(deleteBtn);

            card.appendChild(top);
            card.appendChild(gridWrap);
            card.appendChild(bottom);
            historyList.appendChild(card);
        });
    }

    function openHistory() {
        renderHistory();
        if (historyDialog) historyDialog.showModal();
    }

    if (btnOpenHistory) btnOpenHistory.addEventListener("click", openHistory);
    if (historyCloseBtn) historyCloseBtn.addEventListener("click", () => historyDialog.close());
    if (historyDoneBtn) historyDoneBtn.addEventListener("click", () => historyDialog.close());
    if (historyDialog) {
        historyDialog.addEventListener("click", (e) => {
            if (e.target === historyDialog) historyDialog.close();
        });
    }

    if (btnClearAllHistory) {
        btnClearAllHistory.addEventListener("click", () => {
            if (confirm("Bạn có chắc chắn muốn xóa tất cả lịch sử so sánh xe?")) {
                CompareHistory.clear();
                updateBadge();
                renderHistory();
            }
        });
    }

    updateBadge();
})();
</script>
</body>
</html>
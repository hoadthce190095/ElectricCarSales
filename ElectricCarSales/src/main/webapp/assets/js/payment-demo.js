(() => {
    const orderCode = "EC-2026-001";
    const storageKey = "electricCarSales.paymentDemo.v1";
    const $ = (id) => document.getElementById(id);
    const money = (amount) => `${Math.max(0, Math.round(amount)).toLocaleString("vi-VN")} ₫`;
    const state = loadState();
    let activeTab = "direct";
    let settlementMode = false;

    function loadState() {
        try {
            const saved = JSON.parse(localStorage.getItem(storageKey) || "{}");
            return { payments: Array.isArray(saved.payments) ? saved.payments : [], installmentsPaid: Number(saved.installmentsPaid) || 0 };
        } catch (_) {
            return { payments: [], installmentsPaid: 0 };
        }
    }

    function persist() {
        localStorage.setItem(storageKey, JSON.stringify(state));
    }

    function price() {
        return Math.max(10000000, Number($("vehicle-price").value) || 1200000000);
    }

    function paidTotal() {
        return state.payments.reduce((sum, payment) => sum + payment.amount, 0);
    }

    function debt() {
        const amountAppliedToOrder = state.payments.reduce((sum, payment) => sum + (payment.balanceApplied ?? payment.amount), 0);
        return Math.max(0, price() - amountAppliedToOrder);
    }

    function quoteInstallment() {
        const rate = Number($("down-payment-rate").value) / 100;
        const downPayment = price() * rate;
        const principal = Math.max(0, price() - downPayment);
        const months = Number($("installment-term").value);
        const annualRate = Math.max(0, Number($("annual-rate").value) || 0) / 100;
        const monthlyRate = annualRate / 12;
        const monthly = monthlyRate === 0 ? principal / months : principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months));
        const totalInterest = Math.max(0, monthly * months - principal);
        return { rate, downPayment, principal, months, annualRate, monthlyRate, monthly, totalInterest };
    }

    function principalForInstallment(plan, index) {
        if (index >= plan.months) return 0;
        const outstandingPrincipal = plan.monthlyRate === 0 ? plan.principal * (1 - index / plan.months) : plan.principal * Math.pow(1 + plan.monthlyRate, index) - plan.monthly * (Math.pow(1 + plan.monthlyRate, index) - 1) / plan.monthlyRate;
        return Math.min(outstandingPrincipal, Math.max(0, plan.monthly - outstandingPrincipal * plan.monthlyRate));
    }

    function directAmount() {
        const dueType = $("payment-type").value;
        if (dueType === "deposit") return Math.min(debt(), price() * 0.30);
        if (dueType === "remaining") return debt();
        return Math.min(debt(), Math.max(0, Number($("custom-amount").value) || 0));
    }

    function currentQuote() {
        if (activeTab === "installment" && settlementMode) {
            return { amount: Math.max(0, debt() * 1.02), kind: "settlement", method: "BANK_TRANSFER", memo: "TATTOAN", caption: "QR demo tất toán trước hạn" };
        }
        if (activeTab === "direct") {
            const type = $("payment-type").value;
            const typeNames = { deposit: "DATCOC", remaining: "TATTOAN", custom: "THANHTOAN" };
            return { amount: directAmount(), kind: type, method: $("payment-method").value, memo: typeNames[type], caption: type === "deposit" ? "QR demo cho khoản đặt cọc" : type === "remaining" ? "QR demo thanh toán công nợ" : "QR demo thanh toán một phần" };
        }
        const plan = quoteInstallment();
        const isMonthly = $("installment-payment-kind").value === "monthly";
        const remainingDownPayment = Math.max(0, plan.downPayment - paidTotal());
        return { amount: isMonthly ? (debt() > 0 ? plan.monthly : 0) : Math.min(debt(), remainingDownPayment), kind: isMonthly ? "installment" : "down-payment", method: "BANK_TRANSFER", memo: isMonthly ? `KY${state.installmentsPaid + 1}` : "TRATRUOC", caption: isMonthly ? `QR demo kỳ trả góp ${state.installmentsPaid + 1}` : "QR demo khoản trả trước" };
    }

    function updateQuote() {
        const selectedType = $("payment-type").value;
        $("custom-amount-wrap").classList.toggle("is-hidden", selectedType !== "custom");
        const plan = quoteInstallment();
        $("down-payment-rate").querySelector("option[value='30']").textContent = `30% · ${money(price() * 0.30)}`;
        $("down-payment-rate").querySelector("option[value='40']").textContent = `40% · ${money(price() * 0.40)}`;
        $("down-payment-rate").querySelector("option[value='50']").textContent = `50% · ${money(price() * 0.50)}`;
        $("loan-amount").textContent = money(plan.principal);
        $("monthly-payment").textContent = money(plan.monthly);
        $("interest-total").textContent = money(plan.totalInterest);
        $("settlement-amount").textContent = money(Math.max(0, debt() * 1.02));

        const quote = currentQuote();
        $("due-amount").textContent = money(quote.amount);
        $("qr-amount").textContent = money(quote.amount);
        $("qr-caption").textContent = quote.caption;
        $("due-note").textContent = activeTab === "installment" ? (quote.kind === "installment" ? `Kỳ ${state.installmentsPaid + 1} · lãi suất tham khảo ${($("annual-rate").value || 0)}%/năm.` : `Trả trước ${(plan.rate * 100).toFixed(0)}% theo phương án trả góp.`) : (selectedType === "deposit" ? "Tiền cọc được tính bằng 30% giá trị đơn." : selectedType === "remaining" ? "Thanh toán phần công nợ còn lại của đơn." : "Số tiền demo không vượt quá công nợ hiện tại.");
        const reference = `DEMO ${orderCode} ${quote.memo}`;
        $("qr-reference").textContent = reference;
        renderQr(`${reference}|${Math.round(quote.amount)}|NO_REAL_PAYMENT`);
        $("simulate-payment").disabled = quote.amount <= 0;
        $("simulate-payment").innerHTML = quote.amount <= 0 ? "Không còn công nợ" : "Mô phỏng đã thanh toán <span>→</span>";
        renderSchedule(plan);
        renderSummary();
    }

    function renderQr(payload) {
        const qr = $("demo-qr");
        const fallback = $("qr-fallback");
        qr.replaceChildren();
        fallback.classList.add("is-hidden");
        $("qr-text-fallback").textContent = payload;
        if (typeof QRCode === "undefined") {
            fallback.classList.remove("is-hidden");
            return;
        }
        new QRCode(qr, { text: payload, width: 208, height: 208, colorDark: "#17191d", colorLight: "#ffffff", correctLevel: QRCode.CorrectLevel.M });
    }

    function renderSchedule(plan) {
        const body = $("schedule-body");
        body.replaceChildren();
        const today = new Date();
        for (let index = 0; index < plan.months; index += 1) {
            const dueDate = new Date(today.getFullYear(), today.getMonth() + index + 1, 10);
            const outstandingPrincipal = plan.monthlyRate === 0 ? plan.principal * (1 - index / plan.months) : plan.principal * Math.pow(1 + plan.monthlyRate, index) - plan.monthly * (Math.pow(1 + plan.monthlyRate, index) - 1) / plan.monthlyRate;
            const interestPart = Math.max(0, outstandingPrincipal * plan.monthlyRate);
            const principalPart = principalForInstallment(plan, index);
            const row = document.createElement("tr");
            const status = index < state.installmentsPaid ? "ĐÃ TRẢ" : index === state.installmentsPaid ? "KỲ TIẾP THEO" : "CHƯA ĐẾN HẠN";
            [String(index + 1).padStart(2, "0"), dueDate.toLocaleDateString("vi-VN"), money(principalPart), money(interestPart), money(plan.monthly), status].forEach((value, cellIndex) => {
                const cell = document.createElement("td");
                cell.textContent = value;
                if (cellIndex === 5) cell.className = index < state.installmentsPaid ? "schedule-paid" : "schedule-pending";
                row.appendChild(cell);
            });
            body.appendChild(row);
        }
    }

    function renderSummary() {
        const total = paidTotal();
        $("paid-total").textContent = money(total);
        $("debt-total").textContent = money(debt());
        $("paid-progress").style.width = `${Math.min(100, total / price() * 100)}%`;
        $("demo-order-status").textContent = debt() <= 0 ? "ĐÃ THANH TOÁN" : total > 0 ? "ĐANG THANH TOÁN" : "CÒN CÔNG NỢ";
        $("history-empty").classList.toggle("is-hidden", state.payments.length > 0);
        const history = $("history-list");
        history.replaceChildren();
        [...state.payments].reverse().forEach((payment) => {
            const item = document.createElement("div");
            item.className = "demo-history-row";
            const description = document.createElement("div");
            const title = document.createElement("strong");
            title.textContent = payment.label;
            const meta = document.createElement("span");
            meta.textContent = `${payment.reference} · ${new Date(payment.date).toLocaleString("vi-VN")}`;
            const amount = document.createElement("b");
            amount.textContent = money(payment.amount);
            const badge = document.createElement("i");
            badge.textContent = "DEMO ĐÃ GHI NHẬN";
            description.append(title, meta, badge);
            item.append(description, amount);
            history.appendChild(item);
        });
    }

    document.querySelectorAll(".demo-tab").forEach((button) => button.addEventListener("click", () => {
        settlementMode = false;
        activeTab = button.dataset.tab;
        document.querySelectorAll(".demo-tab").forEach((tab) => {
            const selected = tab === button;
            tab.classList.toggle("is-active", selected);
            tab.setAttribute("aria-selected", String(selected));
        });
        $("direct-panel").classList.toggle("is-hidden", activeTab !== "direct");
        $("installment-panel").classList.toggle("is-hidden", activeTab !== "installment");
        $("installment-panel").setAttribute("aria-hidden", String(activeTab !== "installment"));
        updateQuote();
    }));

    ["vehicle-price", "payment-type", "custom-amount", "payment-method", "down-payment-rate", "installment-term", "annual-rate", "installment-payment-kind"].forEach((id) => $(id).addEventListener("input", updateQuote));
    ["payment-type", "payment-method", "down-payment-rate", "installment-term", "installment-payment-kind"].forEach((id) => $(id).addEventListener("change", updateQuote));

    $("simulate-payment").addEventListener("click", () => {
        const quote = currentQuote();
        if (!quote || quote.amount <= 0 || (quote.kind !== "settlement" && quote.kind !== "installment" && quote.amount > debt())) return;
        const reference = `DEMO-${Date.now().toString().slice(-8)}`;
        const plan = quoteInstallment();
        const balanceApplied = quote.kind === "installment" ? principalForInstallment(plan, state.installmentsPaid) : quote.kind === "settlement" ? debt() : quote.amount;
        state.payments.push({ amount: quote.amount, balanceApplied, date: new Date().toISOString(), label: quote.kind === "installment" ? `Kỳ trả góp ${state.installmentsPaid + 1}` : quote.kind === "settlement" ? "Tất toán trước hạn" : quote.kind === "down-payment" ? "Trả trước hợp đồng trả góp" : quote.kind === "deposit" ? "Đặt cọc đơn hàng" : quote.kind === "remaining" ? "Thanh toán công nợ" : "Thanh toán một phần", reference });
        if (quote.kind === "installment") state.installmentsPaid += 1;
        persist();
        updateQuote();
        $("qr-caption").textContent = "Đã mô phỏng ghi nhận · tạo QR mới để tiếp tục";
    });

    $("settlement-button").addEventListener("click", () => {
        activeTab = "installment";
        document.querySelector(".demo-tab[data-tab='installment']").click();
        settlementMode = true;
        updateQuote();
        $("due-note").textContent = "Dư nợ gốc còn lại cộng phí tất toán mô phỏng 2%.";
    });

    $("reset-demo").addEventListener("click", () => {
        state.payments = [];
        state.installmentsPaid = 0;
        settlementMode = false;
        persist();
        updateQuote();
    });

    updateQuote();
})();

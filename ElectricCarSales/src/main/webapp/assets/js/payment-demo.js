(() => {
    const storageKey = "electricCarSales.gatewayPaymentDemo.v3";
    const $ = (id) => document.getElementById(id);
    const money = (value) => `${Math.max(0, Math.round(value)).toLocaleString("vi-VN")} ₫`;
    const channelNames = { card: "Thẻ đã liên kết", momo: "MoMo", napas: "NAPAS / thẻ ghi nợ", bank: "Ngân hàng trực tuyến" };
    const state = readState();
    if (!state.selectedCard && state.cards.length) state.selectedCard = state.cards[0].id;
    let activeTab = "direct";
    let activeChannel = "momo";
    let settlementMode = false;

    function readState() {
        try {
            const data = JSON.parse(localStorage.getItem(storageKey) || "{}");
            return { cards: Array.isArray(data.cards) ? data.cards : [], payments: Array.isArray(data.payments) ? data.payments : [], selectedCard: data.selectedCard || null, installmentCount: Number(data.installmentCount) || 0 };
        } catch (_) {
            return { cards: [], payments: [], selectedCard: null, installmentCount: 0 };
        }
    }

    function saveState() { localStorage.setItem(storageKey, JSON.stringify(state)); }
    function price() { return Math.max(10000000, Number($("vehicle-price").value) || 1200000000); }
    function totalPaid() { return state.payments.reduce((sum, payment) => sum + payment.amount, 0); }
    function principalPaid() { return state.payments.reduce((sum, payment) => sum + (payment.principalApplied ?? payment.amount), 0); }
    function balance() { return Math.max(0, price() - principalPaid()); }

    function plan() {
        const downRate = Number($("down-payment-rate").value) / 100;
        const principal = price() * (1 - downRate);
        const months = Number($("installment-term").value);
        const monthlyRate = Math.max(0, Number($("annual-rate").value) || 0) / 1200;
        const monthly = monthlyRate ? principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months)) : principal / months;
        return { downRate, principal, months, monthlyRate, monthly, interest: Math.max(0, monthly * months - principal) };
    }

    function principalForPeriod(financePlan, index) {
        if (index >= financePlan.months) return 0;
        const outstanding = financePlan.monthlyRate === 0
            ? financePlan.principal * (1 - index / financePlan.months)
            : financePlan.principal * Math.pow(1 + financePlan.monthlyRate, index) - financePlan.monthly * (Math.pow(1 + financePlan.monthlyRate, index) - 1) / financePlan.monthlyRate;
        return Math.min(outstanding, Math.max(0, financePlan.monthly - outstanding * financePlan.monthlyRate));
    }

    function directAmount() {
        const type = $("payment-type").value;
        if (type === "deposit") return Math.min(balance(), price() * 0.30);
        if (type === "remaining") return balance();
        return Math.min(balance(), Math.max(0, Number($("custom-amount").value) || 0));
    }

    function currentQuote() {
        if (activeTab === "installment" && settlementMode) return { amount: balance() * 1.02, type: "settlement", label: "Tất toán trước hạn" };
        if (activeTab === "direct") {
            const type = $("payment-type").value;
            const labels = { deposit: "Đặt cọc đơn hàng", remaining: "Thanh toán công nợ", custom: "Thanh toán một phần" };
            return { amount: directAmount(), type, label: labels[type] };
        }
        const selectedPlan = plan();
        const monthly = $("installment-payment-kind").value === "monthly";
        const downPaymentDue = Math.max(0, price() * selectedPlan.downRate - principalPaid());
        return { amount: monthly ? (balance() > 0 ? selectedPlan.monthly : 0) : Math.min(balance(), downPaymentDue), type: monthly ? "installment" : "down-payment", label: monthly ? `Kỳ trả góp ${state.installmentCount + 1}` : "Khoản trả trước" };
    }

    function update() {
        const selectedType = $("payment-type").value;
        $("custom-amount-wrap").classList.toggle("is-hidden", selectedType !== "custom");
        const selectedPlan = plan();
        $("loan-amount").textContent = money(selectedPlan.principal);
        $("monthly-payment").textContent = money(selectedPlan.monthly);
        $("interest-total").textContent = money(selectedPlan.interest);
        $("settlement-amount").textContent = money(balance() * 1.02);
        const current = currentQuote();
        $("due-amount").textContent = money(current.amount);
        $("gateway-amount").textContent = money(current.amount);
        $("due-note").textContent = activeTab === "installment"
            ? current.type === "installment" ? `Kỳ ${state.installmentCount + 1}, lãi suất ${$("annual-rate").value || 0}%/năm (ước tính).` : current.type === "settlement" ? "Dư nợ cộng phí tất toán mô phỏng 2%." : `Trả trước ${(selectedPlan.downRate * 100).toFixed(0)}% theo phương án này.`
            : selectedType === "deposit" ? "Đặt cọc bằng 30% giá trị đơn." : selectedType === "remaining" ? "Thanh toán toàn bộ công nợ hiện tại." : "Số tiền demo không vượt công nợ.";
        $("gateway-result").classList.add("is-hidden");
        renderSchedule(selectedPlan);
        renderSummary();
        updateChannel();
    }

    function updateChannel() {
        document.querySelectorAll(".gateway-option").forEach((option) => {
            const active = option.dataset.channel === activeChannel;
            option.classList.toggle("is-selected", active);
            option.setAttribute("aria-checked", String(active));
        });
        $("linked-card-area").classList.toggle("is-hidden", activeChannel !== "card");
        const card = state.cards.find((item) => item.id === state.selectedCard) || state.cards[0];
        const note = {
            card: card ? `Thanh toán bằng thẻ demo ${card.brand} •••• ${card.lastFour}.` : "Thêm thẻ demo để chọn phương thức này.",
            momo: "Luồng MoMo được mô phỏng; chưa mở ứng dụng thật.",
            napas: "Luồng thẻ ghi nợ NAPAS được mô phỏng.",
            bank: "Luồng ngân hàng trực tuyến được mô phỏng; không hiển thị QR."
        };
        $("selected-card-note").textContent = note[activeChannel];
        $("simulate-payment").disabled = currentQuote().amount <= 0 || (activeChannel === "card" && !card);
    }

    function renderSchedule(financePlan) {
        const body = $("schedule-body");
        body.replaceChildren();
        const today = new Date();
        for (let index = 0; index < financePlan.months; index += 1) {
            const date = new Date(today.getFullYear(), today.getMonth() + index + 1, 10);
            const principal = principalForPeriod(financePlan, index);
            const outstanding = financePlan.monthlyRate === 0
                ? financePlan.principal * (1 - index / financePlan.months)
                : financePlan.principal * Math.pow(1 + financePlan.monthlyRate, index) - financePlan.monthly * (Math.pow(1 + financePlan.monthlyRate, index) - 1) / financePlan.monthlyRate;
            const interest = Math.max(0, outstanding * financePlan.monthlyRate);
            const status = index < state.installmentCount ? "ĐÃ TRẢ" : index === state.installmentCount ? "KỲ TIẾP THEO" : "CHƯA ĐẾN HẠN";
            const row = document.createElement("tr");
            [String(index + 1).padStart(2, "0"), date.toLocaleDateString("vi-VN"), money(principal), money(interest), money(financePlan.monthly), status].forEach((value, cellIndex) => {
                const cell = document.createElement("td");
                cell.textContent = value;
                if (cellIndex === 5) cell.className = index < state.installmentCount ? "schedule-paid" : "schedule-pending";
                row.appendChild(cell);
            });
            body.appendChild(row);
        }
    }

    function renderSummary() {
        $("paid-total").textContent = money(totalPaid());
        $("debt-total").textContent = money(balance());
        $("paid-progress").style.width = `${Math.min(100, principalPaid() / price() * 100)}%`;
        $("order-status").textContent = balance() <= 0 ? "ĐÃ THANH TOÁN" : totalPaid() > 0 ? "ĐANG THANH TOÁN" : "CÒN CÔNG NỢ";
        $("history-empty").classList.toggle("is-hidden", state.payments.length > 0);
        const history = $("history-list");
        history.replaceChildren();
        [...state.payments].reverse().forEach((payment) => {
            const row = document.createElement("div");
            row.className = "paydemo-history-row";
            const info = document.createElement("div");
            const title = document.createElement("strong");
            const meta = document.createElement("span");
            const badge = document.createElement("i");
            const amount = document.createElement("b");
            title.textContent = payment.label;
            meta.textContent = `${payment.reference} · ${payment.channel} · ${new Date(payment.date).toLocaleString("vi-VN")}`;
            badge.textContent = "DEMO ĐÃ GHI NHẬN";
            amount.textContent = money(payment.amount);
            info.append(title, meta, badge);
            row.append(info, amount);
            history.appendChild(row);
        });
    }

    function renderCards() {
        const list = $("linked-card-list");
        list.replaceChildren();
        $("card-empty").classList.toggle("is-hidden", state.cards.length > 0);
        state.cards.forEach((card) => {
            const row = document.createElement("div");
            row.className = "paydemo-card-row";
            const radio = document.createElement("input");
            radio.type = "radio";
            radio.name = "linked-card";
            radio.checked = card.id === state.selectedCard;
            radio.setAttribute("aria-label", `${card.brand} ending ${card.lastFour}`);
            radio.addEventListener("change", () => { state.selectedCard = card.id; saveState(); update(); });
            const details = document.createElement("span");
            const brand = document.createElement("b");
            const lastFour = document.createElement("small");
            brand.textContent = card.brand;
            lastFour.textContent = `•••• ${card.lastFour} · demo`;
            details.append(brand, lastFour);
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "paydemo-remove-card";
            remove.textContent = "Gỡ";
            remove.addEventListener("click", () => {
                state.cards = state.cards.filter((item) => item.id !== card.id);
                if (state.selectedCard === card.id) state.selectedCard = state.cards[0]?.id || null;
                saveState();
                renderCards();
                update();
            });
            row.append(radio, details, remove);
            list.appendChild(row);
        });
    }

    document.querySelectorAll(".paydemo-tab").forEach((tab) => tab.addEventListener("click", () => {
        activeTab = tab.dataset.tab;
        settlementMode = false;
        document.querySelectorAll(".paydemo-tab").forEach((item) => {
            const selected = item === tab;
            item.classList.toggle("is-active", selected);
            item.setAttribute("aria-selected", String(selected));
        });
        $("direct-panel").classList.toggle("is-hidden", activeTab !== "direct");
        $("installment-panel").classList.toggle("is-hidden", activeTab !== "installment");
        $("installment-panel").setAttribute("aria-hidden", String(activeTab !== "installment"));
        update();
    }));

    document.querySelectorAll(".gateway-option").forEach((option) => option.addEventListener("click", () => {
        activeChannel = option.dataset.channel;
        $("gateway-result").classList.add("is-hidden");
        updateChannel();
    }));

    ["vehicle-price", "payment-type", "custom-amount", "down-payment-rate", "installment-term", "annual-rate", "installment-payment-kind"].forEach((id) => $(id).addEventListener("input", update));
    ["payment-type", "down-payment-rate", "installment-term", "installment-payment-kind"].forEach((id) => $(id).addEventListener("change", update));

    $("toggle-card-form").addEventListener("click", () => {
        const form = $("card-link-form");
        form.classList.toggle("is-hidden");
        $("toggle-card-form").textContent = form.classList.contains("is-hidden") ? "+ Thêm thẻ demo" : "Đóng";
    });

    $("card-link-form").addEventListener("submit", (event) => {
        event.preventDefault();
        const lastFour = $("card-last-four").value.trim();
        if (!/^\d{4}$/.test(lastFour)) return;
        const card = { id: `demo-card-${Date.now()}`, brand: $("card-brand").value, lastFour };
        state.cards.push(card);
        state.selectedCard = card.id;
        activeChannel = "card";
        saveState();
        event.currentTarget.reset();
        event.currentTarget.classList.add("is-hidden");
        $("toggle-card-form").textContent = "+ Thêm thẻ demo";
        renderCards();
        update();
    });

    $("simulate-payment").addEventListener("click", () => {
        const current = currentQuote();
        const card = state.cards.find((item) => item.id === state.selectedCard) || state.cards[0];
        if (current.amount <= 0 || (activeChannel === "card" && !card)) return;
        if (current.type !== "installment" && current.type !== "settlement" && current.amount > balance()) return;
        const financePlan = plan();
        const principalApplied = current.type === "installment" ? principalForPeriod(financePlan, state.installmentCount) : current.type === "settlement" ? balance() : current.amount;
        const method = activeChannel === "card" && card ? `${card.brand} •••• ${card.lastFour}` : channelNames[activeChannel];
        const record = { amount: current.amount, principalApplied, label: current.label, channel: method, date: new Date().toISOString(), reference: `DEMO-${Date.now().toString().slice(-8)}` };
        state.payments.push(record);
        if (current.type === "installment") state.installmentCount += 1;
        saveState();
        update();
        $("gateway-result").textContent = `Đã ghi nhận giao dịch demo ${record.reference} qua ${method}. Không có tiền thật được chuyển.`;
        $("gateway-result").classList.remove("is-hidden");
    });

    $("settlement-button").addEventListener("click", () => {
        document.querySelector(".paydemo-tab[data-tab='installment']").click();
        settlementMode = true;
        update();
    });

    $("reset-demo").addEventListener("click", () => {
        state.cards = [];
        state.payments = [];
        state.selectedCard = null;
        state.installmentCount = 0;
        settlementMode = false;
        activeChannel = "momo";
        saveState();
        renderCards();
        update();
    });

    renderCards();
    update();
})();


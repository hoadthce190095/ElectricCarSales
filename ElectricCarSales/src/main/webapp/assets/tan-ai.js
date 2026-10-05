"use strict";

function chatMessage(message) {
    const bubble = el("div", { class: "chat-bubble" }, [el("div", { text: message.text })]);
    if (message.response?.recommendations?.length) {
        bubble.append(
            el(
                "div",
                { class: "recommendation-list" },
                message.response.recommendations.map((r) => vehicleCard(r.vehicle, r.reasons))
            )
        );
    }
    if (message.response?.comparison?.length) {
        bubble.append(comparison(message.response.comparison));
    }
    if (message.response) {
        bubble.append(
            el("div", {
                class: "chat-meta",
                text: `Tư vấn theo dữ liệu danh mục · ${date(message.response.generatedAt)}`
            })
        );
    }
    return el("div", { class: `chat-message ${message.user ? "user" : "assistant"}` }, [
        el("span", { class: "chat-avatar", text: message.user ? "B" : "✧" }),
        bubble
    ]);
}

async function loadAI() {
    const [data] = await Promise.all([api("ai"), loadCatalogueMetadata()]);
    if (state.view !== "ai") {
        return;
    }
    state.models = data.models || [];
    let requestPending = false;
    const resetButton = button(
        "Cuộc trò chuyện mới",
        () =>
            task(async () => {
                if (requestPending) {
                    return;
                }
                requestPending = true;
                resetButton.disabled = true;
                send.disabled = true;
                try {
                    await api("ai?action=reset", { method: "POST", body: {} });
                    state.aiHistory = [];
                    state.cataloguePrompt = null;
                    await loadAI();
                    toast("Đã bắt đầu cuộc trò chuyện mới.");
                } finally {
                    requestPending = false;
                    resetButton.disabled = false;
                    send.disabled = false;
                }
            }),
        "button secondary"
    );
    $("#page-actions").replaceChildren(
        el("div", { class: "report-controls" }, [
            button("← Danh mục xe", () => showView("catalogue"), "button secondary"),
            resetButton
        ])
    );
    const messages = el("div", {
        class: "chat-messages",
        id: "chat-messages",
        role: "log",
        "aria-label": "Hội thoại tư vấn xe"
    });
    const intro = {
        text: "Chào bạn! Tôi có thể giúp bạn tìm hiểu thông số xe, so sánh mẫu xe và gợi ý theo ngân sách, quãng đường hoặc số chỗ. Bạn đang tìm chiếc xe như thế nào?"
    };
    messages.append(chatMessage(intro), ...state.aiHistory.map(chatMessage));
    const textarea = el("textarea", {
        name: "message",
        placeholder: "Ví dụ: Tôi cần xe 5 chỗ, ngân sách 800 triệu…",
        rows: 2,
        maxlength: 2000,
        required: true,
        "aria-label": "Câu hỏi tư vấn xe"
    });
    const send = el("button", { type: "submit", class: "button", text: "Gửi ↑" });
    const sendAI = async (message, intent = "AUTO", criteria = {}) => {
        if (requestPending) {
            return;
        }
        requestPending = true;
        send.disabled = true;
        resetButton.disabled = true;
        notice("");
        const userMessage = { user: true, text: message };
        state.aiHistory.push(userMessage);
        messages.append(chatMessage(userMessage));
        const pending = chatMessage({ text: "Đang đối chiếu với thông tin trong danh mục xe…" });
        messages.append(pending);
        messages.scrollTop = messages.scrollHeight;
        try {
            const response = await api("ai", { method: "POST", body: { message, intent, ...criteria } });
            const reply = { text: response.answer, response };
            state.aiHistory.push(reply);
            pending.replaceWith(chatMessage(reply));
        } catch (error) {
            pending.replaceWith(chatMessage({ text: error.message }));
            notice(error.message, true);
        } finally {
            requestPending = false;
            send.disabled = false;
            resetButton.disabled = false;
            messages.scrollTop = messages.scrollHeight;
        }
    };
    const chatForm = el(
        "form",
        {
            class: "chat-form",
            onsubmit: (event) => {
                event.preventDefault();
                const text = textarea.value.trim();
                if (!text) {
                    return;
                }
                textarea.value = "";
                sendAI(text);
            }
        },
        [
            el("div", { class: "chat-input-row" }, [textarea, send]),
            el("p", {
                class: "chat-disclaimer",
                text: "Tư vấn theo dữ liệu danh mục. Trợ lý hỗ trợ thông tin xe; nhân viên showroom tiếp nhận các yêu cầu giao dịch."
            })
        ]
    );
    textarea.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            chatForm.requestSubmit();
        }
    });
    const suggestions = [
        "Bạn có những mẫu xe nào?",
        "Tôi cần xe đi làm trong thành phố",
        "Xe nào có quãng đường di chuyển xa?"
    ];
    chatForm.append(
        el(
            "div",
            { class: "suggestions" },
            suggestions.map((text) =>
                button(
                    text,
                    () => {
                        textarea.value = text;
                        textarea.focus();
                    },
                    "suggestion"
                )
            )
        )
    );
    const chat = el("section", { class: "card" }, [
        el("div", { class: "ai-hero" }, [
            el("span", { class: "ai-symbol", text: "✧" }),
            el("div", {}, [
                el("h2", { text: "Chiếc xe phù hợp, bắt đầu từ bạn." }),
                el("p", { text: `Khám phá ${number(state.models.length)} mẫu xe từ danh mục showroom.` })
            ])
        ]),
        messages,
        chatForm
    ]);
    const recommendationForm = el(
        "form",
        {
            class: "card-body",
            onsubmit: (event) => {
                event.preventDefault();
                const input = values(recommendationForm);
                const budget = input.budget === "" ? null : Number(input.budget) * 1000000,
                    minRange = input.minRange === "" ? null : Number(input.minRange),
                    minSeats = input.minSeats === "" ? null : Number(input.minSeats);
                if (budget == null && minRange == null && minSeats == null) {
                    notice("Nhập ít nhất một nhu cầu để nhận gợi ý.");
                    return;
                }
                const parts = [
                    budget != null ? `ngân sách ${number(budget)} VND` : null,
                    minRange != null ? `quãng đường tối thiểu ${number(minRange)} km` : null,
                    minSeats != null ? `ít nhất ${minSeats} chỗ` : null
                ].filter(Boolean);
                sendAI(`Gợi ý xe phù hợp với ${parts.join(", ")}.`, "RECOMMEND", {
                    budget,
                    minRange,
                    minSeats
                });
            }
        },
        [
            field("budget", "Ngân sách tối đa (triệu đồng)", "number", "", { min: 0, step: "0.1" }),
            field("minRange", "Quãng đường tối thiểu (km)", "number", "", { min: 0, step: 1 }),
            field("minSeats", "Số chỗ tối thiểu", "number", "", { min: 1, max: 20, step: 1 }),
            el("button", { class: "button", type: "submit", text: "✧ Gợi ý xe phù hợp" })
        ]
    );
    const compareForm = el(
        "form",
        {
            class: "card-body",
            onsubmit: (event) => {
                event.preventDefault();
                const modelIds = [...$("[name=models]", compareForm).selectedOptions].map((o) => Number(o.value));
                if (modelIds.length < 2 || modelIds.length > 4) {
                    notice("Chọn từ 2 đến 4 mẫu xe để so sánh.");
                    return;
                }
                const names = state.models.filter((m) => modelIds.includes(m.modelId)).map((m) => m.modelName);
                sendAI(`So sánh ${names.join(" và ")}.`, "COMPARE", { modelIds });
            }
        },
        [
            field("models", "Mẫu xe", "select", [], {
                choices: state.models.map((m) => ({
                    value: m.modelId,
                    label: `${m.brandName} ${m.modelName}`
                })),
                multiple: true,
                help: "Chọn 2–4 mẫu xe. Giữ Ctrl để chọn nhiều mẫu."
            }),
            el("button", { class: "button secondary", type: "submit", text: "So sánh mẫu xe" })
        ]
    );
    const side = el("aside", { class: "ai-side" }, [
        card("Nhu cầu của bạn", "Gợi ý dựa trên tiêu chí bạn cung cấp.", recommendationForm),
        card("Đặt xe cạnh nhau", "Đối chiếu giá bán và thông số sản phẩm.", compareForm)
    ]);
    if (!state.models.length) {
        notice("Danh mục chưa có mẫu xe. Cần cập nhật dữ liệu sản phẩm để tư vấn và so sánh.");
    }
    $("#content").replaceChildren(el("div", { class: "ai-layout" }, [chat, side]));
    messages.scrollTop = messages.scrollHeight;
    if (state.cataloguePrompt) {
        const prompt = state.cataloguePrompt;
        state.cataloguePrompt = null;
        await sendAI(prompt.message, prompt.intent, prompt.criteria);
    }
}

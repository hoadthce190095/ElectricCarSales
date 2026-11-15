const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

class Element {
    constructor(tag = "div") {
        this.tag = tag;
        this.children = [];
        this.attributes = {};
        this.listeners = {};
        this.textContent = "";
        this.value = "";
        this.hidden = false;
        this.disabled = false;
        this.dataset = {};
    }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    setAttribute(name, value) { this.attributes[name] = String(value); }
    addEventListener(name, listener) { (this.listeners[name] ||= []).push(listener); }
    emit(name, extra = {}) {
        const event = { target: this, currentTarget: this, preventDefault() {}, ...extra };
        for (const listener of this.listeners[name] || []) listener(event);
    }
    reportValidity() { return true; }
    requestSubmit() { this.emit("submit"); }
    focus() { this.focused = true; }
    reset() { this.querySelectorAll("input").forEach((input) => input.value = ""); }
    querySelector(selector) { return this.querySelectorAll(selector)[0] || null; }
    querySelectorAll(selector) {
        return flatten(this).slice(1).filter((element) => {
            if (selector === "input") return element.tag === "input";
            const named = selector.match(/^\[name=(.*?)\]$/);
            if (named) return element.name === named[1];
            if (selector === '[type="submit"]') return element.type === "submit";
            return false;
        });
    }
}
const flatten = (element) => [element, ...element.children.flatMap(flatten)];
const text = (element) => flatten(element).map((item) => item.textContent).join(" ");
const flush = async () => { for (let index = 0; index < 4; index++) await new Promise(setImmediate); };
const source = fs.readFileSync(path.resolve(__dirname, "../../main/webapp/assets/js/customer-ai.js"), "utf8");
const models = [1, 2, 3, 4, 5].map((id) => ({ modelId: id, modelName: `Xe ${id}`, brandName: "VinFast", minPrice: 500000000 + id, specifications: { rangeKM: 350, seatingCapacity: 5, batteryCapacity: 50 } }));
const success = (data) => ({ ok: true, status: 200, json: async () => data });
const failure = (status, message) => ({ ok: false, status, json: async () => ({ error: message }) });
const answer = { answer: "Gợi ý theo nhu cầu của bạn <img src=x onerror=alert(1)>", recommendations: [{ vehicle: models[0], reasons: ["Trong ngân sách"] }], comparison: models.slice(0, 2) };

async function load(contextPath, { user = null, search = "", failInitial = false } = {}) {
    const ids = ["ai-messages", "ai-question", "ai-question-form", "ai-needs-form", "ai-compare-form", "ai-reset", "ai-reconnect", "ai-status", "ai-account-link", "ai-send", "ai-selected-count", "ai-model-choices", "ai-catalogue-status"];
    const elements = Object.fromEntries(ids.map((id) => [id, new Element()]));
    for (const id of ["ai-needs-form", "ai-compare-form"]) {
        const submit = new Element("button");
        submit.type = "submit";
        elements[id].append(submit);
    }
    for (const name of ["budget", "minRange", "minSeats"]) {
        const input = new Element("input");
        input.name = name;
        elements["ai-needs-form"].append(input);
    }
    elements["ai-compare-form"].append(elements["ai-model-choices"]);
    const suggestion = new Element("button");
    suggestion.dataset.aiPrompt = "Thông số Xe 2";
    const requests = [];
    const responses = [];
    let initialFailure = failInitial;
    const context = vm.createContext({
        URLSearchParams,
        window: { location: { search } },
        document: {
            createElement: (tag) => new Element(tag),
            querySelector: (selector) => selector === "[data-customer-ai]" ? elements["ai-question-form"] : selector === 'meta[name="app-context-path"]' ? { content: contextPath } : elements[selector.slice(1)] || null,
            querySelectorAll: (selector) => selector === "[data-ai-prompt]" ? [suggestion] : []
        },
        fetch: async (url, options) => {
            requests.push({ url, options });
            if (url === `${contextPath}/api/auth`) {
                if (initialFailure) { initialFailure = false; return failure(503, "Tạm thời mất kết nối"); }
                return success({ user, csrfToken: "customer-token" });
            }
            if (options.method === "GET") return success({ models });
            return responses.length ? await responses.shift() : success(url.includes("action=reset") ? { cleared: true } : answer);
        }
    });
    vm.runInContext(source, context, { filename: "customer-ai.js" });
    await flush();
    return { elements, requests, responses, suggestion };
}

(async () => {
    for (const contextPath of ["", "/ElectricCarSales", "/showroom"]) {
        const app = await load(contextPath);
        assert.deepEqual(app.requests.map((request) => request.url), [`${contextPath}/api/auth`, `${contextPath}/api/ai`]);
        assert.equal(app.elements["ai-account-link"].href, `${contextPath}/login`);
        assert.equal(app.elements["ai-send"].disabled, false, "Guests can use the actual assistant");
        app.elements["ai-question"].value = "Xe dưới 600 triệu";
        app.elements["ai-question-form"].emit("submit");
        await flush();
        const request = app.requests.at(-1);
        assert.equal(request.url, `${contextPath}/api/ai`);
        assert.equal(request.options.headers["X-CSRF-Token"], "customer-token");
        assert.equal(request.options.credentials, "same-origin");
        assert.equal(JSON.parse(request.options.body).message, "Xe dưới 600 triệu");
        assert.match(text(app.elements["ai-messages"]), /Trong ngân sách/);
        assert.equal(flatten(app.elements["ai-messages"]).filter((item) => item.tag === "table").length, 1);
        assert.equal(flatten(app.elements["ai-messages"]).filter((item) => item.tag === "img").length, 0, "Response text never becomes HTML");
        app.elements["ai-question"].value = "Thông số xe đó";
        app.elements["ai-question-form"].emit("submit");
        await flush();
        assert.match(text(app.elements["ai-messages"]), /Xe dưới 600 triệu/);
        assert.match(text(app.elements["ai-messages"]), /Thông số xe đó/);
    }

    const app = await load("/showroom", { user: { username: "customer", role: "CUSTOMER" }, search: "?prompt=So+s%C3%A1nh+Xe+1+v%C3%A0+Xe+2&intent=COMPARE&models=1,2,999" });
    assert.equal(app.elements["ai-account-link"].href, "/showroom/customer/profile");
    assert.equal(app.elements["ai-question"].value, "So sánh Xe 1 và Xe 2");
    assert.equal(app.requests.length, 2, "URL prompt is prefilled and waits for the user");
    app.elements["ai-question-form"].emit("submit");
    await flush();
    assert.deepEqual(JSON.parse(app.requests.at(-1).options.body).modelIds, [1, 2]);
    app.responses.push(failure(503, "Hãy thử lại"));
    app.elements["ai-question"].value = "Câu hỏi lỗi";
    app.elements["ai-question-form"].emit("submit");
    await flush();
    assert.equal(app.elements["ai-question"].value, "Câu hỏi lỗi");
    const retry = flatten(app.elements["ai-messages"]).find((item) => item.textContent === "Gửi lại câu hỏi");
    retry.emit("click");
    await flush();
    assert.equal(text(app.elements["ai-messages"]).split("Câu hỏi lỗi").length - 1, 1, "Retry reuses the failed message");
    assert.doesNotMatch(text(app.elements["ai-messages"]), /Hãy thử lại/);

    let finish;
    app.responses.push(new Promise((resolve) => finish = resolve));
    app.elements["ai-question"].value = "Câu đang xử lý";
    app.elements["ai-question-form"].emit("submit");
    app.elements["ai-question-form"].emit("submit");
    app.elements["ai-reset"].emit("click");
    assert.equal(app.requests.filter((request) => request.options.body?.includes("Câu đang xử lý")).length, 1);
    assert.equal(app.elements["ai-reset"].disabled, true);
    finish(success(answer));
    await flush();
    app.elements["ai-reset"].emit("click");
    await flush();
    assert.equal(app.requests.at(-1).url, "/showroom/api/ai?action=reset");
    assert.doesNotMatch(text(app.elements["ai-messages"]), /Câu đang xử lý/);
    assert.equal(app.elements["ai-question"].value, "");

    const choices = app.elements["ai-model-choices"].querySelectorAll("input");
    for (const checkbox of choices) { checkbox.checked = true; checkbox.emit("change"); }
    assert.equal(choices[4].checked, false);
    assert.match(app.elements["ai-status"].textContent, /tối đa 4/);
    app.elements["ai-compare-form"].emit("submit");
    await flush();
    assert.equal(JSON.parse(app.requests.at(-1).options.body).modelIds.length, 4);
    app.elements["ai-needs-form"].querySelector("[name=budget]").value = "750";
    app.elements["ai-needs-form"].emit("submit");
    await flush();
    assert.equal(JSON.parse(app.requests.at(-1).options.body).budget, 750000000);

    const unavailable = await load("/showroom", { failInitial: true });
    assert.equal(unavailable.elements["ai-send"].disabled, true);
    assert.equal(unavailable.elements["ai-reconnect"].hidden, false);
    unavailable.elements["ai-reconnect"].emit("click");
    await flush();
    assert.equal(unavailable.elements["ai-send"].disabled, false);
    console.log("Customer AI checks passed: guest/customer sessions, contexts/CSRF, prompt links, actual replies, recommendations/comparison, safe text, follow-up/retry/reset, request lock and reconnect.");
})().catch((error) => { console.error(error); process.exitCode = 1; });

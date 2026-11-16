const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

class Node {
    constructor(tag, attrs = {}, children = []) {
        this.tag = tag;
        Object.assign(this, attrs);
        this.children = [];
        this.listeners = {};
        this.classList = { add() {} };
        this.append(...children);
    }
    append(...children) {
        for (const child of children.filter(Boolean)) {
            child.parentElement = this;
            this.children.push(child);
        }
    }
    replaceChildren(...children) { this.children = []; this.append(...children); }
    setAttribute(name, value) { this[name] = value; }
    addEventListener(name, listener) { this.listeners[name] = listener; }
    setCustomValidity(message) { this.validationMessage = message; }
    get selectedOptions() { return (this.options || []).filter((option) => option.selected); }
}
const flatten = (node) => [node, ...node.children.flatMap(flatten)];
const nodes = new Map(["#dialog-form", "#page-actions", "#content"].map((id) => [id, new Node("div")]));
const lookup = {
    roles: [{ id: 1, name: "ADMIN" }, { id: 2, name: "SALES" }, { id: 3, name: "CUSTOMER" }],
    statuses: [{ id: 1, name: "ACTIVE" }],
    models: [{ id: 11, name: "EV" }, { id: 12, name: "Chưa có giá" }],
    modelMinPrices: { 11: "450000000.00" }
};
let lastModal;
let request;
const context = vm.createContext({
    console,
    state: { view: "promotions", viewGeneration: 1, userFilters: {}, employeeFilters: {} },
    lookups: async () => lookup,
    canView: () => true,
    isAdmin: () => true,
    roleLabels: { SALES: "Kinh doanh" },
    statusLabels: {},
    money: (value) => `${value} VND`,
    number: String,
    date: String,
    searchText: (value) => String(value).toLowerCase(),
    query: () => "",
    notice() {}, toast() {}, task: (fn) => fn(),
    beginViewLoad: () => ({}), isViewCurrent: () => true,
    readViewData: (_, fn) => fn(),
    api: (...args) => request(...args),
    $: (selector, root) => root ? flatten(root).find((node) => selector[0] === "#" ? node.id === selector.slice(1) : node.name === /^\[name=(.*)\]$/.exec(selector)?.[1]) : nodes.get(selector),
    el: (tag, attrs, children) => new Node(tag, attrs, children),
    field: (name, label, type, value, options = {}) => {
        const input = new Node(type === "select" ? "select" : "input", { name, type, value, ...options });
        input.options = (options.choices || []).map((option) => ({ value: String(option.value), selected: options.multiple ? value.map(String).includes(String(option.value)) : String(value) === String(option.value) }));
        return new Node("label", { label }, [input]);
    },
    modal: (title, fields, submit, submitLabel) => {
        nodes.get("#dialog-form").replaceChildren(...fields);
        lastModal = { title, fields, submit, submitLabel };
    },
    button: (text, onclick) => new Node("button", { text, onclick }),
    cellTitle: (text, subtext) => new Node("cell", { text, subtext }),
    pill: (text) => new Node("pill", { text }),
    card: (title, help, content) => new Node("card", { title, help }, [content]),
    table: (headings, rows) => new Node("table", { headings, rows }),
    empty: () => new Node("empty"), pager: () => new Node("pager")
});
const source = fs.readFileSync(path.resolve(__dirname, "../../main/webapp/assets/admin-management.js"), "utf8");
vm.runInContext(source, context);
const run = (code) => vm.runInContext(code, context);
const read = (code) => JSON.parse(run(`JSON.stringify(${code})`));
const valid = { discountType: "PERCENTAGE", discountValue: "10", startDate: "2026-01-31T10:30", endDate: "2026-04-30T10:30", modelIds: [11] };
const validation = (input) => read(`promotionValidation(${JSON.stringify({ ...valid, ...input })}, ${JSON.stringify(lookup.modelMinPrices)})`);

(async () => {
    for (const [start, end] of [
        ["2026-01-31T10:30", "2026-04-30T10:30"],
        ["2023-11-30T10:30", "2024-02-29T10:30"],
        ["2024-02-29T10:30", "2024-05-29T10:30"],
        ["2026-08-31T10:30", "2026-11-30T10:30"]
    ]) {
        assert.equal(run(`promotionEndLimit('${start}')`), end);
        assert.deepEqual(validation({ startDate: start, endDate: end }), {});
        assert.match(validation({ startDate: start, endDate: end.slice(0, 14) + "31" }).endDate, /3 tháng lịch/);
    }
    assert.match(validation({ endDate: valid.startDate }).endDate, /sau ngày/);
    assert.match(validation({ startDate: "2026-02-30T10:30" }).startDate, /hợp lệ/);
    for (const discountValue of ["0", "-1", "0.001", "100.01", "99999999999999999.00"]) {
        assert.ok(validation({ discountValue }).discountValue);
    }
    assert.deepEqual(validation({ discountValue: "100.00" }), {});
    assert.deepEqual(validation({ discountValue: "0.010" }), {});
    assert.deepEqual(validation({ discountType: "FIXED", discountValue: "450000000.00" }), {});
    assert.ok(validation({ discountType: "FIXED", discountValue: "450000000.01" }).discountValue);
    assert.deepEqual(validation({ discountType: "FIXED", discountValue: "500000000.00", modelIds: [12] }), {});
    assert.ok(validation({ modelIds: [] }).modelIds);
    assert.equal(run("typeof previewPromotion"), "undefined");
    assert.doesNotMatch(source, /Tính thử/);

    await run("editPromotion()");
    const form = nodes.get("#dialog-form");
    const control = (name) => flatten(form).find((node) => node.name === name);
    control("startDate").value = "2023-11-30T10:30";
    control("startDate").listeners.input();
    assert.equal(control("endDate").max, "2024-02-29T10:30");
    assert.equal(control("discountValue").min, "0.01");
    assert.equal(control("discountValue").max, "100");
    control("discountType").value = "FIXED";
    control("modelIds").options[0].selected = true;
    control("discountType").listeners.change();
    assert.equal(control("discountValue").max, "450000000");
    control("discountValue").value = "450000000.01";
    control("discountValue").listeners.input();
    assert.match(control("discountValue").validationMessage, /giá xe thấp nhất/);
    assert.equal(flatten(form).find((node) => node.id === "promotion-discountValue-error").hidden, false);

    run("state.view='users'");
    await run("editUser({username:'staff',userId:71,employeeId:81,role:'SALES',roleId:2,statusId:1})");
    assert.deepEqual(flatten(nodes.get("#dialog-form")).filter((node) => node.name).map((node) => node.name), ["roleId", "statusId"]);
    const requests = [];
    request = async (url, options) => { requests.push({ url, options }); return { items: [], total: 0, page: 1, pageSize: 20 }; };
    await lastModal.submit({ roleId: "2", statusId: "1" });
    assert.deepEqual(Object.keys(requests[0].options.body), ["roleId", "statusId"]);
    assert.equal(nodes.get("#page-actions").children.length, 0, "accounts must not offer a duplicate creation flow");

    run("state.view='employees'");
    await run("editUser()");
    assert.ok(flatten(nodes.get("#dialog-form")).some((node) => node.name === "employeeName"));
    assert.ok(flatten(nodes.get("#dialog-form")).some((node) => node.name === "username"));
    await run("editEmployee({employeeId:81,userId:71,username:'staff',employeeName:'Staff',email:'s@test.local',phone:'0900000001'})");
    assert.deepEqual(flatten(nodes.get("#dialog-form")).filter((node) => node.name).map((node) => node.name), ["employeeName", "email", "phone"]);
    assert.ok(flatten(nodes.get("#dialog-form")).some((node) => node.text?.includes("UserID #71")));
    console.log("Admin management checks passed: calendar-month boundaries, exact discount validation, field errors, dynamic bounds and separate account/profile forms.");
})().catch((error) => { console.error(error); process.exitCode = 1; });

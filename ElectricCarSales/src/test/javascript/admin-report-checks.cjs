const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

class Node {
    constructor(tag, text = "") {
        this.tag = tag;
        this.nodeType = 1;
        this.children = [];
        this.attributes = {};
        this.listeners = {};
        this.textContent = text;
        this.classList = { add() {}, toggle() {} };
    }
    setAttribute(name, value) {
        this.attributes[name] = value;
        if (name === "value" || name === "name") this[name] = value;
    }
    append(...children) {
        this.children.push(...children);
    }
    replaceChildren(...children) {
        this.children = children;
    }
    addEventListener(name, listener) {
        this.listeners[name] = listener;
    }
    querySelector(selector) {
        const match = /^(\w+)\[name=(\w+)\]$/.exec(selector);
        return match ? flatten(this).find((node) => node.tag === match[1] && node.attributes.name === match[2]) : null;
    }
}

const nodes = new Map(["#content", "#page-actions", "#notice", "#toast"].map((id) => [id, new Node("div")]));
const document = {
    createElement: (tag) => new Node(tag),
    createTextNode: (text) => new Node("#text", text),
    querySelector: (selector) =>
        selector === 'meta[name="app-context-path"]' ? { content: "/showroom" } : nodes.get(selector) || null
};
let request;
const context = vm.createContext({
    console,
    document,
    URLSearchParams,
    request: (...args) => request(...args),
    setTimeout: () => 1,
    clearTimeout() {}
});
const assets = path.resolve(__dirname, "../../main/webapp/assets");
vm.runInContext(
    ["admin-common.js", "admin-management.js", "sales-reports.js"]
        .map((name) => fs.readFileSync(path.join(assets, name), "utf8"))
        .join("\n"),
    context
);
vm.runInContext(
    "api=(path,options)=>request(path,options); loadCatalogueMetadata=async()=>{}; state.user={username:'admin',role:'ADMIN'};",
    context
);
const run = (source) => vm.runInContext(source, context);
const read = (source) => JSON.parse(run(`JSON.stringify(${source})`));
const flatten = (node) => [node, ...node.children.flatMap(flatten)];
const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((yes, no) => {
        resolve = yes;
        reject = no;
    });
    return { promise, resolve, reject };
};
const flush = () => new Promise((resolve) => setImmediate(resolve));
const users = { items: [], total: 0, page: 1, pageSize: 20 };
const report = (from, to = from) => ({ period: { from, to }, summary: {}, models: [], employees: [] });

(async () => {
    run("state.view='users'; state.lookups={roles:[],statuses:[]};");
    const first = deferred();
    request = (url) => (url.includes("q=old") ? first.promise : Promise.resolve(users));
    const oldUsers = run("loadUsers({q:'old'},1)");
    await run("loadUsers({q:'new'},1)");
    const renderedUsers = nodes.get("#content").children[0];
    first.resolve(users);
    await oldUsers;
    assert.equal(read("state.userFilters.q"), "new", "a late filter response must not replace the current filter");
    assert.equal(nodes.get("#content").children[0], renderedUsers);

    const failed = deferred();
    request = (url) => (url.includes("q=failed") ? failed.promise : Promise.resolve(users));
    const staleFailure = run("loadUsers({q:'failed'},1)");
    await run("loadUsers({q:'latest'},1)");
    failed.reject(new Error("old request failed"));
    await staleFailure;
    assert.equal(read("state.userFilters.q"), "latest", "a stale failed request must be ignored");

    const navigated = deferred();
    request = () => navigated.promise;
    const previousVisit = run("loadUsers({q:'previous-visit'},1)");
    run("state.viewGeneration+=2; state.view='users';");
    navigated.resolve(users);
    await previousVisit;
    assert.equal(read("state.userFilters.q"), "latest", "returning to a view must not revive a previous visit");

    run("state.view='reports'; state.report=null; state.reportFilters={}; state.reportEmployees=[];");
    const comparison = deferred();
    const comparedRequest = deferred();
    request = (url) => {
        if (url === "catalogue") {
            return Promise.resolve({ models: [] });
        }
        const query = new URLSearchParams(url.split("?")[1]);
        if (query.get("from") === "2026-09-30") {
            comparedRequest.resolve();
            return comparison.promise;
        }
        return Promise.resolve(report(query.get("from"), query.get("to")));
    };
    const olderReport = run("loadReports({from:'2026-10-01',to:'2026-10-01',compare:'PREVIOUS'})");
    await comparedRequest.promise;
    assert.equal(read("state.report"), null, "report state must wait for its comparison before committing");
    await run("loadReports({from:'2026-10-02',to:'2026-10-02',compare:'NONE'})");
    comparison.resolve(report("2026-09-30"));
    await olderReport;
    assert.equal(read("state.report.period.from"), "2026-10-02");
    assert.equal(read("state.reportComparison"), "NONE");
    const exports = flatten(nodes.get("#page-actions"))
        .filter((node) => node.tag === "a")
        .map((node) => node.attributes.href);
    assert.deepEqual(exports, [
        "/showroom/api/reports?from=2026-10-02&to=2026-10-02&format=xlsx",
        "/showroom/api/reports?from=2026-10-02&to=2026-10-02&format=csv"
    ]);

    let reportRequests = 0;
    request = (url) => {
        if (url === "catalogue") {
            return Promise.resolve({ models: [] });
        }
        reportRequests++;
        const error = new Error("Mẫu xe không tồn tại.");
        error.status = 400;
        return Promise.reject(error);
    };
    await assert.rejects(run("loadReports({from:'2026-10-01',to:'2026-10-01',modelId:'999'})"), /Mẫu xe/);
    assert.equal(
        reportRequests,
        1,
        "an explicit invalid filter must not silently fall back to today's unfiltered data"
    );
    await assert.rejects(run("loadReports({from:'2026-10-02',to:'2026-10-01'})"), /Ngày bắt đầu/);
    assert.equal(reportRequests, 1, "an inverted date range must be rejected before sending a request");

    const requestedPeriods = [];
    request = async (url) => {
        if (url === "catalogue") {
            return { models: [] };
        }
        const query = new URLSearchParams(url.split("?")[1]);
        requestedPeriods.push(query.get("from"));
        return report(query.get("from"), query.get("to"));
    };
    run("state.reportComparison='PREVIOUS';");
    await run("loadReports({from:'2026-10-03',to:'2026-10-03'})");
    assert.equal(read("state.reportComparison"), "NONE", "a reset or preset must not inherit the old comparison");
    assert.deepEqual(requestedPeriods, ["2026-10-03"]);

    run("state.view='overview'; state.overviewFilters={};");
    const overviewRequests = [];
    request = async (url) => {
        overviewRequests.push(url);
        const query = new URLSearchParams(url.split("?")[1]);
        return {
            ...report(query.get("from"), query.get("to")),
            sales: [{ orderId: 4, customerName: "Test customer", totalAmount: 500000000, status: "PAID" }],
            inventory: [{ status: "AVAILABLE", count: 2, amount: 1000000000 }]
        };
    };
    await run("loadOverview({from:'2026-10-01',to:'2026-10-07',preset:'CUSTOM'})");
    assert.deepEqual(read("state.overviewFilters"), { from: "2026-10-01", to: "2026-10-07", preset: "CUSTOM" });
    assert.equal(nodes.get("#content").children[0].attributes["aria-label"], "Khoảng thời gian tổng quan");
    const overviewNodes = flatten(nodes.get("#content"));
    assert.ok(overviewNodes.some((node) => node.className === "overview-details" && node.children.length === 2));
    assert.ok(overviewNodes.some((node) => node.textContent === "Test customer"));
    assert.equal(overviewNodes.filter((node) => node.tag === "article").length, 4, "overview must retain four KPIs");
    assert.equal(
        overviewNodes.filter((node) => node.tag === "button" && node.textContent === "Làm mới").length,
        0,
        "refresh is provided once in the topbar"
    );
    const overviewForm = overviewNodes.find((node) => node.className === "overview-period-form");
    const presetInput = overviewForm.querySelector("select[name=preset]");
    presetInput.value = "YEAR";
    overviewForm.listeners.change({ target: presetInput });
    assert.equal(overviewForm.querySelector("input[name=from]").value, `${read("isoToday().slice(0,4)")}-01-01`);
    overviewForm.listeners.change({ target: overviewForm.querySelector("input[name=from]") });
    assert.equal(presetInput.value, "CUSTOM", "editing a date must stop claiming a preset period");
    await run("loadOverview()");
    assert.equal(overviewRequests[1], overviewRequests[0], "refresh must preserve the selected overview period");
    await assert.rejects(run("loadOverview({from:'2026-10-08',to:'2026-10-07'})"), /Ngày bắt đầu/);
    assert.equal(overviewRequests.length, 2, "invalid overview dates must fail before a request");

    const oldOverview = deferred();
    request = (url) => (url.includes("2026-10-01") ? oldOverview.promise : Promise.resolve(report("2026-10-09")));
    const pendingOverview = run("loadOverview({from:'2026-10-01',to:'2026-10-01'})");
    await run("loadOverview({from:'2026-10-09',to:'2026-10-09'})");
    oldOverview.resolve(report("2026-10-01"));
    await pendingOverview;
    assert.equal(
        read("state.overviewFilters.from"),
        "2026-10-09",
        "a late overview result must not replace the active period"
    );

    run("state.view='logs'; state.logFilters={}; state.logPage=1;");
    const logs = [];
    const dangerousDetail = '<img src=x onerror="alert(1)">';
    request = async (url) => {
        logs.push(url);
        return {
            items: [{ logId: 8, action: "LOGIN", detail: dangerousDetail }],
            total: 42,
            page: Number(new URLSearchParams(url.split("?")[1]).get("page")),
            pageSize: 20
        };
    };
    await run("loadLogs({action:'LOGIN'},2)");
    await run("loadLogs()");
    assert.equal(logs[1], "admin/logs?action=LOGIN&page=2", "refresh must preserve the log filter and page");
    const detail = flatten(nodes.get("#content")).find((node) => node.textContent === dangerousDetail);
    assert.equal(detail.tag, "p", "log content must remain plain text");
    assert.equal(detail.children.length, 0);
    assert.ok(flatten(nodes.get("#content")).some((node) => node.tag === "details"));
    await assert.rejects(run("loadLogs({from:'2026-10-02',to:'2026-10-01'},1)"), /Ngày bắt đầu/);
    assert.equal(logs.length, 2);

    await run("refreshAdminAfterSave(async()=>{throw new Error('Mất kết nối');}, 'Đã tạo tài khoản.')");
    assert.match(nodes.get("#notice").textContent, /Đã tạo tài khoản.*Mất kết nối/);

    const lookup = deferred();
    let openedDialog = false;
    context.dialogOpened = () => {
        openedDialog = true;
    };
    run("lookups=()=>request(); modal=()=>dialogOpened(); state.view='users';");
    request = () => lookup.promise;
    const pendingDialog = run("editUser()");
    run("state.viewGeneration++; state.view='reports';");
    lookup.resolve({ roles: [], statuses: [] });
    await pendingDialog;
    assert.equal(openedDialog, false, "a delayed lookup must not open an edit dialog after navigation");

    await flush();
    console.log(
        "Admin/report checks passed: request races, overview presets/filter persistence/layout, atomic report filters/exports, explicit errors and log details."
    );
})().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});

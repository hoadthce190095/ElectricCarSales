const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const assets = path.resolve(__dirname, "../../main/webapp/assets");
const files = ["tan-common.js", "tan-admin.js", "tan-report.js", "tan-catalogue.js", "tan-ai.js"];
let contextPath = "";
const requests = [];
const document = {
    baseURI: "http://localhost/",
    querySelector: (selector) => (selector === 'meta[name="app-context-path"]' ? { content: contextPath } : null)
};
const context = vm.createContext({
    console,
    URLSearchParams,
    document,
    fetch: async (url) => {
        requests.push(url);
        return { ok: true, json: async () => ({ models: [] }) };
    }
});
vm.runInContext(files.map((name) => fs.readFileSync(path.join(assets, name), "utf8")).join("\n"), context);
const read = (expression) => JSON.parse(vm.runInContext(`JSON.stringify(${expression})`, context));

assert.deepEqual(read("previousPeriod({from:'2026-10-01',to:'2026-10-10'})"), { from: "2026-09-21", to: "2026-09-30" });
assert.deepEqual(read("previousPeriod({from:'2024-02-01',to:'2024-02-29'})"), { from: "2024-01-03", to: "2024-01-31" });
assert.deepEqual(read("previousPeriod({from:'2024-03-01',to:'2024-03-31'})"), { from: "2024-01-30", to: "2024-02-29" });
assert.equal(read("previousPeriod({from:'0001-01-01',to:'0001-01-01'})"), null);
assert.equal(read("previousPeriod({from:'2026-10-05',to:'2026-10-04'})"), null);
assert.equal(read("metricChange(0,0)"), "Không đổi (0%)");
assert.equal(read("metricChange(50,0)"), "Kỳ trước bằng 0; chưa tính tỷ lệ");
assert.equal(read("metricChange(150,100)"), "Tăng 50%");
assert.equal(read("metricChange(50,100)"), "Giảm 50%");
assert.equal(read("searchText('Phạm Trần Phúc Tấn')"), "pham tran phuc tan");
assert.equal(read("businessDate('2026-10-05T09:00:00').toISOString()"), "2026-10-05T02:00:00.000Z");
assert.equal(
    read(
        "promotionPhase({status:'ACTIVE',startDate:'2026-10-05T08:00:00',endDate:'2026-10-05T10:00:00'},new Date('2026-10-05T00:30:00Z'))"
    ),
    "SCHEDULED"
);
assert.equal(
    read(
        "promotionPhase({status:'ACTIVE',startDate:'2026-10-05T08:00:00',endDate:'2026-10-05T10:00:00'},new Date('2026-10-05T02:00:00Z'))"
    ),
    "RUNNING"
);
assert.equal(
    read(
        "promotionPhase({status:'ACTIVE',startDate:'2026-10-05T08:00:00',endDate:'2026-10-05T10:00:00'},new Date('2026-10-05T03:00:01Z'))"
    ),
    "EXPIRED"
);
assert.equal(
    read(
        "promotionPhase({status:'INACTIVE',startDate:'2026-10-05T08:00:00',endDate:'2026-10-05T10:00:00'},new Date('2026-10-05T02:00:00Z'))"
    ),
    "INACTIVE"
);
assert.equal(read("canView('overview')"), false);
vm.runInContext("state.permissions=['VIEW_REPORTS'];state.user={username:'manager',role:'MANAGER'};", context);
assert.equal(read("canView('overview')"), true);
assert.equal(read("canView('users')"), false);

(async () => {
    // Cùng API/asset phải hoạt động từ welcome, trang JSP trực tiếp và trang JSP lồng thư mục.
    for (const [deployedContext, pagePath] of [
        ["", "/"],
        ["", "/tan.jsp"],
        ["/ElectricCarSales", "/ElectricCarSales/"],
        ["/ElectricCarSales", "/ElectricCarSales/tan.jsp"],
        ["/nhom-xe-dien", "/nhom-xe-dien/pages/catalogue.jsp"]
    ]) {
        contextPath = deployedContext;
        document.baseURI = `http://localhost${pagePath}`;
        requests.length = 0;
        await vm.runInContext("state.catalogueMetadata=null; api('auth')", context);
        await vm.runInContext("loadCatalogueMetadata()", context);
        assert.deepEqual(requests, [`${deployedContext}/api/auth`, `${deployedContext}/assets/vinfast-catalogue.json`]);
        assert.equal(
            read("appUrl('api/reports?' + query({from:'2026-10-01',format:'xlsx'}))"),
            `${deployedContext}/api/reports?from=2026-10-01&format=xlsx`
        );
        assert.equal(
            read("appUrl('/api/admin/logs?' + query({format:'csv',search:'Đăng nhập'}))"),
            `${deployedContext}/api/admin/logs?format=csv&search=%C4%90%C4%83ng+nh%E1%BA%ADp`
        );
    }

    const fallback = await vm.runInContext(
        `(async()=>{
    let calls=0;
    api=async path=>{
      calls++;
      if(calls===1){const error=new Error('Khoảng báo cáo tối đa 1 ngày');error.status=400;throw error;}
      const query=new URLSearchParams(path.slice(path.indexOf('?')+1));
      return {period:{from:query.get('from'),to:query.get('to')}};
    };
    const result=await readDefaultReport(lastSixMonths());
    return JSON.stringify({calls,period:result.data.period,warning:result.warning});
  })()`,
        context
    );
    const result = JSON.parse(fallback);
    assert.equal(result.calls, 2);
    assert.equal(result.period.from, result.period.to);
    assert.match(result.warning, /Đang hiển thị hôm nay/);
    console.log(
        "Frontend checks passed: periods, zero baseline, Vietnamese search, timezone/promotion phases, permissions, restricted default period and context-aware API/assets/exports."
    );
})().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});

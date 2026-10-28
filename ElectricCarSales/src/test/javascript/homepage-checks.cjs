const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const assets = path.resolve(__dirname, "../../main/webapp/assets");
const source = fs.readFileSync(path.join(assets, "js/home.js"), "utf8");
const catalogue = JSON.parse(fs.readFileSync(path.join(assets, "vinfast-catalogue.json"), "utf8"));

// Small DOM adapter: exercise the shipped script through the same controls as the browser.
class Element {
    constructor(tag = "div", value = "") {
        this.tag = tag;
        this.value = value;
        this.textContent = "";
        this.className = "";
        this.children = [];
        this.options = [];
        this.attributes = {};
        this.listeners = {};
        this.hidden = false;
        this.classList = {
            toggle: (name, enabled) => {
                const classes = new Set(this.className.split(/\s+/).filter(Boolean));
                if (enabled) classes.add(name);
                else classes.delete(name);
                this.className = [...classes].join(" ");
            },
            remove: (name) => this.classList.toggle(name, false)
        };
    }
    append(...children) {
        this.children.push(...children);
    }
    replaceChildren(...children) {
        this.children = children;
    }
    add(option) {
        this.options.push(option);
    }
    get selectedIndex() {
        return this.options.findIndex((option) => option.value === this.value);
    }
    setAttribute(name, value) {
        this.attributes[name] = value;
    }
    getAttribute(name) {
        return this.attributes[name];
    }
    addEventListener(name, listener) {
        (this.listeners[name] ??= []).push(listener);
    }
    emit(name) {
        const event = { target: this, preventDefault() {} };
        for (const listener of this.listeners[name] || []) listener(event);
    }
    showModal() {
        this.open = true;
    }
    close() {
        this.open = false;
    }
    querySelector(selector) {
        return (
            descendants(this).find((element) =>
                selector.startsWith(".")
                    ? element.className.split(/\s+/).includes(selector.slice(1))
                    : element.tag === selector
            ) || null
        );
    }
}

function descendants(element) {
    return element.children.flatMap((child) => [child, ...descendants(child)]);
}

async function load(contextPath, available = true) {
    const ids = [
        "vehicle-grid",
        "vehicle-search",
        "model-filter",
        "price-filter",
        "range-filter",
        "color-filter",
        "sort-filter",
        "result-summary",
        "active-filters",
        "empty-state",
        "compare-dock",
        "compare-dialog",
        "compare-table-wrap",
        "compare-count",
        "catalog-disclaimer",
        "search-form",
        "reset-filters",
        "empty-reset",
        "compare-open",
        "compare-clear",
        "compare-close",
        "primary-nav"
    ];
    const elements = Object.fromEntries(ids.map((id) => [id, new Element()]));
    elements["empty-state"].append(new Element("h3"), new Element("p"));
    elements["sort-filter"].value = "recommended";
    for (const id of ["model-filter", "price-filter", "range-filter", "color-filter"]) {
        elements[id].add({ value: "", text: "Tất cả" });
    }
    for (const [id, values] of [
        ["price-filter", ["500", "800", "1000", "1500"]],
        ["range-filter", ["300", "400", "500"]]
    ]) {
        for (const value of values) elements[id].add({ value, text: value });
    }
    const menu = new Element("button");
    const banners = [new Element("img"), new Element("img"), new Element("img")];
    const requests = [];
    const errors = [];
    const context = vm.createContext({
        URL,
        URLSearchParams,
        Intl,
        console: { error: (...args) => errors.push(args) },
        Option: function (text, value) {
            return { text, value };
        },
        document: {
            getElementById: (id) => elements[id],
            createElement: (tag) => new Element(tag),
            createTextNode: (text) => Object.assign(new Element("#text"), { textContent: text }),
            querySelector: (selector) => {
                if (selector === 'meta[name="app-context-path"]') return { content: contextPath };
                if (selector === ".mobile-menu-toggle") return menu;
                const banner = selector.match(/^\[data-banner="(\d)"\] img$/);
                return banner ? banners[Number(banner[1])] : null;
            }
        },
        fetch: async (url, options) => {
            requests.push({ url, options });
            return { ok: available, status: available ? 200 : 503, json: async () => catalogue };
        }
    });
    vm.runInContext(source, context, { filename: "home.js" });
    await new Promise(setImmediate);
    return { elements, requests, errors, banners, menu, setAvailable: (value) => (available = value) };
}

const vehicleNames = (elements) =>
    elements["vehicle-grid"].children.map((card) => card.querySelector("h3").textContent);
function change(elements, id, value) {
    elements[id].value = value;
    elements[id].emit(id === "vehicle-search" ? "input" : "change");
}

(async () => {
    for (const contextPath of ["", "/ElectricCarSales", "/showroom"]) {
        const { elements, requests, errors, banners } = await load(contextPath);
        assert.equal(requests.length, 1);
        assert.equal(requests[0].url, `${contextPath}/assets/vinfast-catalogue.json`);
        assert.equal(requests[0].options.credentials, "same-origin");
        assert.equal(errors.length, 0);
        assert.equal(
            elements["vehicle-grid"].children.length,
            10,
            "Blank numeric filters must show the complete catalogue"
        );
        assert.equal(elements["vehicle-grid"].getAttribute("aria-busy"), "false");
        assert.equal(elements["empty-state"].hidden, true);
        assert.ok(banners.every((banner) => banner.src?.startsWith("https://")));
    }

    const { elements, menu } = await load("/ElectricCarSales");
    change(elements, "vehicle-search", "VF 6");
    assert.deepEqual(vehicleNames(elements), ["VF 6 Eco", "VF 6 Plus"]);
    change(elements, "price-filter", "800");
    change(elements, "range-filter", "500");
    assert.equal(elements["vehicle-grid"].children.length, 0, "Search, price and range filters must combine");
    assert.equal(elements["empty-state"].hidden, false);
    elements["empty-reset"].emit("click");
    assert.equal(elements["vehicle-grid"].children.length, 10);

    change(elements, "model-filter", "VF 6");
    change(elements, "price-filter", "800");
    change(elements, "range-filter", "400");
    change(elements, "color-filter", "Đen (Jet Black)");
    assert.deepEqual(vehicleNames(elements), ["VF 6 Eco", "VF 6 Plus"]);
    change(elements, "sort-filter", "price-desc");
    assert.deepEqual(vehicleNames(elements), ["VF 6 Plus", "VF 6 Eco"]);
    elements["reset-filters"].emit("click");
    assert.equal(elements["active-filters"].children.length, 0);
    assert.equal(elements["vehicle-grid"].children.length, 10);

    for (const index of [0, 1, 2, 3]) {
        elements["vehicle-grid"].children[index].querySelector(".quick-compare").emit("click");
    }
    assert.equal(elements["compare-count"].textContent, "3");
    assert.match(elements["result-summary"].textContent, /tối đa 3/);
    assert.equal(elements["compare-dock"].hidden, false);
    elements["compare-open"].emit("click");
    assert.equal(elements["compare-dialog"].open, true);
    const table = elements["compare-table-wrap"].children[0];
    assert.equal(
        table.querySelector("thead").children[0].children.length,
        4,
        "Comparison must have one label and three models"
    );
    assert.equal(table.querySelector("tbody").children.length, 7);
    elements["compare-close"].emit("click");
    assert.equal(elements["compare-dialog"].open, false);
    elements["compare-clear"].emit("click");
    assert.equal(elements["compare-count"].textContent, "0");
    assert.equal(elements["compare-dock"].hidden, true);

    menu.emit("click");
    assert.equal(menu.getAttribute("aria-expanded"), "true");
    assert.equal(elements["primary-nav"].className, "open");
    menu.emit("click");
    assert.equal(menu.getAttribute("aria-expanded"), "false");

    const unavailable = await load("/showroom", false);
    assert.equal(unavailable.elements["vehicle-grid"].getAttribute("aria-busy"), "false");
    assert.equal(unavailable.elements["vehicle-grid"].hidden, true);
    assert.equal(unavailable.elements["empty-state"].hidden, false);
    assert.match(unavailable.elements["empty-state"].querySelector("h3").textContent, /Chưa thể tải/);
    assert.equal(unavailable.elements["empty-reset"].textContent, "Thử tải lại");
    assert.equal(unavailable.errors.length, 1);
    change(unavailable.elements, "vehicle-search", "VF 6");
    assert.match(unavailable.elements["result-summary"].textContent, /chưa sẵn sàng/);
    unavailable.setAvailable(true);
    unavailable.elements["empty-reset"].emit("click");
    assert.equal(unavailable.elements["vehicle-grid"].getAttribute("aria-busy"), "true");
    await new Promise(setImmediate);
    assert.equal(unavailable.requests.length, 2, "Retry must fetch the catalogue again after a 503 response");
    assert.equal(unavailable.elements["vehicle-grid"].getAttribute("aria-busy"), "false");
    assert.equal(unavailable.elements["vehicle-grid"].hidden, false);
    assert.equal(unavailable.elements["vehicle-grid"].children.length, 10);
    assert.equal(unavailable.elements["empty-state"].hidden, true);
    assert.equal(unavailable.elements["empty-reset"].textContent, "Xóa bộ lọc");
    change(unavailable.elements, "vehicle-search", "không có xe phù hợp");
    assert.match(unavailable.elements["empty-state"].querySelector("h3").textContent, /Chưa tìm thấy/);
    unavailable.elements["empty-reset"].emit("click");
    assert.equal(unavailable.elements["vehicle-grid"].children.length, 10);
    console.log(
        "Homepage checks passed: deployed contexts, catalogue, combined filters, sorting, comparison, mobile menu and load recovery."
    );
})().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

function element(properties = {}) {
    const listeners = new Map();
    return {
        ...properties,
        addEventListener(type, handler) {
            if (!listeners.has(type)) listeners.set(type, []);
            listeners.get(type).push(handler);
        },
        setAttribute(name, value) {
            this[name] = value;
        },
        dispatch(type) {
            const event = {
                currentTarget: this,
                defaultPrevented: false,
                preventDefault() {
                    this.defaultPrevented = true;
                }
            };
            for (const handler of listeners.get(type) || []) handler(event);
            return event;
        }
    };
}

const form = element({ reportValidity: () => true });
const message = element({ hidden: false, textContent: "Server validation message" });
const password = element({ type: "password" });
const toggle = element();
const nodes = new Map([
    ["#login-form", form],
    ["#form-message", message],
    ["#password", password],
    ["[data-password-toggle]", toggle],
    ["[data-forgot-password]", element()],
    ["[data-register]", element()]
]);
vm.runInNewContext(
    fs.readFileSync(path.resolve(__dirname, "../../main/webapp/assets/js/login.js"), "utf8"),
    { document: { querySelector: (selector) => nodes.get(selector) || null } },
    { filename: "login.js" }
);

assert.equal(form.dispatch("submit").defaultPrevented, false, "Valid login must reach the form's server action");
assert.equal(message.textContent, "Server validation message", "Client code must preserve the server response");
assert.equal(message.hidden, false);
toggle.dispatch("click");
assert.equal(password.type, "text");
toggle.dispatch("click");
assert.equal(password.type, "password");
assert.equal(form.dispatch("submit").defaultPrevented, false, "Toggling password visibility must preserve submission");
console.log("Login checks passed: native form submission, server messages and password visibility.");

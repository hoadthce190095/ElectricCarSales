"use strict";

const loginMessage = document.querySelector("#form-message");
const passwordInput = document.querySelector("#password");

document.querySelector("[data-password-toggle]")?.addEventListener("click", (event) => {
    const button = event.currentTarget;
    const showing = passwordInput.type === "text";
    passwordInput.type = showing ? "password" : "text";
    button.textContent = showing ? "Hiện" : "Ẩn";
    button.setAttribute("aria-label", showing ? "Hiện mật khẩu" : "Ẩn mật khẩu");
    button.setAttribute("aria-pressed", String(!showing));
});

document.querySelector("[data-forgot-password]")?.addEventListener("click", () => {
    window.location.href = "verify-otp?mode=forgot";
});

const setup = document.querySelector("#admin-setup");
if (setup) {
    const form = setup.querySelector("form");
    const message = setup.querySelector("[data-setup-message]");
    const submit = form.querySelector("button[type=submit]");
    const accept = { Accept: "application/json" };
    fetch(setup.dataset.url, { headers: accept })
        .then(response => response.ok ? response.json() : null)
        .then(result => { setup.hidden = !result?.available; })
        .catch(() => { setup.hidden = true; });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (submit.disabled) return;
        const input = Object.fromEntries(new FormData(form));
        if (input.password !== input.confirmPassword) {
            message.textContent = "Hai mật khẩu không trùng nhau.";
            return;
        }
        submit.disabled = true;
        try {
            const auth = await fetch(setup.dataset.authUrl, { headers: accept });
            const csrf = (await auth.json()).csrfToken;
            if (!auth.ok || !csrf) throw new Error("Không thể xác minh phiên thiết lập.");
            const response = await fetch(setup.dataset.url, {
                method: "POST",
                headers: { ...accept, "Content-Type": "application/json", "X-CSRF-Token": csrf },
                body: JSON.stringify(input)
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || "Không thể tạo tài khoản Admin.");
            form.reset();
            setup.hidden = true;
            loginMessage.textContent = result.message;
            loginMessage.classList.add("success");
            loginMessage.hidden = false;
            document.querySelector("#username").value = input.username;
            passwordInput.focus();
        } catch (error) {
            message.textContent = error.message;
        } finally {
            submit.disabled = false;
        }
    });
}

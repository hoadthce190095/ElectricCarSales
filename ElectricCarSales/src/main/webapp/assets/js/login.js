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

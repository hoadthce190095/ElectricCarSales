"use strict";

const passwordInput = document.querySelector("#password");
const passwordToggle = document.querySelector("[data-password-toggle]");
const forgotButton = document.querySelector("[data-forgot-password]");
const loginMessage = document.querySelector("#form-message");

if (passwordToggle && passwordInput) {
    passwordToggle.addEventListener("click", () => {
        const showing = passwordInput.type === "text";
        passwordInput.type = showing ? "password" : "text";
        passwordToggle.textContent = showing ? "Hiện" : "Ẩn";
        passwordToggle.setAttribute("aria-label", showing ? "Hiện mật khẩu" : "Ẩn mật khẩu");
        passwordToggle.setAttribute("aria-pressed", String(!showing));
    });
}

if (forgotButton && loginMessage) {
    forgotButton.addEventListener("click", () => {
        loginMessage.textContent = "Chức năng khôi phục mật khẩu sẽ được thực hiện qua xác thực OTP.";
        loginMessage.hidden = false;
    });
}

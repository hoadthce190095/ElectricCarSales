
"use strict";

const passwordInput = document.querySelector("#password");
const passwordToggle = document.querySelector("[data-password-toggle]");
const loginMessage = document.querySelector("#form-message");

if (passwordToggle && passwordInput) {
    passwordToggle.addEventListener("click", () => {
        const showing = passwordInput.type === "text";

        passwordInput.type = showing ? "password" : "text";

        passwordToggle.textContent = showing ? "Hiện" : "Ẩn";
        passwordToggle.setAttribute(
            "aria-pressed", String(!showing)
        );
    });
}

const forgotButton = document.querySelector("[data-forgot-password]");

if (forgotButton && loginMessage) {
    forgotButton.addEventListener("click", () => {
        loginMessage.textContent =
            "Vui lòng liên hệ quản trị viên để khôi phục mật khẩu.";
        loginMessage.hidden = false;
    });
}

"use strict";

const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#form-message");
const passwordInput = document.querySelector("#password");

document.querySelector("[data-password-toggle]").addEventListener("click", (event) => {
    const button = event.currentTarget;
    const showing = passwordInput.type === "text";
    passwordInput.type = showing ? "password" : "text";
    button.textContent = showing ? "Hiện" : "Ẩn";
    button.setAttribute("aria-label", showing ? "Hiện mật khẩu" : "Ẩn mật khẩu");
    button.setAttribute("aria-pressed", String(!showing));
});

document.querySelector("[data-forgot-password]").addEventListener("click", () => {
    loginMessage.textContent = "Vui lòng liên hệ hotro@electriccarsale.vn để được hỗ trợ khôi phục mật khẩu.";
    loginMessage.hidden = false;
});

document.querySelector("[data-register]").addEventListener("click", () => {
    loginMessage.textContent = "Đăng ký tài khoản sẽ được mở khi hệ thống kết nối chức năng xác thực.";
    loginMessage.hidden = false;
});

loginForm.addEventListener("submit", (event) => {
    event.preventDefault();
    if (!loginForm.reportValidity()) return;
    loginMessage.textContent = "Giao diện đã sẵn sàng. Cần kết nối Servlet/DAO để xác thực và chuyển đến trang theo vai trò.";
    loginMessage.hidden = false;
});

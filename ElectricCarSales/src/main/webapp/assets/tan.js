"use strict";

// Khởi động trang sau khi các file chức năng đã được tải.
async function refreshSetup() {
    try {
        const response = await fetch("./api/setup", {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });
        if (!response.ok) {
            return;
        }
        const setup = await response.json();
        $("#setup-button").hidden = !setup.available;
    } catch (_) {
        /* Thiết lập đầu tiên không bật trên tất cả môi trường. */
    }
}

function createFirstAdmin() {
    modal(
        "Tạo tài khoản Admin đầu tiên",
        [
            el("p", {
                class: "field full setup-introduction",
                text: "Bạn tự đặt mật khẩu trên máy. Sau khi tạo tài khoản, đăng nhập để xem báo cáo, quản lý tài khoản và khuyến mãi."
            }),
            field("username", "Tên đăng nhập", "text", "admin", {
                required: true,
                minlength: 3,
                maxlength: 50,
                full: true,
                autocomplete: "username"
            }),
            field("password", "Mật khẩu", "password", "", {
                required: true,
                minlength: 10,
                maxlength: 128,
                full: true,
                autocomplete: "new-password",
                help: "Từ 10 đến 128 ký tự. Nên kết hợp chữ, số và ký tự đặc biệt."
            }),
            field("confirmPassword", "Nhập lại mật khẩu", "password", "", {
                required: true,
                minlength: 10,
                maxlength: 128,
                full: true,
                autocomplete: "new-password"
            })
        ],
        async (input) => {
            if (input.password !== input.confirmPassword) {
                throw new Error("Hai lần nhập mật khẩu chưa giống nhau.");
            }
            await api("setup", { method: "POST", body: input });
            $("#setup-button").hidden = true;
            $("#dialog-fields").replaceChildren();
            toast("Đã tạo Admin. Dùng mật khẩu bạn vừa đặt để đăng nhập.");
            // Chờ dialog tạo tài khoản đóng trước khi mở form đăng nhập.
            setTimeout(login, 0);
        },
        "Tạo tài khoản Admin"
    );
}

$("#dialog-close").addEventListener("click", () => $("#form-dialog").close());
$("#dialog-cancel").addEventListener("click", () => $("#form-dialog").close());
$("#form-dialog").addEventListener("close", () => {
    for (const input of document.querySelectorAll("#dialog-fields input[type=password]")) {
        input.value = "";
    }
});
$("#form-dialog").addEventListener("close", () => {
    for (const input of $("#dialog-form").querySelectorAll("input[type=password]")) {
        input.value = "";
    }
});
$("#session-button").addEventListener("click", login);
$("#setup-button").addEventListener("click", createFirstAdmin);
for (const nav of document.querySelectorAll("[data-view]")) {
    nav.addEventListener("click", () => showView(nav.dataset.view));
}
$("#today").textContent = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
}).format(new Date());
(async () => {
    try {
        const result = await api("auth");
        state.user = result.user;
        state.permissions = result.permissions || [];
        state.csrf = result.csrfToken;
    } catch (error) {
        notice(error.message, true);
    }
    updateSession();
    await refreshSetup();
    await showView(canReport() ? "overview" : canView("users") ? "users" : "catalogue");
})();

"use strict";

// Khởi động trang sau khi các file chức năng đã được tải.
async function refreshSetup() {
    try {
        const response = await fetch(appUrl("api/setup"), {
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

function closeForm() {
    if ($("#form-dialog").dataset.pending !== "true") {
        $("#form-dialog").close();
    }
}

$("#dialog-close").addEventListener("click", closeForm);
$("#dialog-cancel").addEventListener("click", closeForm);
$("#form-dialog").addEventListener("cancel", (event) => {
    if ($("#form-dialog").dataset.pending === "true") {
        event.preventDefault();
    }
});
$("#form-dialog").addEventListener("close", () => {
    if (!$("#form-dialog").open) {
        modalSequence++;
        for (const input of $("#dialog-form").querySelectorAll("input[type=password]")) {
            input.value = "";
        }
    }
});

const mobileNavigation = window.matchMedia("(max-width: 760px)");
function closeNavigation(restoreFocus = false) {
    document.body.classList.remove("navigation-open");
    $("#navigation-toggle").setAttribute("aria-expanded", "false");
    $("#navigation-backdrop").hidden = true;
    $("#sidebar-panel").inert = mobileNavigation.matches;
    $(".main-shell").inert = false;
    $("#sidebar-panel").removeAttribute("role");
    $("#sidebar-panel").removeAttribute("aria-modal");
    if (restoreFocus) {
        $("#navigation-toggle").focus();
    }
}

$("#navigation-toggle").addEventListener("click", () => {
    document.body.classList.add("navigation-open");
    $("#navigation-toggle").setAttribute("aria-expanded", "true");
    $("#navigation-backdrop").hidden = false;
    $("#sidebar-panel").inert = false;
    $(".main-shell").inert = true;
    $("#sidebar-panel").setAttribute("role", "dialog");
    $("#sidebar-panel").setAttribute("aria-modal", "true");
    $("#navigation-close").focus();
});
$("#navigation-close").addEventListener("click", () => closeNavigation(true));
$("#navigation-backdrop").addEventListener("click", () => closeNavigation(true));
document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && document.body.classList.contains("navigation-open")) {
        closeNavigation(true);
    }
});
mobileNavigation.addEventListener("change", () => closeNavigation());
closeNavigation();

$("#refresh-view").addEventListener("click", () => showView(state.view, { history: false }));
$("#session-button").addEventListener("click", login);
$("#setup-button").addEventListener("click", createFirstAdmin);
for (const nav of document.querySelectorAll("[data-view]")) {
    nav.addEventListener("click", () => showView(nav.dataset.view, { focus: true }));
}
$(".skip-link").addEventListener("click", (event) => {
    event.preventDefault();
    $("#main").focus();
});
function restoreView() {
    const requested = viewFromLocation();
    if (!requested && window.location.hash) {
        return;
    }
    const name = requested || defaultView();
    if (name !== state.view) {
        showView(name, { history: false, focus: true });
    }
}
window.addEventListener("popstate", restoreView);
window.addEventListener("hashchange", restoreView);
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
    await showView(viewFromLocation() || defaultView());
    const entryUrl = new URL(window.location.href);
    if (entryUrl.searchParams.get("login") === "1") {
        entryUrl.searchParams.delete("login");
        window.history.replaceState(window.history.state, "", entryUrl);
        if (!state.user && !$("#form-dialog").open) {
            login();
        }
    }
})();

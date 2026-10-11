"use strict";

(() => {
    const toast = document.querySelector("[data-toast]");
    let toastTimer;

    function notify(message) {
        if (!toast) return;
        toast.textContent = message;
        toast.classList.add("visible");
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 3200);
    }

    const menuToggle = document.querySelector("[data-menu-toggle]");
    const sidebar = document.querySelector("#customer-sidebar");
    menuToggle?.addEventListener("click", () => {
        const open = sidebar.classList.toggle("open");
        menuToggle.setAttribute("aria-expanded", String(open));
        menuToggle.setAttribute("aria-label", open ? "Đóng menu" : "Mở menu");
    });

    document.querySelector("[data-notification]")?.addEventListener("click", () => {
        notify("Thông báo sẽ xuất hiện tại đây khi có cập nhật mới.");
    });

    document.querySelector("[data-profile-edit]")?.addEventListener("click", (event) => {
        event.preventDefault();
        notify("Hồ sơ sẽ cho phép cập nhật khi kết nối dịch vụ tài khoản.");
    });

    document.querySelector("[data-document-filter]")?.addEventListener("click", () => {
        notify("Hiện chưa có tài liệu để lọc.");
    });

    document.querySelectorAll(".filter-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".filter-tab").forEach((item) => {
                item.classList.toggle("active", item === tab);
                item.setAttribute("aria-selected", String(item === tab));
            });
        });
    });

    document.querySelector("[data-consultation-form]")?.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!event.currentTarget.reportValidity()) return;
        notify("Giao diện đã nhận thông tin. Cần kết nối API tư vấn để gửi yêu cầu.");
        event.currentTarget.reset();
    });

    const aiForm = document.querySelector("[data-ai-form]");
    const aiQuestion = aiForm?.querySelector("textarea");
    aiForm?.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!aiForm.reportValidity()) return;
        notify("Giao diện đã sẵn sàng. Cần kết nối dịch vụ AI để nhận câu trả lời.");
    });
    document.querySelectorAll("[data-ai-prompt]").forEach((prompt) => {
        prompt.addEventListener("click", () => {
            if (aiQuestion) {
                aiQuestion.value = prompt.dataset.aiPrompt || "";
                aiQuestion.focus();
            }
        });
    });

    document.querySelectorAll("[data-cart-count]").forEach((count) => {
        count.textContent = "0";
    });
})();

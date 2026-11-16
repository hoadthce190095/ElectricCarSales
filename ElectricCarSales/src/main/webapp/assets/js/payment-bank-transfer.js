(() => {
    'use strict';
    const panel = document.querySelector('[data-payment-id][data-check-url]');
    const status = document.getElementById('bank-transfer-state');
    if (!panel || !status) return;
    document.querySelectorAll('[data-copy]').forEach(button => {
        button.addEventListener('click', async () => {
            const value = document.getElementById(button.dataset.copy)?.textContent?.trim();
            if (!value) return;
            try {
                await navigator.clipboard.writeText(value);
                button.textContent = 'Đã sao chép';
                window.setTimeout(() => { button.textContent = 'Sao chép'; }, 1600);
            } catch (_) {
                status.textContent = 'Trình duyệt không cho phép sao chép tự động. Hãy chọn và sao chép thông tin thủ công.';
            }
        });
    });
    async function check() {
        if (document.hidden) return;
        try {
            const response = await fetch(panel.dataset.checkUrl, {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: { 'Accept': 'application/json' }
            });
            const contentType = response.headers.get('content-type') || '';
            const body = await response.text();
            if (!contentType.toLowerCase().includes('application/json')) {
                if (response.status === 401 || (response.redirected && /\/login(?:[?#]|$)/i.test(response.url))) {
                    status.textContent = 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại để kiểm tra trạng thái.';
                } else if (response.status >= 400) {
                    status.textContent = `Chưa kiểm tra được trạng thái (HTTP ${response.status}). Tiền vẫn ở trạng thái chờ đối chiếu.`;
                }
                return;
            }
            let payment;
            try { payment = body ? JSON.parse(body) : {}; }
            catch (_) {
                status.textContent = 'Máy chủ trả dữ liệu không hợp lệ. Tiền vẫn ở trạng thái chờ đối chiếu.';
                return;
            }
            if (!response.ok) {
                status.textContent = payment.error || 'Chưa kiểm tra được trạng thái. Tiền vẫn ở trạng thái chờ đối chiếu.';
                return;
            }
            if (payment.status === 'COMPLETED') {
                status.textContent = 'Thanh toán thành công.';
                status.classList.add('is-complete');
                window.location.assign(panel.dataset.successUrl);
            } else if (payment.status === 'CANCELLED' || payment.status === 'FAILD') {
                status.textContent = 'Giao dịch đã bị từ chối/hủy. Kiểm tra lại với kế toán.';
            } else if (payment.bankReported) {
                status.textContent = 'Đã nhận thông tin giao dịch, chờ kế toán xác nhận.';
                status.classList.add('is-reported');
            } else {
                status.textContent = 'Chờ chuyển khoản hoặc ngân hàng ghi nhận giao dịch.';
                status.classList.remove('is-reported', 'is-complete');
            }
        } catch (error) {
            // A network interruption must never be treated as payment success.
        }
    }
    check();
    setInterval(check, 10000);
})();

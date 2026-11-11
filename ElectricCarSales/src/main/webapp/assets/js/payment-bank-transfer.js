(() => {
    'use strict';
    const panel = document.querySelector('[data-payment-id][data-check-url]');
    const status = document.getElementById('bank-transfer-state');
    if (!panel || !status) return;
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
                status.textContent = 'Kế toán đã xác nhận tiền thực nhận. Đang mở kết quả...';
                window.location.assign(panel.dataset.successUrl);
            } else if (payment.status === 'CANCELLED' || payment.status === 'FAILD') {
                status.textContent = 'Giao dịch đã bị từ chối/hủy. Kiểm tra lại với kế toán.';
            }
        } catch (error) {
            // A network interruption must never be treated as payment success.
        }
    }
    check();
    setInterval(check, 10000);
})();

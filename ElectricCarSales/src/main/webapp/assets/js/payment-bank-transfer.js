(() => {
    'use strict';
    const panel = document.querySelector('[data-payment-id][data-check-url]');
    const status = document.getElementById('bank-transfer-state');
    if (!panel || !status) return;

    const recheckBtn = document.getElementById('btn-recheck-transfer');
    if (recheckBtn) {
        recheckBtn.addEventListener('click', async () => {
            recheckBtn.disabled = true;
            recheckBtn.textContent = '⏳ Đang kiểm tra giao dịch...';
            await check();
            setTimeout(() => {
                recheckBtn.disabled = false;
                recheckBtn.textContent = '⚡ Tôi đã chuyển khoản (Kiểm tra giao dịch ngay)';
            }, 2000);
        });
    }

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

    let completed = false;

    async function check() {
        if (completed || document.hidden) return;
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
                    status.textContent = `Chưa kiểm tra được trạng thái (HTTP ${response.status}).`;
                }
                return;
            }
            let payment;
            try { payment = body ? JSON.parse(body) : {}; }
            catch (_) {
                status.textContent = 'Máy chủ trả dữ liệu không hợp lệ.';
                return;
            }
            if (!response.ok) {
                status.textContent = payment.error || 'Chưa kiểm tra được trạng thái.';
                return;
            }
            if (payment.status === 'COMPLETED' || payment.bankReported) {
                completed = true;
                status.textContent = '✓ Giao dịch đã thành công! Đang chuyển hướng...';
                status.classList.remove('is-reported');
                status.classList.add('is-complete');
                setTimeout(() => {
                    window.location.assign(panel.dataset.successUrl);
                }, 800);
            } else if (payment.status === 'CANCELLED' || payment.status === 'FAILD') {
                status.textContent = 'Giao dịch đã bị từ chối/hủy. Kiểm tra lại với bộ phận hỗ trợ.';
            } else {
                status.textContent = 'Chờ chuyển khoản hoặc ngân hàng ghi nhận giao dịch.';
                status.classList.remove('is-reported', 'is-complete');
            }
        } catch (error) {
            // A network interruption must never be treated as payment success.
        }
    }
    check();
    setInterval(check, 3000);
})();

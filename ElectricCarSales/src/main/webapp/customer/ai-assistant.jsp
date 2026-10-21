<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "AI tư vấn xe"); request.setAttribute("customerEyebrow", "TRỢ LÝ XE ĐIỆN VINFAST"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="ai-layout">
    <section class="panel ai-chat-panel">
        <div class="ai-chat-header">
            <div class="ai-avatar">✳</div>
            <div>
                <strong>Trợ lý VinFast</strong>
                <span>
                    <i></i> Hỗ trợ tìm hiểu sản phẩm</span>
            </div>
            <span class="ai-header-tag">AI ASSISTANT</span>
        </div>
        <div class="ai-chat-body" data-ai-messages aria-live="polite">
            <div class="ai-message assistant-message">
                <span class="message-avatar">✳</span>
                <div>
                    <p>Xin chào! Mình có thể giúp bạn tìm hiểu thông tin xe điện, so sánh các mẫu xe và gợi ý lựa chọn theo nhu cầu.</p>
                    <small>Trợ lý VinFast · Hỗ trợ sản phẩm</small>
                </div>
            </div>
        </div>
        <div class="ai-chat-compose">
            <div class="ai-prompts">
                <span>Gợi ý:</span>
                <button type="button" data-ai-prompt="So sánh VF 5 và VF 6">So sánh VF 5 và VF 6</button>
                <button type="button" data-ai-prompt="Tư vấn xe phù hợp với nhu cầu của tôi">Tư vấn theo nhu cầu</button>
                <button type="button" data-ai-prompt="Tôi muốn tìm hiểu về tầm hoạt động của xe">Tầm hoạt động của xe</button>
            </div>
            <form data-ai-form>
                <textarea name="question" rows="2" maxlength="1000" placeholder="Hỏi về mẫu xe, thông số hoặc nhu cầu sử dụng…" aria-label="Câu hỏi cho trợ lý xe" required></textarea>
                <button class="button button-primary" type="submit" aria-label="Gửi câu hỏi">Gửi <span>↑</span>
                </button>
            </form>
            <p>Trợ lý chỉ tư vấn sản phẩm, không truy cập thông tin tài chính hoặc giao dịch cá nhân.</p>
        </div>
    </section>
    <aside class="ai-aside">
        <section class="panel ai-capabilities">
            <span class="section-overline">BẠN CÓ THỂ HỎI</span>
            <h2>Khám phá xe theo cách của bạn</h2>
            <div class="capability">
                <span>⌕</span>
                <div>
                    <strong>Tìm hiểu sản phẩm</strong>
                    <p>Hỏi về thông tin và đặc điểm xe điện.</p>
                </div>
            </div>
            <div class="capability">
                <span>⇄</span>
                <div>
                    <strong>So sánh mẫu xe</strong>
                    <p>Đối chiếu các thông số có trong danh mục.</p>
                </div>
            </div>
            <div class="capability">
                <span>✧</span>
                <div>
                    <strong>Nhận gợi ý phù hợp</strong>
                    <p>Chia sẻ ngân sách, quãng đường và nhu cầu sạc.</p>
                </div>
            </div>
        </section>
        <div class="ai-safety">
            <span>i</span>
            <p>AI hỗ trợ tham khảo thông tin sản phẩm. Giá, tình trạng xe và điều kiện giao dịch cần được nhân viên xác nhận.</p>
        </div>
    </aside>
</div>
<%@ include file="../common/customer-shell-end.jspf" %>

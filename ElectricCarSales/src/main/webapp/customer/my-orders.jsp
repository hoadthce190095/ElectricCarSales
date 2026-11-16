<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("customerTitle", "Đơn hàng của tôi"); request.setAttribute("customerEyebrow", "GIAO DỊCH CỦA BẠN"); %>
<%@ include file="../common/customer-shell.jspf" %>
<div class="page-intro split-intro">
    <p>Theo dõi đơn hàng và tiến độ giao dịch thuộc tài khoản của bạn.</p>
    <span class="soft-badge">
        <i></i> Dữ liệu cá nhân</span>
</div>
<section class="panel table-panel">
    <div class="panel-heading">
        <div>
            <span class="section-overline">LỊCH SỬ GIAO DỊCH</span>
            <h2>Tất cả đơn hàng</h2>
        </div>
        <label class="search-field">
            <span>⌕</span>
            <input type="search" placeholder="Tìm mã đơn hàng" aria-label="Tìm mã đơn hàng">
        </label>
    </div>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>MÃ GIAO DỊCH</th>
                    <th>NGÀY TẠO</th>
                    <th>MẪU XE</th>
                    <th>TỔNG GIÁ TRỊ</th>
                    <th>TRẠNG THÁI</th>
                    <th>
                    </th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="order" items="${orders}">
                    <tr>
                        <td>#<c:out value="${order.id}"/></td>
                        <td><c:out value="${order.orderDate}"/></td>
                        <td><c:out value="${order.vehicleName}" default="—"/></td>
                        <td><c:out value="${order.originalAmount}"/> VND</td>
                        <td><c:out value="${order.status}"/></td>
                        <td>
                            <a href="<%= request.getContextPath() %>/payment/detail?orderId=<c:out value='${order.id}'/>">Công nợ</a>
                            <c:if test="${order.status eq 'DEPOSITED' or order.status eq 'PROCESSING' or order.status eq 'PAID' or order.status eq 'COMPLETED'}">
                                | <a href="<%= request.getContextPath() %>/customer/sales-document?kind=deposit&amp;orderId=<c:out value='${order.id}'/>">Biên nhận cọc</a>
                            </c:if>
                            <c:if test="${order.contractStatus eq 'SIGNED'}">
                                | <a href="<%= request.getContextPath() %>/customer/sales-document?kind=contract&amp;orderId=<c:out value='${order.id}'/>">Hợp đồng</a>
                            </c:if>
                            <c:if test="${order.status eq 'PAID' or order.status eq 'COMPLETED'}">
                                | <a href="<%= request.getContextPath() %>/customer/sales-document?kind=invoice&amp;orderId=<c:out value='${order.id}'/>">Hóa đơn</a>
                            </c:if>
                            <c:if test="${order.status eq 'PENDING' or order.status eq 'RESERVED'}">
                                <form method="post" action="<%= request.getContextPath() %>/payment/create">
                                    <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">
                                    <input type="hidden" name="orderId" value="<c:out value='${order.id}'/>">
                                    <input type="hidden" name="type" value="DEPOSIT">
                                    <button type="submit" name="method" value="BANK_TRANSFER" <c:if test="${not sepayReady}">title="Cổng thanh toán VietQR MB Bank chưa sẵn sàng"</c:if>>Thanh toán cọc 30% qua VietQR (MB Bank)<c:if test="${not sepayReady}"> (!)</c:if></button>
                                    <button type="submit" name="method" value="CASH">Yêu cầu thanh toán tiền mặt</button>
                                </form>
                            </c:if>
                            <c:if test="${order.paymentMethodName eq 'INSTALLMENT' and order.contractStatus eq 'SIGNED' and order.remainingAmount gt 0}">
                                <span>Chờ đối tác tài chính giải ngân 70%.</span>
                            </c:if>
                            <c:if test="${(order.status eq 'DEPOSITED' or order.status eq 'PROCESSING') and order.contractStatus eq 'SIGNED' and order.paymentMethodName ne 'INSTALLMENT' and order.remainingAmount gt 0}">
                                <form method="post" action="<%= request.getContextPath() %>/payment/create">
                                    <input type="hidden" name="csrfToken" value="<c:out value='${paymentCsrf}'/>">
                                    <input type="hidden" name="orderId" value="<c:out value='${order.id}'/>">
                                    <input type="hidden" name="type" value="REMAINING">
                                    <button type="submit" name="method" value="BANK_TRANSFER" <c:if test="${not sepayReady}">title="Cổng thanh toán VietQR MB Bank chưa sẵn sàng"</c:if>>Thanh toán 70% còn lại qua VietQR (MB Bank)<c:if test="${not sepayReady}"> (!)</c:if></button>
                                    <button type="submit" name="method" value="CASH">Yêu cầu thanh toán tiền mặt</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty orders}">
                    <tr><td colspan="6"><div class="table-empty"><strong>Chưa có giao dịch</strong>
                        <p>Đơn hàng được Sales Staff lập sẽ xuất hiện tại đây.</p></div></td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
    <div class="table-foot">
        <span>Đang hiển thị <strong><c:out value="${orders.size()}"/></strong> giao dịch</span>
        <div class="pagination">
            <button disabled aria-label="Trang trước">←</button>
            <span>1</span>
            <button disabled aria-label="Trang tiếp">→</button>
        </div>
    </div>
</section>
<%@ include file="../common/customer-shell-end.jspf" %>

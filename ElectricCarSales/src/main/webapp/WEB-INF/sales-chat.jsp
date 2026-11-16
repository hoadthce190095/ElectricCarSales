<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Trò chuyện tư vấn</title>
<style>body{font:16px system-ui;background:#f4f7fb;color:#18324a;margin:0}main{max-width:850px;margin:32px auto;padding:20px}article{background:white;padding:24px;margin:20px 0;border-radius:16px}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:inherit;line-height:1.7;background:#f3f6fa;padding:16px}textarea{box-sizing:border-box;width:100%;padding:12px}button{padding:10px 24px;margin-top:12px;background:#146cbd;color:white;border:0;border-radius:8px}.error{color:#a32222}</style></head><body><main>
<a href="${pageContext.request.contextPath}/${isCustomer ? 'consultation' : 'staff/dashboard'}">← Quay lại</a>
<h1>Trò chuyện tư vấn</h1><p>Trao đổi với nhân viên về mẫu xe và phương án thanh toán. Tải lại trang để xem phản hồi mới.</p>
<p class="error" role="alert"><c:out value="${chatError}"/></p>
<c:if test="${empty threads}"><p>Chưa có cuộc trò chuyện. Hãy gửi yêu cầu tư vấn để bắt đầu.</p></c:if>
<c:forEach var="thread" items="${threads}"><article id="thread-${thread.id}">
<h2>#${thread.id} · <c:out value="${thread.modelName}"/></h2>
<p><c:out value="${thread.customerName}"/> · <c:out value="${thread.staffName}"/> · <c:out value="${thread.status}"/></p>
<pre><c:out value="${thread.content}"/></pre>
<c:choose><c:when test="${thread.status eq 'COMPLETED' or thread.status eq 'CANCELLED'}"><p>Cuộc trò chuyện đã kết thúc.</p></c:when><c:otherwise>
<form method="post" action="${pageContext.request.contextPath}/sales-chat">
<input type="hidden" name="csrfToken" value="<c:out value='${chatCsrf}'/>"><input type="hidden" name="requestId" value="${thread.id}">
<label for="message-${thread.id}">Tin nhắn</label><textarea id="message-${thread.id}" name="message" maxlength="800" rows="3" required></textarea>
<p>Lịch sử mỗi yêu cầu có giới hạn 1.000 ký tự. Khi đầy, hãy mở yêu cầu tư vấn mới.</p><button type="submit">Gửi tin nhắn</button></form>
</c:otherwise></c:choose></article></c:forEach></main></body></html>

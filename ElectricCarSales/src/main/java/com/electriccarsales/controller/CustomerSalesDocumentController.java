package com.electriccarsales.controller;

import com.electriccarsales.model.SalesPreviewKind;
import com.electriccarsales.model.SessionUser;
import com.electriccarsales.service.SalesWordService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.*;

@WebServlet({"/customer/sales-document","/staff/sales-document"})
public final class CustomerSalesDocumentController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        var session=req.getSession(false);
        if(session==null || !(session.getAttribute("user") instanceof SessionUser user)) {resp.sendError(403);return;}
        // Chấp nhận role nhân viên nếu gọi từ /staff/ hoặc người dùng đang đăng nhập là nhân viên
        String userRole = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        boolean isStaffRole = java.util.List.of("ACCOUNTANT","SALES","MANAGER","ADMIN","OPERATIONS").contains(userRole);
        boolean isStaff = req.getServletPath().startsWith("/staff/") || isStaffRole;
        if (isStaff) {
            if (!isStaffRole) {
                resp.sendError(403); return;
            }
        } else {
            if (!"CUSTOMER".equalsIgnoreCase(userRole)) { resp.sendError(403); return; }
        }
        Integer orderId=FinanceControllerSupport.positiveInt(req.getParameter("orderId"));
        SalesPreviewKind kind;
        try {kind=SalesPreviewKind.valueOf(req.getParameter("kind").toUpperCase(Locale.ROOT));}
        catch(RuntimeException ex){resp.sendError(400);return;}
        if(orderId==null){resp.sendError(400);return;}
        byte[] bytes;
        try {
            int ownerUserId=user.getUserId();
            if(isStaff) {
                try(var db=com.electriccarsales.dao.FinanceConnection.open();
                    var query=db.prepareStatement("SELECT c.UserID FROM Orders o JOIN Customers c ON c.CustomerID=o.CustomerID WHERE o.OrderID=?")) {
                    query.setInt(1,orderId);
                    try(var row=query.executeQuery()) {if(!row.next()){resp.sendError(404);return;}ownerUserId=row.getInt(1);}
                }
            }
            bytes=new SalesWordService().render(kind,orderId,ownerUserId);
        }
        catch(Exception ex){log("Cannot render sales document",ex);resp.sendError(503,"Không thể tạo chứng từ.");return;}
        if(bytes==null){
            if("html".equals(req.getParameter("view"))) {
                resp.setContentType("text/html;charset=UTF-8");
                var out=resp.getWriter();
                String label = kind == SalesPreviewKind.CONTRACT ? "Hợp đồng mua bán" : (kind == SalesPreviewKind.INVOICE ? "Hóa đơn giá trị gia tăng" : "Biên nhận đặt cọc");
                String reason = kind == SalesPreviewKind.INVOICE ? "Hóa đơn chưa được Kế toán phát hành hoặc đơn hàng chưa hoàn tất 100% thanh toán và ký hợp đồng."
                        : (kind == SalesPreviewKind.CONTRACT ? "Hợp đồng chưa được tạo hoặc đơn hàng chưa hoàn tất đặt cọc 30%."
                        : "Chưa có dữ liệu biên nhận thanh toán đặt cọc 30% hợp lệ.");
                out.print("<!doctype html><html lang='vi'><head><meta charset='utf-8'><title>Chứng từ chưa sẵn sàng</title><style>body{font-family:'Segoe UI',sans-serif;background:#f8fafc;padding:40px;text-align:center}.box{background:#fff;max-width:540px;margin:40px auto;padding:32px;border:1px solid #e2e8f0;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,0.05)}h2{color:#0f172a;margin-top:0}p{color:#64748b;line-height:1.6}.btn{display:inline-block;padding:10px 20px;background:#1456db;color:#fff;text-decoration:none;border-radius:6px;font-weight:600;margin-top:16px}</style></head><body><div class='box'><h2>" + escape(label) + " chưa sẵn sàng</h2><p>" + escape(reason) + "</p><a class='btn' href='javascript:history.back()'>← Quay lại</a></div></body></html>");
                return;
            }
            resp.sendError(404, "Chứng từ chưa sẵn sàng hoặc chưa được phát hành.");
            return;
        }
        resp.setHeader("Cache-Control","private, no-store");
        resp.setHeader("X-Content-Type-Options","nosniff");
        if("html".equals(req.getParameter("view"))) {
            resp.setContentType("text/html;charset=UTF-8");
            var out=resp.getWriter();
            String title = kind == SalesPreviewKind.CONTRACT ? "Hợp đồng mua bán xe điện VinFast"
                    : (kind == SalesPreviewKind.INVOICE ? "Hóa đơn giá trị gia tăng" : "Biên nhận đặt cọc");
            out.print("<!doctype html><html lang='vi'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>" + escape(title) + "</title><style>body{font-family:'Segoe UI',system-ui,sans-serif;max-width:960px;margin:32px auto;padding:32px;background:#f8fafc;color:#0f172a;line-height:1.7}.doc-box{background:#fff;border:1px solid #cbd5e1;border-radius:12px;padding:40px;box-shadow:0 4px 12px rgba(0,0,0,0.05)}table{border-collapse:collapse;width:100%;margin:16px 0}td,th{border:1px solid #cbd5e1;padding:10px 12px}th{background:#f1f5f9;font-weight:600}.top-bar{display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;padding-bottom:16px;border-bottom:2px solid #e2e8f0}.btn{display:inline-flex;align-items:center;gap:6px;padding:8px 16px;border-radius:6px;text-decoration:none;font-weight:600;font-size:14px;border:none;cursor:pointer}.btn-primary{background:#1456db;color:#fff}.btn-secondary{background:#e2e8f0;color:#1e293b;margin-left:8px}@media print{.top-bar,.bottom-bar{display:none !important}body{background:#fff;padding:0;margin:0}.doc-box{border:none;box-shadow:none;padding:0}}</style></head><body>");
            out.print("<div class='doc-box'><div class='top-bar'><div><h1 style='margin:0;font-size:22px;color:#0f172a;'>" + escape(title) + "</h1><small style='color:#64748b;'>Đơn hàng #" + orderId + " · Xem trước chứng từ điện tử</small></div><div><a class='btn btn-primary' href='" + req.getContextPath() + (isStaff ? "/staff/sales-document" : "/customer/sales-document") + "?kind=" + kind.name().toLowerCase(Locale.ROOT) + "&amp;orderId=" + orderId + "'>📥 Tải file Word (.docx)</a><a class='btn btn-secondary' href='javascript:history.back()'>← Quay lại</a></div></div>");
            try(var doc=new XWPFDocument(new ByteArrayInputStream(bytes))) {writeBody(doc,out);}
            out.print("<div class='bottom-bar' style='margin-top:32px;padding-top:16px;border-top:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;'><small style='color:#64748b;'>Hệ thống quản lý bán xe điện VinFast (SWP391) · Bản xem trước đồng bộ từ mẫu DOCX</small><div><a class='btn btn-primary' href='" + req.getContextPath() + (isStaff ? "/staff/sales-document" : "/customer/sales-document") + "?kind=" + kind.name().toLowerCase(Locale.ROOT) + "&amp;orderId=" + orderId + "'>📥 Tải file Word (.docx)</a></div></div></div></body></html>");
            return;
        }
        resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resp.setHeader("Content-Disposition","attachment; filename="+kind.name().toLowerCase(Locale.ROOT)+"-"+orderId+".docx");
        resp.setContentLength(bytes.length);resp.getOutputStream().write(bytes);
    }
    private static void writeBody(IBody body,PrintWriter out) {
        for(var item:body.getBodyElements()) {
            if(item instanceof XWPFParagraph p) out.print("<p>"+escape(p.getText())+"</p>");
            else if(item instanceof XWPFTable table) {
                out.print("<table>");for(var row:table.getRows()) {
                    out.print("<tr>");for(var cell:row.getTableCells()) {out.print("<td>");writeBody(cell,out);out.print("</td>");}out.print("</tr>");
                }out.print("</table>");
            }
        }
    }
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}

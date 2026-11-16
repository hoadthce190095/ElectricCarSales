package com.electriccarsales.service;

import com.electriccarsales.dao.SalesPreviewDAO;
import com.electriccarsales.model.SalesPreviewKind;
import java.io.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

/** Fills copies of the supplied SWP391 demonstration templates. No document or schema writes. */
public final class SalesWordService {
    private static final String MISSING = "Chưa cung cấp";
    private static final Pattern FIELD = Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");

    public byte[] render(SalesPreviewKind kind, int orderId, int userId) throws Exception {
        var fields = new SalesPreviewDAO().find(kind, orderId, userId, true);
        if (fields.isEmpty()) return null;
        return renderFields(kind, fields);
    }

    public byte[] renderFields(SalesPreviewKind kind, Map<String,String> source) throws IOException {
        var f = new HashMap<>(source);
        var bankEvidence = SePayEvidence.fromNote(f.get("payment.audit"));
        if (bankEvidence != null) f.put("payment.depositReference", bankEvidence.bankReference());
        // Demo identity supplied explicitly by the project owner on 2026-11-16.
        f.put("seller.companyName", "ElectricCarSales");
        f.put("seller.taxCode", "096205004458");
        f.put("seller.address", "Đường Hai Bà Trưng, Quận Ninh Kiều, TP. Cần Thơ");
        f.put("seller.representativeName", "Đỗ Tấn Đạt");
        f.put("seller.representativeTitle", "Quản lý");
        f.put("seller.bankName", "MB Bank");
        f.put("seller.bankAccount", "0917231548 (Do Tan Dat)");
        f.put("signature.customerSignature", "SIGNED".equals(f.get("contract.status")) ? "Đã xác thực OTP" : "Chưa ký");
        f.putIfAbsent("signature.signedAt", "Chưa ký");
        f.putIfAbsent("signature.verificationReference", "Chưa ký");
        f.put("signature.documentHash", "Chưa lưu mã băm tài liệu");
        f.put("signature.sellerSignature", "Chưa cung cấp chữ ký");
        f.put("order.totalAmountWords", VietnameseDong.words(f.get("order.totalAmount")));
        for (String key : List.of("order.totalAmount","order.depositAmount","order.remainingInitial",
                "order.salesPrice","payment.currentOutstanding","payment.amount","invoice.totalAmount",
                "invoice.subTotal","invoice.taxAmount","invoice.discountAmount")) {
            if (f.get(key) != null) f.put(key, money(f.get(key)));
        }
        var literals = new LinkedHashMap<String,String>();
        if (kind != SalesPreviewKind.CONTRACT) {
            literals.put("Electric Car Sales (DEMO)", f.get("seller.companyName"));
            literals.put("Chưa khai báo (mẫu)", f.get("seller.taxCode"));
            literals.put("[Số điện thoại công ty]", MISSING);
            literals.put("Nguyễn Văn A (DEMO)", value(f,"customer.name"));
            literals.put("[Số điện thoại khách]", value(f,"customer.phone"));
            literals.put("[Email khách hàng]", value(f,"customer.email"));
            literals.put("EV-01 Premium (DEMO)", value(f,"car.model")+" "+value(f,"car.version"));
            literals.put("Trắng; năm sản xuất: 2026", value(f,"car.color")+"; năm sản xuất: "+value(f,"car.year"));
            literals.put("[VIN của xe thuộc OrderDetails]", value(f,"car.vin"));
            literals.put("[EngineNumber của xe]", value(f,"car.engineNumber"));
            literals.put("ORDER-DEMO-0001", value(f,"order.id"));
            literals.put("HDMB-DEMO-0001", value(f,"contract.number"));
            literals.put("BC-DEMO-0001", "BC-"+value(f,"payment.id"));
            literals.put("HD-DEMO-0001", value(f,"invoice.number"));
            literals.put("800.000.000", value(f,"order.totalAmount"));
            literals.put("240.000.000", value(f,"order.depositAmount"));
            literals.put("560.000.000", value(f,"order.remainingInitial"));
            literals.put("Hai trăm bốn mươi triệu đồng", VietnameseDong.words(source.get("order.depositAmount")));
            literals.put("Tám trăm triệu đồng", VietnameseDong.words(source.get("order.totalAmount")));
            literals.put("DEMO-REF-COC-001", value(f,"payment.depositReference"));
            literals.put("DEMO-REF-70-001", value(f,"payment.remainingReference"));
            literals.put("10/11/2026", date(f.get("payment.depositConfirmedDate")));
            literals.put("15/11/2026", date(f.get("invoice.date")));
            literals.put("Chuyển khoản ngân hàng", value(f,"payment.channel"));
            literals.put("Trần Thị B (DEMO)", "Chưa lưu tên người xác nhận");
            literals.put("Chưa lập (chờ xử lý cọc)", f.getOrDefault("contract.number","Chưa lập"));
            literals.put("Chưa ký", f.getOrDefault("contract.status","Chưa ký"));
        }
        String filename = switch(kind) {
            case CONTRACT -> "Mau_Hop_Dong_Mua_Ban_O_To_Dien_ECS.docx";
            case DEPOSIT -> "ElectricCarSales_BienNhan_Coc_30.docx";
            case INVOICE -> "ElectricCarSales_HoaDon_ThanhToan_100.docx";
        };
        try (var input = SalesWordService.class.getResourceAsStream("/sales-templates/"+filename)) {
            if (input == null) throw new IOException("Không tìm thấy mẫu Word gốc.");
            try (var doc = new XWPFDocument(input); var out = new ByteArrayOutputStream()) {
                int[] address = {0};
                fill(doc, f, literals, address);
                for (var h : doc.getHeaderList()) fill(h,f,literals,address);
                for (var h : doc.getFooterList()) fill(h,f,literals,address);
                doc.write(out);
                return out.toByteArray();
            }
        }
    }

    private static void fill(IBody body, Map<String,String> f, Map<String,String> literals, int[] address) {
        // Walk document order, including nested cells, so the seller/buyer address slots remain distinct.
        for (var element : body.getBodyElements()) {
            if (element instanceof XWPFTable table) {
                for (var row : table.getRows()) {
                    for (int index=0;index<row.getTableCells().size();index++) {
                        var cell=row.getCell(index);
                        var cellSlots=new LinkedHashMap<>(literals);
                        if(index==row.getTableCells().size()-1 && cell.getText().trim().equals("0")) {
                            if(row.getCell(0).getText().contains("Giảm giá")) cellSlots.put("0",value(f,"invoice.discountAmount"));
                            else if(row.getCell(0).getText().contains("Thuế")) cellSlots.put("0",value(f,"invoice.taxAmount"));
                            else if(row.getCell(0).getText().contains("SỐ DƯ")) cellSlots.put("0",value(f,"payment.currentOutstanding"));
                        }
                        fill(cell,f,cellSlots,address);
                    }
                }
            } else if (element instanceof XWPFParagraph paragraph) {
                var nodes = new ArrayList<CTText>();
                var text = new StringBuilder();
                for (var run : paragraph.getRuns()) for (var node : run.getCTR().getTList()) {
                    nodes.add(node); text.append(node.getStringValue());
                }
                var slots = new LinkedHashMap<>(literals);
                if(text.indexOf("ĐẠI DIỆN BÊN BÁN")>=0) slots.put("_____________________",value(f,"seller.representativeName"));
                if(text.indexOf("15/11/2026 • Khách hàng")>=0) {
                    slots.put("15/11/2026 • Khách hàng",date(f.get("payment.remainingDate"))+" • "+
                            ("INSTALLMENT".equals(f.get("payment.mode"))?value(f,"finance.partnerName"):"Khách hàng"));
                }
                var matcher = FIELD.matcher(text);
                while (matcher.find()) slots.put(matcher.group(), value(f,matcher.group(1)));
                if (text.indexOf("Cần Thơ (dữ liệu mẫu)") >= 0)
                    slots.put("Cần Thơ (dữ liệu mẫu)", value(f, address[0]++ == 0 ? "seller.address" : "customer.address"));
                if (slots.isEmpty()) continue;
                var pattern = Pattern.compile(slots.keySet().stream().sorted(Comparator.comparingInt(String::length).reversed())
                        .map(Pattern::quote).collect(java.util.stream.Collectors.joining("|")));
                var matches = pattern.matcher(text);
                var edits = new ArrayList<Edit>();
                while(matches.find()) edits.add(new Edit(matches.start(),matches.end(),slots.get(matches.group())));
                for (var edit : edits.reversed()) {
                    int offset=0;
                    for(var node:nodes) {
                        String old=node.getStringValue(); int end=offset+old.length();
                        if(end>edit.start && offset<edit.end) {
                            int a=Math.max(0,edit.start-offset),b=Math.min(old.length(),edit.end-offset);
                            node.setStringValue(old.substring(0,a)+(offset<=edit.start?edit.value:"")+old.substring(b));
                        }
                        offset=end;
                    }
                }
            }
        }
    }
    private record Edit(int start,int end,String value) {}
    private static String value(Map<String,String> f,String key) {
        String v=f.get(key);return v==null || v.isBlank()?MISSING:v;
    }
    private static String date(String s) {
        if(s==null || s.length()<10)return MISSING;
        return s.substring(8,10)+"/"+s.substring(5,7)+"/"+s.substring(0,4);
    }
    private static String money(String s) {return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(new BigDecimal(s));}
}

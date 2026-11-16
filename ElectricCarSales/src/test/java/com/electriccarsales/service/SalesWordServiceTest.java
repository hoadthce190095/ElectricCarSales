package com.electriccarsales.service;

import static org.junit.jupiter.api.Assertions.*;
import com.electriccarsales.model.SalesPreviewKind;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.junit.jupiter.api.Test;

class SalesWordServiceTest {
    @Test void fillsOriginalTemplatesWithoutDemoTransactionValues() throws Exception {
        var f=new HashMap<String,String>();
        f.put("order.id","82");f.put("customer.name","Khách hàng kiểm thử");
        f.put("customer.address","Địa chỉ kiểm thử");f.put("car.model","Xe thử");f.put("car.version","Bản thử");
        f.put("order.totalAmount","1000000000");f.put("order.depositAmount","300000000");
        f.put("order.remainingInitial","700000000");f.put("payment.currentOutstanding","0");
        f.put("invoice.taxAmount","1234");f.put("invoice.discountAmount","5678");
        f.put("payment.id","25");f.put("payment.depositReference","TEST-DEPOSIT-REF");
        f.put("payment.remainingReference","TEST-BALANCE-REF");
        f.put("contract.number","TEST-CONTRACT");f.put("contract.status","SIGNED");
        f.put("invoice.number","TEST-INVOICE");f.put("payment.mode","INSTALLMENT");
        f.put("finance.partnerName","Đối tác kiểm thử");f.put("payment.remainingDate","2026-11-16 10:00:00");
        for(var kind:SalesPreviewKind.values()) {
            byte[] bytes=new SalesWordService().renderFields(kind,f);
            try(var doc=new XWPFDocument(new ByteArrayInputStream(bytes));var extractor=new XWPFWordExtractor(doc)) {
                String text=extractor.getText();
                assertTrue(text.contains("Đỗ Tấn Đạt"));assertTrue(text.contains("ElectricCarSales"));assertTrue(text.contains("096205004458"));
                assertTrue(text.contains("Khách hàng kiểm thử"));assertTrue(text.contains("Địa chỉ kiểm thử"));
                assertFalse(text.contains("Nguyễn Văn A"));assertFalse(text.contains("800.000.000"));
                assertFalse(text.contains("DEMO-REF"));
                assertTrue(doc.getTables().size()>0);
                if(kind==SalesPreviewKind.INVOICE) {
                    assertTrue(text.contains("1.234"));assertTrue(text.contains("5.678"));
                    assertTrue(text.contains("16/11/2026 • Đối tác kiểm thử"));
                }
                if(kind==SalesPreviewKind.CONTRACT) {
                    assertTrue(text.contains("Đỗ Tấn Đạt"));assertTrue(text.contains("0917231548 (Do Tan Dat)"));
                    assertTrue(text.contains("ĐIỀU 10."));assertFalse(text.contains("{{customer.name}}"));
                }
            }
        }
    }
    @Test void readsDongAmountsExactly() {
        assertEquals("Một tỷ đồng",VietnameseDong.words("1000000000.00"));
        assertEquals("Ba trăm triệu đồng",VietnameseDong.words("300000000"));
        assertEquals("Một trăm lẻ năm đồng",VietnameseDong.words("105"));
        assertThrows(ArithmeticException.class,()->VietnameseDong.words("1.25"));
    }
}

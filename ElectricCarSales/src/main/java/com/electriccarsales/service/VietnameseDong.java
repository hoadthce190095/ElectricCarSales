package com.electriccarsales.service;

import java.math.BigDecimal;
import java.util.ArrayList;

final class VietnameseDong {
    private static final String[] DIGITS={"không","một","hai","ba","bốn","năm","sáu","bảy","tám","chín"};
    private static final String[] UNITS={""," nghìn"," triệu"," tỷ"," nghìn tỷ"," triệu tỷ"};
    static String words(String amount) {
        if(amount==null)return "Chưa cung cấp";
        long value=new BigDecimal(amount).longValueExact();
        if(value<0)throw new IllegalArgumentException("Số tiền không được âm.");
        if(value==0)return "Không đồng";
        var groups=new ArrayList<Integer>();
        while(value>0){groups.add((int)(value%1000));value/=1000;}
        var text=new StringBuilder();
        for(int i=groups.size()-1;i>=0;i--) {
            int n=groups.get(i);if(n==0)continue;
            if(!text.isEmpty())text.append(' ');
            int h=n/100,t=n/10%10,u=n%10;
            if(h>0 || i<groups.size()-1)text.append(DIGITS[h]).append(" trăm ");
            if(t>1)text.append(DIGITS[t]).append(" mươi ");
            else if(t==1)text.append("mười ");
            else if(u>0 && (h>0 || i<groups.size()-1))text.append("lẻ ");
            if(u>0)text.append(u==1 && t>1?"mốt":u==5 && t>0?"lăm":DIGITS[u]);
            while(!text.isEmpty() && text.charAt(text.length()-1)==' ')text.setLength(text.length()-1);
            text.append(UNITS[i]);
        }
        return Character.toUpperCase(text.charAt(0))+text.substring(1)+" đồng";
    }
}

package com.qros.modules.promotion.service;

public interface VoucherUsageCheckPort {

    boolean isVoucherInActiveUse(String voucherCode);
}

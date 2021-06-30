package com.rbkmoney.scheduledpayoutworker.util;

public class GenerateUtil {

    public static String generatePayoutScheduleId(String partyId, String shopId, int scheduleId) {
        return "payouter-" + partyId + "-" + shopId + "-" + scheduleId;
    }

    private GenerateUtil() {
        throw new UnsupportedOperationException("Unable to instantiate utility class!");
    }
}

package com.rbkmoney.scheduledpayoutworker.util;

public class GenerateUtil {

    public static String generatePayoutScheduleId(int scheduleId) {
        return "payouter-" + scheduleId;
    }

    private GenerateUtil() {
        throw new UnsupportedOperationException("Unable to instantiate utility class!");
    }
}

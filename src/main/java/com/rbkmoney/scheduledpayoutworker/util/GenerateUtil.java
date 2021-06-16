package com.rbkmoney.scheduledpayoutworker.util;

public class GenerateUtil {

    public static String generatePayoutScheduleId(int scheduleId){
        return "payouter-" + String.valueOf(scheduleId);
    }

    private GenerateUtil(){
        throw new UnsupportedOperationException("Unable to instantiate utility class!");
    }
}

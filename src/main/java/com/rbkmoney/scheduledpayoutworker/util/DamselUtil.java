package com.rbkmoney.scheduledpayoutworker.util;

import com.rbkmoney.damsel.domain.CashFlowAccount;
import com.rbkmoney.damsel.domain.FinalCashFlowAccount;
import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.domain.MerchantCashFlowAccount;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DamselUtil {

    public static Long computeMerchantAmount(List<FinalCashFlowPosting> finalCashFlow) {
        long amountSource = computeAmount(finalCashFlow, FinalCashFlowPosting::getSource);
        long amountDest = computeAmount(finalCashFlow, FinalCashFlowPosting::getDestination);
        return amountDest - amountSource;
    }

    private static long computeAmount(List<FinalCashFlowPosting> finalCashFlow,
                                      Function<FinalCashFlowPosting, FinalCashFlowAccount> func) {
        return finalCashFlow.stream()
                .filter(f -> isMerchantSettlement(func.apply(f).getAccountType()))
                .mapToLong(cashFlow -> cashFlow.getVolume().getAmount())
                .sum();
    }

    private static boolean isMerchantSettlement(CashFlowAccount cashFlowAccount) {
        return cashFlowAccount.isSetMerchant()
                && cashFlowAccount.getMerchant() == MerchantCashFlowAccount.settlement;
    }

    public static Map<CashFlowType, Long> parseCashFlow(List<FinalCashFlowPosting> finalCashFlow) {
        return finalCashFlow.stream().collect(
                Collectors.groupingBy(CashFlowType::getCashFlowType,
                        Collectors.summingLong(cashFlow -> cashFlow.getVolume().getAmount())));
    }

    private DamselUtil() {
        throw new UnsupportedOperationException("Unable to instantiate utility class!");
    }

}

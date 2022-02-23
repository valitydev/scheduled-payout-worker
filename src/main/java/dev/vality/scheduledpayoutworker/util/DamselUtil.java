package dev.vality.scheduledpayoutworker.util;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.payment_processing.ClaimStatus;
import dev.vality.damsel.payment_processing.PartyChange;

import java.util.List;
import java.util.function.Function;

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

    private DamselUtil() {
        throw new UnsupportedOperationException("Unable to instantiate utility class!");
    }

    public static ClaimStatus getClaimStatus(PartyChange change) {
        ClaimStatus claimStatus = null;
        if (change.isSetClaimCreated()) {
            claimStatus = change.getClaimCreated().getStatus();
        } else if (change.isSetClaimStatusChanged()) {
            claimStatus = change.getClaimStatusChanged().getStatus();
        }
        return claimStatus;
    }

    public static boolean isClaimAccepted(PartyChange change) {
        ClaimStatus claimStatus = getClaimStatus(change);
        return claimStatus != null && claimStatus.isSetAccepted();
    }

}

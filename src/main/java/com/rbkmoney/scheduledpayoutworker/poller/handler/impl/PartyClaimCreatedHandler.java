package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import com.rbkmoney.scheduledpayoutworker.service.SchedulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.rbkmoney.scheduledpayoutworker.util.DamselUtil.getClaimStatus;

@Component
@RequiredArgsConstructor
public class PartyClaimCreatedHandler implements PartyManagementHandler {

    private final SchedulatorService schedulatorService;

    @Override
    public boolean accept(PartyChange change) {
        //TODO: Уточнить касательно корректности фильтра
        return getClaimStatus(change).isSetAccepted();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PartyChange change, MachineEvent event) {
        ClaimStatusChanged claimStatusChanged = change.getClaimStatusChanged();
        String partyId = event.getSourceId();
        List<ClaimEffect> claimEffects = claimStatusChanged
                .getStatus()
                .getAccepted()
                .getEffects();

        for (ClaimEffect claimEffect : claimEffects) {
            if (claimEffect.isSetShopEffect()) {
                ShopEffectUnit shopEffectUnit = claimEffect.getShopEffect();
                String shopId = shopEffectUnit.getShopId();
                ShopEffect shopEffect = shopEffectUnit.getEffect();
                if (shopEffect.isSetPayoutScheduleChanged()) {
                    ScheduleChanged scheduleChanged = shopEffect.getPayoutScheduleChanged();
                    if (scheduleChanged.isSetSchedule()) {
                        schedulatorService.registerJob(partyId, shopId, scheduleChanged.getSchedule());
                    } else {
                        schedulatorService.deregisterJob(partyId, shopId);
                    }
                } else if (shopEffect.isSetCreated()) {
                    Shop shop = shopEffect.getCreated();
                    if (shop.isSetPayoutSchedule()) {
                        schedulatorService.registerJob(partyId, shopId, shop.getPayoutSchedule());
                    }
                }
            }
        }
    }

}

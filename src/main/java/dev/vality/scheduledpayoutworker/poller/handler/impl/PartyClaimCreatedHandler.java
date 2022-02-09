package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.payment_processing.*;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import dev.vality.scheduledpayoutworker.service.SchedulatorService;
import dev.vality.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PartyClaimCreatedHandler implements PartyManagementHandler {

    private final SchedulatorService schedulatorService;

    private final ShopMetaDao shopMetaDao;

    @Override
    public boolean accept(PartyChange change, MachineEvent event) {
        return DamselUtil.isClaimAccepted(change);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PartyChange change, MachineEvent event) {
        String partyId = event.getSourceId();
        List<ClaimEffect> claimEffects = DamselUtil.getClaimStatus(change)
                .getAccepted()
                .getEffects();

        for (ClaimEffect claimEffect : claimEffects) {
            if (claimEffect.isSetShopEffect()) {
                ShopEffectUnit shopEffectUnit = claimEffect.getShopEffect();
                String shopId = shopEffectUnit.getShopId();
                ShopEffect shopEffect = shopEffectUnit.getEffect();
                ShopMeta shopMeta = shopMetaDao.get(partyId, shopId);
                if (shopMeta != null && shopMeta.getHasPaymentInstitutionAccPayTool()) {
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

}

package dev.vality.scheduledpayoutworker.poller.handler.impl;

import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.payment_processing.ClaimEffect;
import dev.vality.damsel.payment_processing.PartyChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import dev.vality.scheduledpayoutworker.service.PartyManagementService;
import dev.vality.scheduledpayoutworker.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Component
@Order(HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ShopCreatedHandler implements PartyManagementHandler {

    private final PartyManagementService partyManagementService;
    private final ShopMetaDao shopMetaDao;

    @Override
    public boolean accept(PartyChange partyChange, MachineEvent event) {
        return DamselUtil.isClaimAccepted(partyChange);
    }

    @Override
    public void handle(PartyChange change, MachineEvent event) {
        List<ClaimEffect> claimEffects = DamselUtil.getClaimStatus(change).getAccepted().getEffects();
        for (ClaimEffect claimEffect : claimEffects) {
            if (claimEffect.isSetShopEffect() && claimEffect.getShopEffect().getEffect().isSetCreated()) {
                handleEvent(event, claimEffect);
            }
        }
    }

    private void handleEvent(MachineEvent event, ClaimEffect claimEffect) {

        String shopId = claimEffect.getShopEffect().getShopId();
        String partyId = event.getSourceId();

        Party party = partyManagementService.getParty(partyId);
        Shop shop = party.getShops().get(shopId);

        if (shop == null) {
            log.warn("Shop not found, partyId='{}', shopId='{}'", partyId, shopId);
            return;
        }

        shopMetaDao.update(partyId, shopId);
        log.info("Shop have been saved, partyId={}, shopId={}", partyId, shopId);
    }

}

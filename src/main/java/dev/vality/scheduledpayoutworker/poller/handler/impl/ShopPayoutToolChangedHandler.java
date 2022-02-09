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
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopPayoutToolChangedHandler implements PartyManagementHandler {

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
            if (claimEffect.isSetShopEffect() && claimEffect.getShopEffect().getEffect().isSetPayoutToolChanged()) {
                handleEvent(event, claimEffect);
            }
        }
    }

    private void handleEvent(MachineEvent event, ClaimEffect claimEffect) {

        String shopId = claimEffect.getShopEffect().getShopId();
        String partyId = event.getSourceId();

        String changedPayoutToolId = claimEffect.getShopEffect().getEffect().getPayoutToolChanged();

        Party party = partyManagementService.getParty(partyId);
        Shop shop = party.getShops().get(shopId);
        boolean hasPaymentInstitutionAccPayTool =
                DamselUtil.hasPaymentInstitutionAccountPayTool(party, shop.getContractId(), changedPayoutToolId);

        if (hasPaymentInstitutionAccPayTool || shopMetaDao.get(partyId, shopId) != null) {
            shopMetaDao.update(partyId, shopId, hasPaymentInstitutionAccPayTool);
            log.info("Shop have been saved, partyId={}, shopId={}", partyId, shopId);
        }
    }
}

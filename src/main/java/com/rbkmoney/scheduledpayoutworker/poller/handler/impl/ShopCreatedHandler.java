package com.rbkmoney.scheduledpayoutworker.poller.handler.impl;

import com.rbkmoney.damsel.domain.Party;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.ClaimEffect;
import com.rbkmoney.damsel.payment_processing.PartyChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.poller.handler.PartyManagementHandler;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.rbkmoney.scheduledpayoutworker.util.DamselUtil.getClaimStatus;
import static com.rbkmoney.scheduledpayoutworker.util.DamselUtil.hasPaymentInstitutionAccountPayTool;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCreatedHandler implements PartyManagementHandler {

    private final PartyManagementService partyManagementService;
    private final ShopMetaDao shopMetaDao;

    @Override
    public void handle(PartyChange change, MachineEvent event) {
        List<ClaimEffect> claimEffects = getClaimStatus(change).getAccepted().getEffects();
        for (ClaimEffect claimEffect : claimEffects) {
            if (claimEffect.isSetShopEffect() && claimEffect.getShopEffect().getEffect().isSetCreated()) {
                handleEvent(event, claimEffect);
            }
        }
    }

    @Override
    public Filter<PartyChange> getFilter() {
        return null;
    }

    private void handleEvent(MachineEvent event, ClaimEffect claimEffect) {

        String shopId = claimEffect.getShopEffect().getShopId();
        String partyId = event.getSourceId();

        Party party = partyManagementService.getParty(partyId);
        Shop shop = party.getShops().get(shopId);

        if (hasPaymentInstitutionAccountPayTool(party, shop.getContractId(), shop.getPayoutToolId())) {
            shopMetaDao.save(partyId, shopId, true);
        }

    }

}

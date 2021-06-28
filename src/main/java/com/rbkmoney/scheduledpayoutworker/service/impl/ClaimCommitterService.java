package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.scheduledpayoutworker.handler.CommitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimCommitterService implements ClaimCommitterSrv.Iface {

    private final CommitHandler<ScheduleModification> pmCommitHandler;

    @Override
    public void accept(String partyId, Claim receivedClaim) throws PartyNotFound, InvalidChangeset, TException {
        
    }

    @Override
    public void commit(String partyId, Claim claim) throws TException {
        for (ModificationUnit modificationUnit : claim.getChangeset()) {
            Modification modification = modificationUnit.getModification();
            if (modification.isSetPartyModification()) {
                PartyModification partyModification = modification.getPartyModification();
                if (partyModification.isSetShopModification()) {
                    ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
                    String shopId = shopModificationUnit.getId();
                    ShopModification shopModification = shopModificationUnit.getModification();
                    if (shopModification.isSetPayoutScheduleModification()) {
                        pmCommitHandler.commit(partyId, shopId, shopModification.getPayoutScheduleModification());
                    }
                }
            }
        }
    }

}

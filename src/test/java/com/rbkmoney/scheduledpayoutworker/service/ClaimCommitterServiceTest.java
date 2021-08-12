package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.handler.CommitHandler;
import com.rbkmoney.scheduledpayoutworker.service.impl.ClaimCommitterService;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.mockito.Mockito.*;

class ClaimCommitterServiceTest {

    @Mock
    private CommitHandler<ScheduleModification> handler;

    @Mock
    private ShopMetaDao shopMetaDao;

    private ClaimCommitterService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {handler};
        service = new ClaimCommitterService(handler, shopMetaDao);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() throws TException {
        String shopId = generateRandomStringId();
        String partyId = generateRandomStringId();
        Claim claim = prepareAcceptedClaim(shopId);
        service.accept(partyId, claim);
        verify(handler, times(1)).accept(eq(partyId), eq(shopId), notNull());
    }

    @Test
    void commit() throws TException {
        String shopId = generateRandomStringId();
        String partyId = generateRandomStringId();
        Claim claim = prepareAcceptedClaim(shopId);
        service.commit(partyId, claim);
        verify(handler, times(1)).commit(eq(partyId), eq(shopId), notNull());
    }

    private Claim prepareAcceptedClaim(String shopId) {
        Claim claim = fillTBaseObject(new Claim(), Claim.class);
        ModificationUnit modificationUnit = fillTBaseObject(new ModificationUnit(), ModificationUnit.class);
        claim.setChangeset(List.of(modificationUnit));
        Modification modification = fillTBaseObject(new Modification(), Modification.class);
        modificationUnit.setModification(modification);
        PartyModification partyModification = fillTBaseObject(new PartyModification(), PartyModification.class);
        modification.setPartyModification(partyModification);
        ShopModificationUnit shopModificationUnit =
                fillTBaseObject(new ShopModificationUnit(), ShopModificationUnit.class);
        shopModificationUnit.setId(shopId);
        partyModification.setShopModification(shopModificationUnit);
        ShopModification shopModification = fillTBaseObject(new ShopModification(), ShopModification.class);
        shopModificationUnit.setModification(shopModification);
        ScheduleModification scheduleModification =
                fillTBaseObject(new ScheduleModification(), ScheduleModification.class);
        shopModification.setPayoutScheduleModification(scheduleModification);
        return claim;
    }
}
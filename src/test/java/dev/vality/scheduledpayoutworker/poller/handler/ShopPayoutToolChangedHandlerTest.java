package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.ShopPayoutToolChangedHandler;
import dev.vality.scheduledpayoutworker.service.PartyManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static dev.vality.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ShopPayoutToolChangedHandlerTest {
    @Mock
    private PartyManagementService partyManagementService;

    @Mock
    private ShopMetaDao shopMetaDao;

    private ShopPayoutToolChangedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new ShopPayoutToolChangedHandler(partyManagementService, shopMetaDao);
        preparedMocks = new Object[] {partyManagementService, shopMetaDao};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        assertTrue(handler.accept(preparePartyChange(), prepareEvent()));
    }

    @Test
    void handle() {
        PartyChange change = preparePartyChange();
        MachineEvent event = prepareEvent();

        String partyId = event.getSourceId();
        ShopEffectUnit effectUnit = change.getClaimCreated().getStatus()
                .getAccepted().getEffects().get(0)
                .getShopEffect();

        String shopId = effectUnit.getShopId();

        Party party = fillTBaseObject(new Party(), Party.class);
        party.setShops(Map.of(shopId, fillTBaseObject(new Shop(), Shop.class)));

        when(partyManagementService.getParty(partyId)).thenReturn(party);
        when(shopMetaDao.get(partyId, shopId)).thenReturn(new ShopMeta());
        handler.handle(change, event);
        verify(partyManagementService, times(1))
                .getParty(partyId);
        verify(shopMetaDao, times(1)).get(partyId, shopId);
        verify(shopMetaDao, times(1)).update(partyId, shopId, false);
    }


    private PartyChange preparePartyChange() {
        PartyChange change = fillTBaseObject(new PartyChange(), PartyChange.class);
        Claim claim = fillTBaseObject(new Claim(), Claim.class);
        change.setClaimCreated(claim);
        ClaimStatus status = fillTBaseObject(new ClaimStatus(), ClaimStatus.class);
        claim.setStatus(status);

        ClaimAccepted acceptedStatus = fillTBaseObject(new ClaimAccepted(), ClaimAccepted.class);
        status.setAccepted(acceptedStatus);

        ClaimEffect claimEffect = fillTBaseObject(new ClaimEffect(), ClaimEffect.class);
        ShopEffectUnit shopEffectUnit = fillTBaseObject(new ShopEffectUnit(), ShopEffectUnit.class);
        ShopEffect shopEffect = fillTBaseObject(new ShopEffect(), ShopEffect.class);
        shopEffectUnit.setEffect(shopEffect);
        claimEffect.setShopEffect(shopEffectUnit);
        shopEffect.setPayoutToolChanged(generateRandomStringId());
        List<ClaimEffect> claimEffects = List.of(claimEffect);
        acceptedStatus.setEffects(claimEffects);
        return change;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }
}
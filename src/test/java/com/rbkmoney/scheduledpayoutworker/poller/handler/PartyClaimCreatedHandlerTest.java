package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.domain.BusinessScheduleRef;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.scheduledpayoutworker.poller.handler.impl.PartyClaimCreatedHandler;
import com.rbkmoney.scheduledpayoutworker.service.SchedulatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.rbkmoney.scheduledpayoutworker.integration.data.TestData.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PartyClaimCreatedHandlerTest {

    @Mock
    private SchedulatorService schedulatorService;

    private PartyClaimCreatedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new PartyClaimCreatedHandler(schedulatorService);
        preparedMocks = new Object[] {schedulatorService};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        assertTrue(handler.accept(preparePartyChange()));
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

        handler.handle(change, event);
        verify(schedulatorService, times(1))
                .registerJob(partyId, shopId, effectUnit.getEffect().getPayoutScheduleChanged().getSchedule());
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
        ScheduleChanged scheduleChanged = fillTBaseObject(new ScheduleChanged(), ScheduleChanged.class);
        shopEffect.setPayoutScheduleChanged(scheduleChanged);
        BusinessScheduleRef scheduleRef = fillTBaseObject(new BusinessScheduleRef(), BusinessScheduleRef.class);
        scheduleChanged.setSchedule(scheduleRef);
        List<ClaimEffect> claimEffects = List.of(claimEffect);
        acceptedStatus.setEffects(claimEffects);
        return change;
    }

    private MachineEvent prepareEvent() {
        MachineEvent event = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        return event;
    }

}
package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.domain.BusinessScheduleRef;
import dev.vality.damsel.payment_processing.*;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
import dev.vality.scheduledpayoutworker.dao.ShopMetaDao;
import dev.vality.scheduledpayoutworker.poller.handler.impl.PartyClaimCreatedHandler;
import dev.vality.scheduledpayoutworker.service.SchedulatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PartyClaimCreatedHandlerTest {

    @Mock
    private SchedulatorService schedulatorService;

    @Mock
    private ShopMetaDao shopMetaDao;

    private PartyClaimCreatedHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new PartyClaimCreatedHandler(schedulatorService, shopMetaDao);
        preparedMocks = new Object[] {schedulatorService};
        ShopMeta shopMeta = new ShopMeta();
        shopMeta.setHasPaymentInstitutionAccPayTool(true);
        Mockito.when(shopMetaDao.get(anyString(), anyString()))
                .thenReturn(shopMeta);
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
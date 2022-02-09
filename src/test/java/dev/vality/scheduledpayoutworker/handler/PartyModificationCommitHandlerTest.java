package dev.vality.scheduledpayoutworker.handler;

import dev.vality.damsel.claim_management.ScheduleModification;
import dev.vality.damsel.domain.BusinessScheduleRef;
import dev.vality.scheduledpayoutworker.service.DominantService;
import dev.vality.scheduledpayoutworker.service.SchedulatorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static dev.vality.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.mockito.Mockito.*;

class PartyModificationCommitHandlerTest {
    @Mock
    private SchedulatorService schedulatorService;
    @Mock
    private DominantService dominantService;

    private PartyModificationCommitHandler handler;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {schedulatorService, dominantService};
        handler = new PartyModificationCommitHandler(schedulatorService, dominantService);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void accept() {
        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();
        ScheduleModification scheduleModification = prepareScheduleModification();
        handler.accept(partyId, shopId, scheduleModification);
        verify(dominantService, times(1)).getBusinessSchedule(scheduleModification.getSchedule());
    }

    @Test
    void commit() {
        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();
        ScheduleModification scheduleModification = prepareScheduleModification();
        handler.commit(partyId, shopId, scheduleModification);
        verify(schedulatorService, times(1)).registerJob(partyId, shopId, scheduleModification.getSchedule());
    }

    private ScheduleModification prepareScheduleModification() {
        ScheduleModification scheduleModification =
                fillTBaseObject(new ScheduleModification(), ScheduleModification.class);
        BusinessScheduleRef scheduleRef = fillTBaseObject(new BusinessScheduleRef(), BusinessScheduleRef.class);
        scheduleModification.setSchedule(scheduleRef);
        return scheduleModification;
    }
}
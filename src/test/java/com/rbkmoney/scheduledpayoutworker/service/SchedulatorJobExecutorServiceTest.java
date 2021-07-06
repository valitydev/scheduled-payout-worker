package com.rbkmoney.scheduledpayoutworker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.schedule.ContextValidationResponse;
import com.rbkmoney.damsel.schedule.ExecuteJobRequest;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.scheduledpayoutworker.dao.ShopMetaDao;
import com.rbkmoney.scheduledpayoutworker.model.ScheduledJobContext;
import com.rbkmoney.scheduledpayoutworker.serde.impl.ScheduledJobSerializer;
import com.rbkmoney.scheduledpayoutworker.service.impl.SchedulatorJobExecutorServiceImpl;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

class SchedulatorJobExecutorServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ScheduledJobSerializer scheduledJobSerializer;
    @Mock
    private ShopMetaDao shopMetaDao;
    @Mock
    private PayoutManagerService payoutManagerService;

    private SchedulatorJobExecutorServiceImpl service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {shopMetaDao, payoutManagerService};
        scheduledJobSerializer = new ScheduledJobSerializer(mapper);
        service = new SchedulatorJobExecutorServiceImpl(scheduledJobSerializer, shopMetaDao, payoutManagerService);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void validateExecutionContext() throws JsonProcessingException, TException {
        ScheduledJobContext jobContext = new ScheduledJobContext();
        jobContext.setJobId(generateRandomStringId());
        jobContext.setShopId(generateRandomStringId());
        jobContext.setPartyId(generateRandomStringId());

        ObjectMapper objectMapper = new ObjectMapper();
        ByteBuffer byteBuffer = ByteBuffer.wrap(objectMapper.writeValueAsBytes(jobContext));
        ContextValidationResponse response = service.validateExecutionContext(byteBuffer);
        assertTrue(response.getResponseStatus().isSetSuccess());
    }

    @Test
    void executeJob() throws JsonProcessingException, TException {
        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();

        ScheduledJobContext jobContext = new ScheduledJobContext();
        jobContext.setJobId(generateRandomStringId());
        jobContext.setShopId(shopId);
        jobContext.setPartyId(partyId);
        String toTime = "2021-07-01T10:15:30Z";
        byte[] context = mapper.writeValueAsBytes(jobContext);
        ExecuteJobRequest executeJobRequest = fillTBaseObject(new ExecuteJobRequest(), ExecuteJobRequest.class);
        executeJobRequest.setServiceExecutionContext(context);

        var scheduledJobContext = fillTBaseObject(new com.rbkmoney.damsel.schedule.ScheduledJobContext(),
                com.rbkmoney.damsel.schedule.ScheduledJobContext.class);
        executeJobRequest.setScheduledJobContext(scheduledJobContext);
        scheduledJobContext.setNextCronTime(toTime);

        ShopMeta shopMeta = new ShopMeta();
        shopMeta.setPartyId(partyId);
        shopMeta.setShopId(shopId);
        when(shopMetaDao.get(partyId, shopId)).thenReturn(shopMeta);
        when(payoutManagerService
                .createPayoutByRange(eq(partyId), eq(shopId), notNull()))
                .thenReturn("test");
        ByteBuffer response = service.executeJob(executeJobRequest);
        assertEquals(ByteBuffer.wrap(context), response);
        verify(shopMetaDao, times(1)).get(partyId, shopId);
        verify(payoutManagerService, times(1))
                .createPayoutByRange(eq(jobContext.getPartyId()), eq(jobContext.getShopId()), notNull());

    }
}
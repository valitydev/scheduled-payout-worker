package dev.vality.scheduledpayoutworker.integration;

import dev.vality.damsel.accounter.AccounterSrv;
import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain_config.RepositoryClientSrv;
import dev.vality.damsel.domain_config.VersionedObject;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.damsel.schedule.*;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.payout.manager.Payout;
import dev.vality.payout.manager.PayoutManagementSrv;
import dev.vality.payout.manager.PayoutParams;
import dev.vality.payouter.domain.tables.ShopMeta;
import dev.vality.scheduledpayoutworker.config.AbstractKafkaTestContainerConfig;
import dev.vality.scheduledpayoutworker.dao.mapper.RecordRowMapper;
import dev.vality.scheduledpayoutworker.integration.config.TestKafkaConfig;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static dev.vality.scheduledpayoutworker.util.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestKafkaConfig.class)
@ExtendWith(MockitoExtension.class)
class ScheduledPayoutWorkerIntegrationTest extends AbstractKafkaTestContainerConfig {

    private static final int MOCK_TIMEOUT_MILLIS = 10000;

    @MockBean
    public PartyManagementSrv.Iface partyManagementClient;

    @MockBean
    public RepositoryClientSrv.Iface dominantClient;

    @MockBean
    public SchedulatorSrv.Iface schedulatorClient;

    @MockBean
    public PayoutManagementSrv.Iface payoutManagerClient;

    @MockBean
    public AccounterSrv.Iface shumwayClient;

    @Autowired
    private KafkaTemplate<String, SinkEvent> producer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${kafka.topics.party-management.id}")
    private String partyTopic;

    @LocalServerPort
    private int port;

    @Captor
    private ArgumentCaptor<String> scheduleIdCaptor;

    @Captor
    private ArgumentCaptor<RegisterJobRequest> jobRequestCaptor;

    @Captor
    private ArgumentCaptor<PayoutParams> payoutParamsCaptor;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {partyManagementClient, dominantClient, schedulatorClient, payoutManagerClient,
                shumwayClient};
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    @SneakyThrows
    void readInvoicesAndProcessSchedulatorJobSuccessfully() {

        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();
        Integer paymentInstitutionId = generateRandomIntId();

        Party party = createParty(partyId, shopId, paymentInstitutionId);
        VersionedObject versionedObject = createVersionedObject();

        //Create shop via ShopCreatedHandler

        when(partyManagementClient.get(any(), eq(partyId))).thenReturn(party);
        when(dominantClient.checkoutObject(any(), any())).thenReturn(versionedObject);

        producer.send(partyTopic, shopCreatedEvent(partyId, shopId));

        verify(partyManagementClient, timeout(MOCK_TIMEOUT_MILLIS).times(3))
                .get(any(), eq(partyId));
        verify(dominantClient, timeout(MOCK_TIMEOUT_MILLIS).times(1))
                .checkoutObject(any(),
                        argThat(reference -> paymentInstitutionId.equals(reference.getPaymentInstitution().getId())));
        verify(schedulatorClient, timeout(MOCK_TIMEOUT_MILLIS).times(1))
                .registerJob(scheduleIdCaptor.capture(), jobRequestCaptor.capture());

        List<dev.vality.payouter.domain.tables.pojos.ShopMeta> shopMetas =
                jdbcTemplate
                        .query(
                                "SELECT party_id, shop_id, wtime, last_payout_created_at " +
                                        "FROM pt.shop_meta",
                                new RecordRowMapper<>(new ShopMeta(),
                                        dev.vality.payouter.domain.tables.pojos.ShopMeta.class));
        assertEquals(1, shopMetas.size());
        var shopMeta = shopMetas.get(0);

        assertAll(
                () -> assertEquals(partyId, shopMeta.getPartyId()),
                () -> assertEquals(shopId, shopMeta.getShopId()),
                () -> assertNotNull(shopMeta.getWtime()),
                () -> assertNull(shopMeta.getLastPayoutCreatedAt())
        );

        //Check triggered job processing
        ScheduledJobExecutorSrv.Iface client = new THSpawnClientBuilder()
                .withNetworkTimeout(0)
                .withAddress(new URI("http://localhost:" + port + "/v1/schedulator"))
                .build(ScheduledJobExecutorSrv.Iface.class);

        RegisterJobRequest registerJobRequest = jobRequestCaptor.getValue();
        ContextValidationResponse validationResponse =
                client.validateExecutionContext(registerJobRequest.bufferForContext());

        assertTrue(validationResponse.getResponseStatus().isSetSuccess());

        var currentTime = LocalDateTime.now();
        final String toTime = TypeUtil.temporalToString(currentTime);
        final long amount = 1234;


        when(shumwayClient.getAccountBalance(Long.parseLong(shopId), toTime)).thenReturn(amount);
        var payout = fillTBaseObject(new Payout(), Payout.class);
        when(payoutManagerClient.createPayout(any())).thenReturn(payout);

        ExecuteJobRequest executeJobRequest = fillTBaseObject(new ExecuteJobRequest(), ExecuteJobRequest.class);
        executeJobRequest.setServiceExecutionContext(registerJobRequest.bufferForContext());
        ScheduledJobContext context = fillTBaseObject(new ScheduledJobContext(), ScheduledJobContext.class);
        context.setNextCronTime(toTime);
        executeJobRequest.setScheduledJobContext(context);
        assertEquals(registerJobRequest.bufferForContext(), client.executeJob(executeJobRequest));

        verify(partyManagementClient, times(4)).get(any(), eq(partyId));
        verify(shumwayClient, times(1))
                .getAccountBalance(Long.parseLong(shopId), toTime);
        verify(payoutManagerClient, times(1)).createPayout(payoutParamsCaptor.capture());

        PayoutParams payoutParams = payoutParamsCaptor.getValue();

        assertAll(
                () -> assertEquals(party.getShops().get(shopId).getAccount().getCurrency().getSymbolicCode(),
                        payoutParams.getCash().getCurrency().getSymbolicCode()),
                () -> assertEquals(amount, payoutParams.getCash().getAmount()),
                () -> assertEquals(partyId, payoutParams.getShopParams().getPartyId()),
                () -> assertEquals(shopId, payoutParams.getShopParams().getShopId())
        );

        shopMetas = jdbcTemplate.query("SELECT party_id, shop_id, wtime, last_payout_created_at FROM pt.shop_meta",
                new RecordRowMapper<>(new ShopMeta(), dev.vality.payouter.domain.tables.pojos.ShopMeta.class));
        assertEquals(1, shopMetas.size());
        var payoutShopMeta = shopMetas.get(0);

        assertAll(() -> assertEquals(partyId, payoutShopMeta.getPartyId()),
                () -> assertEquals(shopId, payoutShopMeta.getShopId()),
                () -> assertNotNull(payoutShopMeta.getWtime()),
                () -> assertEquals(toTime,
                        TypeUtil.temporalToString(payoutShopMeta.getLastPayoutCreatedAt())));
    }

}

package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.*;
import dev.vality.payout.manager.Payout;
import dev.vality.payout.manager.PayoutManagementSrv;
import dev.vality.payout.manager.PayoutParams;
import dev.vality.payout.manager.ShopParams;
import dev.vality.scheduledpayoutworker.dao.*;
import dev.vality.scheduledpayoutworker.service.impl.PayoutManagerServiceImpl;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.UUID;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static dev.vality.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PayoutManagerServiceTest {

    @Mock
    private PaymentDao paymentDao;
    @Mock
    private RefundDao refundDao;
    @Mock
    private AdjustmentDao adjustmentDao;
    @Mock
    private ChargebackDao chargebackDao;
    @Mock
    private PayoutDao payoutDao;
    @Mock
    private PayoutManagementSrv.Iface payoutManagerClient;
    @Mock
    private PartyManagementService partyManagementService;
    @Captor
    private ArgumentCaptor<PayoutParams> payoutParamsCaptor;

    private PayoutManagerService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {paymentDao, refundDao, adjustmentDao, chargebackDao,
                payoutDao, payoutManagerClient, partyManagementService};
        service = new PayoutManagerServiceImpl(paymentDao, refundDao, adjustmentDao, chargebackDao,
                payoutDao, payoutManagerClient, partyManagementService);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void createPayoutByRange() throws TException {
        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();
        LocalDateTime toTime = LocalDateTime.now();
        LocalDateTime fromTime = toTime.minusDays(7);
        Shop shop = prepareShop(shopId);
        long amount = 100L;
        String payoutId = UUID.randomUUID().toString();

        when(partyManagementService.getShop(partyId, shopId)).thenReturn(shop);
        when(paymentDao.includeUnpaid(payoutId, partyId, shopId, fromTime, toTime)).thenReturn(1);
        when(refundDao.includeUnpaid(notNull(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime))).thenReturn(0);
        when(adjustmentDao.includeUnpaid(notNull(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime))).thenReturn(0);
        when(chargebackDao.includeUnpaid(notNull(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime))).thenReturn(0);
        when(payoutDao.getAvailableAmount(notNull())).thenReturn(amount);

        Payout payout = new Payout();
        payout.setPayoutId(payoutId);
        when(payoutManagerClient.createPayout(payoutParamsCaptor.capture())).thenReturn(payout);

        String actualPayoutId = service.createPayoutByRange(partyId, shopId, toTime);
        PayoutParams payoutParams = payoutParamsCaptor.getValue();
        assertEquals(payoutParams.getPayoutId(), actualPayoutId);

        String symbolicCode = shop.getAccount().getCurrency().getSymbolicCode();
        CurrencyRef currency = new CurrencyRef().setSymbolicCode(symbolicCode);
        Cash cash = new Cash().setAmount(amount).setCurrency(currency);
        assertEquals(cash, payoutParams.getCash());

        ShopParams shopParams = payoutParams.getShopParams();
        assertEquals(partyId, shopParams.getPartyId());
        assertEquals(shopId, shopParams.getShopId());

        verify(partyManagementService, times(1)).getShop(partyId, shopId);
        verify(paymentDao, times(1)).includeUnpaid(actualPayoutId, partyId, shopId, fromTime, toTime);
        verify(refundDao, times(1)).includeUnpaid(actualPayoutId, partyId, shopId, fromTime, toTime);
        verify(adjustmentDao, times(1)).includeUnpaid(actualPayoutId, partyId, shopId, fromTime, toTime);
        verify(chargebackDao, times(1)).includeUnpaid(actualPayoutId, partyId, shopId, fromTime, toTime);
        verify(payoutDao, times(1)).getAvailableAmount(actualPayoutId);
        verify(payoutManagerClient, times(1)).createPayout(payoutParams);

    }

    private Shop prepareShop(String shopId) {
        Shop shop = fillTBaseObject(new Shop(), Shop.class);
        shop.setId(shopId);

        Blocking blocking = fillTBaseObject(new Blocking(), Blocking.class);
        Unblocked unblocked = fillTBaseObject(new Unblocked(), Unblocked.class);
        blocking.setUnblocked(unblocked);
        shop.setBlocking(blocking);

        ShopAccount account = fillTBaseObject(new ShopAccount(), ShopAccount.class);
        account.setCurrency(fillTBaseObject(new dev.vality.damsel.domain.CurrencyRef(),
                dev.vality.damsel.domain.CurrencyRef.class));
        shop.setAccount(account);
        return shop;
    }
}
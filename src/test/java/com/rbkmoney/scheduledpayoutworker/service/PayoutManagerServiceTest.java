package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.payout.manager.Payout;
import com.rbkmoney.payout.manager.PayoutManagementSrv;
import com.rbkmoney.payout.manager.PayoutParams;
import com.rbkmoney.payout.manager.ShopParams;
import com.rbkmoney.scheduledpayoutworker.dao.*;
import com.rbkmoney.scheduledpayoutworker.service.impl.PayoutManagerServiceImpl;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static com.rbkmoney.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
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
    private ArgumentCaptor<String> payoutIdCaptor;

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

        when(partyManagementService.getShop(partyId, shopId)).thenReturn(shop);
        when(paymentDao.includeUnpaid(payoutIdCaptor.capture(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime)))
                .thenReturn(1);
        when(refundDao.includeUnpaid(notNull(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime))).thenReturn(0);
        when(adjustmentDao.includeUnpaid(notNull(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime))).thenReturn(0);
        when(chargebackDao.includeUnpaid(notNull(), eq(partyId), eq(shopId), eq(fromTime), eq(toTime))).thenReturn(0);
        when(payoutDao.getAvailableAmount(notNull())).thenReturn(amount);

        CurrencyRef currency = shop.getAccount().getCurrency();
        Cash cash = new Cash().setAmount(amount).setCurrency(currency);
        ShopParams shopParams = new ShopParams().setPartyId(partyId).setShopId(shopId);
        PayoutParams payoutParams = new PayoutParams(shopParams, cash);

        Payout payout = new Payout();
        String payoutId = generateRandomStringId();
        payout.setPayoutId(payoutId);

        when(payoutManagerClient.createPayout(payoutParams)).thenReturn(payout);

        assertEquals(payoutId, service.createPayoutByRange(partyId, shopId, toTime));
        String tempPayoutId = payoutIdCaptor.getValue();

        verify(partyManagementService, times(1)).getShop(partyId, shopId);
        verify(paymentDao, times(1)).includeUnpaid(tempPayoutId, partyId, shopId, fromTime, toTime);
        verify(refundDao, times(1)).includeUnpaid(tempPayoutId, partyId, shopId, fromTime, toTime);
        verify(adjustmentDao, times(1)).includeUnpaid(tempPayoutId, partyId, shopId, fromTime, toTime);
        verify(chargebackDao, times(1)).includeUnpaid(tempPayoutId, partyId, shopId, fromTime, toTime);
        verify(payoutDao, times(1)).getAvailableAmount(tempPayoutId);
        verify(payoutManagerClient, times(1)).createPayout(payoutParams);
        verify(paymentDao, times(1)).updatePayoutId(tempPayoutId, payoutId);
        verify(refundDao, times(1)).updatePayoutId(tempPayoutId, payoutId);
        verify(adjustmentDao, times(1)).updatePayoutId(tempPayoutId, payoutId);
        verify(chargebackDao, times(1)).updatePayoutId(tempPayoutId, payoutId);

    }

    private Shop prepareShop(String shopId) {
        Shop shop = fillTBaseObject(new Shop(), Shop.class);
        shop.setId(shopId);

        Blocking blocking = fillTBaseObject(new Blocking(), Blocking.class);
        Unblocked unblocked = fillTBaseObject(new Unblocked(), Unblocked.class);
        blocking.setUnblocked(unblocked);
        shop.setBlocking(blocking);

        ShopAccount account = fillTBaseObject(new ShopAccount(), ShopAccount.class);
        account.setCurrency(fillTBaseObject(new CurrencyRef(), CurrencyRef.class));
        shop.setAccount(account);
        return shop;
    }
}
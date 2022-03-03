package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.*;
import dev.vality.payout.manager.Payout;
import dev.vality.payout.manager.PayoutManagementSrv;
import dev.vality.payout.manager.PayoutParams;
import dev.vality.payout.manager.ShopParams;
import dev.vality.payouter.domain.tables.pojos.ShopMeta;
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

import static dev.vality.scheduledpayoutworker.util.TestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PayoutManagerServiceTest {

    @Mock
    private PayoutManagementSrv.Iface payoutManagerClient;
    @Mock
    private PartyManagementService partyManagementService;
    @Mock
    private ShumwayService shumwayService;
    @Mock
    private ShopMetaDao shopMetaDao;
    @Captor
    private ArgumentCaptor<PayoutParams> payoutParamsCaptor;

    private PayoutManagerService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {payoutManagerClient, partyManagementService, shumwayService, shopMetaDao};
        service =
                new PayoutManagerServiceImpl(payoutManagerClient, partyManagementService, shumwayService, shopMetaDao);
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
        Shop shop = prepareShop(shopId);
        String payoutId = UUID.randomUUID().toString();
        long amount = generateRandomIntId();

        when(partyManagementService.getShop(partyId, shopId)).thenReturn(shop);

        Payout payout =  fillTBaseObject(new Payout(), Payout.class);
        payout.setPayoutId(payoutId);
        LocalDateTime toTime = LocalDateTime.now();
        when(shumwayService.getAccountBalance(Long.parseLong(shopId), toTime)).thenReturn(amount);
        var shopMeta = new ShopMeta();
        shopMeta.setShopId(shopId);
        shopMeta.setPartyId(partyId);
        when(shopMetaDao.get(partyId, shopId)).thenReturn(shopMeta);
        when(payoutManagerClient.createPayout(payoutParamsCaptor.capture())).thenReturn(payout);

        String actualPayoutId = service.createPayoutByRange(partyId, shopId, toTime);
        assertEquals(payoutId, actualPayoutId);

        PayoutParams payoutParams = payoutParamsCaptor.getValue();
        String symbolicCode = shop.getAccount().getCurrency().getSymbolicCode();
        CurrencyRef currency = new CurrencyRef().setSymbolicCode(symbolicCode);
        Cash cash = new Cash().setAmount(amount).setCurrency(currency);
        assertEquals(cash, payoutParams.getCash());

        ShopParams shopParams = payoutParams.getShopParams();
        assertEquals(partyId, shopParams.getPartyId());
        assertEquals(shopId, shopParams.getShopId());

        verify(partyManagementService, times(1)).getShop(partyId, shopId);
        verify(payoutManagerClient, times(1)).createPayout(payoutParams);
        verify(shumwayService, times(1)).getAccountBalance(Long.parseLong(shopId), toTime);
        verify(shopMetaDao, times(1)).get(partyId, shopId);
        verify(shopMetaDao, times(1)).update(eq(partyId), eq(shopId), notNull());
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
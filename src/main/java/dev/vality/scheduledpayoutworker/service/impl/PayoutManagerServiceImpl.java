package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.CurrencyRef;
import dev.vality.damsel.domain.Shop;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.payout.manager.PayoutManagementSrv;
import dev.vality.payout.manager.PayoutParams;
import dev.vality.payout.manager.ShopParams;
import dev.vality.scheduledpayoutworker.dao.*;
import dev.vality.scheduledpayoutworker.exception.InvalidStateException;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.service.PartyManagementService;
import dev.vality.scheduledpayoutworker.service.PayoutManagerService;
import dev.vality.scheduledpayoutworker.service.ShumwayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutManagerServiceImpl implements PayoutManagerService {

    private final PayoutManagementSrv.Iface payoutManagerClient;
    private final PartyManagementService partyManagementService;
    private final ShumwayService shumwayService;
    private final ShopMetaDao shopMetaDao;

    /**
     * Create payout by partyId, shopId and toTime. fromTime = toTime - MAX_DAYS
     *
     * @return payoutId if payout successfully created, null if payout amount = 0
     */
    @Override
    @Transactional
    public String createPayoutByRange(String partyId, String shopId, LocalDateTime toTime)
            throws NotFoundException, TException {

        Shop shop = partyManagementService.getShop(partyId, shopId);

        if (shop.getBlocking().isSetBlocked()) {
            throw new InvalidStateException(
                    String.format("Party or shop blocked for payouts, partyId='%s', shopId='%s'", partyId, shopId));
        }

        LocalDateTime fromTime = shopMetaDao.get(partyId, shopId).getLastPayoutCreatedAt();
        long amount = shumwayService.getAccountBalance(Long.parseLong(shopId), fromTime, toTime);
        if (amount == 0) {
            return null;
        }

        String symbolicCode = shop.getAccount().getCurrency().getSymbolicCode();
        CurrencyRef currency = new CurrencyRef().setSymbolicCode(symbolicCode);
        Cash cash = new Cash().setAmount(amount).setCurrency(currency);
        ShopParams shopParams = new ShopParams().setPartyId(partyId).setShopId(shopId);
        PayoutParams payoutParams = new PayoutParams(shopParams, cash);

        var payout = payoutManagerClient.createPayout(payoutParams);
        shopMetaDao.update(partyId, shopId, TypeUtil.stringToLocalDateTime(payout.getCreatedAt()));
        return payout.getPayoutId();
    }

}

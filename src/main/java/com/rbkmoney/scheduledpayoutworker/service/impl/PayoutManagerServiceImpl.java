package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.payout.manager.Payout;
import com.rbkmoney.payout.manager.PayoutManagementSrv;
import com.rbkmoney.payout.manager.PayoutParams;
import com.rbkmoney.payout.manager.ShopParams;
import com.rbkmoney.scheduledpayoutworker.dao.*;
import com.rbkmoney.scheduledpayoutworker.exception.DaoException;
import com.rbkmoney.scheduledpayoutworker.exception.InvalidStateException;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.exception.StorageException;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementService;
import com.rbkmoney.scheduledpayoutworker.service.PayoutManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutManagerServiceImpl implements PayoutManagerService {

    private final PaymentDao paymentDao;
    private final RefundDao refundDao;
    private final AdjustmentDao adjustmentDao;
    private final ChargebackDao chargebackDao;
    private final PayoutDao payoutDao;

    private final PayoutManagementSrv.Iface payoutManagerClient;
    private final PartyManagementService partyManagementService;

    @Override
    public String createPayoutByRange(String partyId, String shopId, LocalDateTime toTime)
            throws NotFoundException, StorageException, TException {

        Shop shop = partyManagementService.getShop(partyId, shopId);

        if (shop.getBlocking().isSetBlocked()) {
            throw new InvalidStateException(
                    String.format("Party or shop blocked for payouts, partyId='%s', shopId='%s'", partyId, shopId)
            );
        }

        //Temporary payoutId, before the final one from payoutManager is received
        String tempPayoutId = UUID.randomUUID().toString();
        LocalDateTime fromTime = toTime.minusDays(7);
        includeUnpaid(tempPayoutId, partyId, shopId, fromTime, toTime);

        long amount = calculateAvailableAmount(tempPayoutId);

        CurrencyRef currency = shop.getAccount().getCurrency();
        Cash cash = new Cash().setAmount(amount).setCurrency(currency);
        ShopParams shopParams = new ShopParams().setPartyId(partyId).setShopId(shopId);
        PayoutParams payoutParams = new PayoutParams(shopParams, cash);

        Payout payout = payoutManagerClient.createPayout(payoutParams);
        updatePayoutId(tempPayoutId, payout.getPayoutId());
        return payout.getPayoutId();
    }

    private void includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime fromTime,
                               LocalDateTime toTime)
            throws StorageException {
        log.info("Trying to include operations in payout," +
                        " payoutId='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                payoutId, partyId, shopId, fromTime, toTime);
        try {
            int paymentCount = paymentDao.includeUnpaid(payoutId, partyId, shopId, fromTime, toTime);
            int refundCount = refundDao.includeUnpaid(payoutId, partyId, shopId, fromTime, toTime);
            int adjustmentCount = adjustmentDao.includeUnpaid(payoutId, partyId, shopId, fromTime, toTime);
            int chargebackCount = chargebackDao.includeUnpaid(payoutId, partyId, shopId, fromTime, toTime);
            log.info("Operations have been included in payout, payoutId='{}' (paymentCount='{}', refundCount='{}', " +
                            "adjustmentCount='{}', chargebackCount='{}')",
                    payoutId, paymentCount, refundCount, adjustmentCount, chargebackCount);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to include operations in payout, payoutId='%s'", payoutId), ex);
        }
    }

    private long calculateAvailableAmount(String payoutId) {
        try {
            long amount = payoutDao.getAvailableAmount(payoutId);
            log.info("Available amount have been calculated, payoutId='{}', availableAmount={}", payoutId, amount);
            return amount;
        } catch (DaoException ex) {
            throw new StorageException(ex);
        }
    }

    private void updatePayoutId(String oldPayoutId, String newPayoutId) {
        log.info("Trying to update payoutId from '{}' to '{}'", oldPayoutId, newPayoutId);
        try {
            paymentDao.updatePayoutId(oldPayoutId, newPayoutId);
            refundDao.updatePayoutId(oldPayoutId, newPayoutId);
            adjustmentDao.updatePayoutId(oldPayoutId, newPayoutId);
            chargebackDao.updatePayoutId(oldPayoutId, newPayoutId);
            log.info("Successfully updated payoutId from '{}' to '{}'", oldPayoutId, newPayoutId);
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to update payoutId from '%s' to '%s'", oldPayoutId, newPayoutId), ex);
        }
    }

}

package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.payout.manager.PayoutManagementSrv;
import com.rbkmoney.payout.manager.PayoutParams;
import com.rbkmoney.payout.manager.ShopParams;
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

    private final PayoutManagementSrv.Iface payoutManagerClient;
    private final PartyManagementService partyManagementService;

    @Override
    public String createPayoutByRange(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime)
            throws NotFoundException, StorageException {

        Shop shop = partyManagementService.getShop(partyId, shopId);
        CurrencyRef currency = shop.getAccount().getCurrency();
        String payoutId = UUID.randomUUID().toString();
        long amount = calculateAvailableAmount(payoutId);

        Cash cash = new Cash().setAmount(amount).setCurrency(currency);
        ShopParams shopParams = new ShopParams().setPartyId(partyId).setShopId(shopId);
        PayoutParams payoutParams = new PayoutParams(shopParams, cash);

        try {
            payoutManagerClient.createPayout(payoutParams);
        } catch (TException e) {
            e.printStackTrace();
        }

        return payoutId;
    }

    private long calculateAvailableAmount(String payoutId) {

    }

}

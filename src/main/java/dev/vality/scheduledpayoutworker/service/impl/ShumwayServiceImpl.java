package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.accounter.AccountNotFound;
import dev.vality.damsel.accounter.AccounterSrv;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.service.ShumwayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShumwayServiceImpl implements ShumwayService {

    private final AccounterSrv.Iface shumwayClient;

    @Override
    public long getAccountBalanceDiff(long accountId, LocalDateTime fromTime, LocalDateTime toTime) {
        log.info("Trying to get account balance diff, accountId='{}', fromTime='{}', toTime='{}'",
                accountId, fromTime, toTime);
        long balance;
        try {
            var from = TypeUtil.temporalToString(fromTime);
            var to = TypeUtil.temporalToString(toTime);
            balance = shumwayClient.getAccountBalanceDiff(accountId, from, to);
        } catch (AccountNotFound e) {
            throw new NotFoundException(String.format("Failed to find account, accountId = '%d'", accountId));
        } catch (TException e) {
            throw new RuntimeException(
                    String.format("Failed to get account balance diff, accountId='%d', fromTime='%s', toTime='%s'",
                            accountId, fromTime, toTime), e);
        }
        log.info("Account balance diff has been found, accountId='{}', fromTime='{}', toTime='{}'",
                accountId, fromTime, toTime);
        return balance;
    }

    @Override
    public long getAccountBalance(long accountId, LocalDateTime dateTime) {
        log.info("Trying to get account balance, accountId='{}', dateTime='{}'", accountId, dateTime);
        long balance;
        try {
            var toTime = TypeUtil.temporalToString(dateTime);
            balance = shumwayClient.getAccountBalance(accountId, toTime);
        } catch (AccountNotFound e) {
            throw new NotFoundException(String.format("Failed to find account, accountId = '%d'", accountId));
        } catch (TException e) {
            throw new RuntimeException(
                    String.format("Failed to get account balance, accountId='%d', dateTime='%s'",
                            accountId, dateTime), e);
        }
        log.info("Account balance has been found, accountId='{}', dateTime='{}'",
                accountId, dateTime);
        return balance;
    }
}

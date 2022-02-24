package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.accounter.AccountNotFound;
import dev.vality.damsel.accounter.AccounterSrv;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.service.ShumwayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShumwayServiceImpl implements ShumwayService {

    private final AccounterSrv.Iface shumwayClient;

    @Override
    public long getAccountBalance(long accountId, @Nullable LocalDateTime fromTime, LocalDateTime toTime) {
        log.info("Trying to get account balance, accountId='{}', fromTime='{}', toTime='{}'",
                accountId, fromTime, toTime);
        long balance;
        try {
            var from = fromTime == null ? null : TypeUtil.temporalToString(fromTime);
            var to = TypeUtil.temporalToString(toTime);
            balance = shumwayClient.getAccountBalance(accountId, from, to);
        } catch (AccountNotFound e) {
            throw new NotFoundException(String.format("Failed to find account, accountId = '%d'", accountId));
        } catch (TException e) {
            throw new RuntimeException(
                    String.format("Failed to get account balance, accountId='%d', fromTime='%s', toTime='%s'",
                            accountId, fromTime, toTime), e);
        }
        log.info("Account balance has been found, accountId='{}', fromTime='{}', toTime='{}'",
                accountId, fromTime, toTime);
        return balance;
    }
}

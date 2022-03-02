package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.accounter.AccountNotFound;
import dev.vality.damsel.accounter.AccounterSrv;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.service.impl.ShumwayServiceImpl;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static dev.vality.scheduledpayoutworker.util.TestUtil.generateRandomIntId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ShumwayServiceTest {

    @Mock
    private AccounterSrv.Iface shumwayClient;

    private ShumwayService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {shumwayClient};
        service =
                new ShumwayServiceImpl(shumwayClient);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void getAccountBalanceDiffTest() throws TException {
        int accountId = generateRandomIntId();
        var toTime = LocalDateTime.now();
        var fromTime = toTime.minusDays(7);
        long amount = generateRandomIntId();
        when(shumwayClient.getAccountBalanceDiff(accountId,
                TypeUtil.temporalToString(fromTime),
                TypeUtil.temporalToString(toTime))).thenReturn(amount);
        long actualBalance = service.getAccountBalanceDiff(accountId, fromTime, toTime);
        assertEquals(amount, actualBalance);
        verify(shumwayClient, times(1)).getAccountBalanceDiff(accountId,
                TypeUtil.temporalToString(fromTime),
                TypeUtil.temporalToString(toTime));
    }

    @Test
    void getAccountBalanceTest() throws TException {
        int accountId = generateRandomIntId();
        var toTime = LocalDateTime.now();
        long amount = generateRandomIntId();
        when(shumwayClient.getAccountBalance(accountId,
                TypeUtil.temporalToString(toTime))).thenReturn(amount);
        long actualBalance = service.getAccountBalance(accountId, toTime);
        assertEquals(amount, actualBalance);
        verify(shumwayClient, times(1)).getAccountBalance(accountId,
                TypeUtil.temporalToString(toTime));
    }

    @Test
    void getAccountBalanceDiffNotFoundTest() throws TException {
        int accountId = generateRandomIntId();
        var toTime = LocalDateTime.now();
        var fromTime = toTime.minusDays(7);
        when(shumwayClient.getAccountBalanceDiff(accountId,
                TypeUtil.temporalToString(fromTime),
                TypeUtil.temporalToString(toTime))).thenThrow(new AccountNotFound(accountId));

        assertThrows(NotFoundException.class, () -> service.getAccountBalanceDiff(accountId, fromTime, toTime));
        verify(shumwayClient, times(1)).getAccountBalanceDiff(accountId,
                TypeUtil.temporalToString(fromTime),
                TypeUtil.temporalToString(toTime));

    }
}

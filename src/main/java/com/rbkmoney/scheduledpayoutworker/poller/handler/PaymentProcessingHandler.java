package com.rbkmoney.scheduledpayoutworker.poller.handler;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;

public interface PaymentProcessingHandler extends Handler<InvoiceChange, MachineEvent> {
}

package dev.vality.scheduledpayoutworker.poller.handler;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.machinegun.eventsink.MachineEvent;

public interface PaymentProcessingHandler extends Handler<InvoiceChange, MachineEvent> {
}

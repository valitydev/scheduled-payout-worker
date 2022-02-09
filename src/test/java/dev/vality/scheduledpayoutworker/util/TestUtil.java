package dev.vality.scheduledpayoutworker.util;

import dev.vality.damsel.domain.Invoice;
import dev.vality.damsel.domain.InvoicePayment;
import dev.vality.damsel.domain.*;
import dev.vality.damsel.domain_config.VersionedObject;
import dev.vality.damsel.payment_processing.*;
import dev.vality.geck.serializer.kit.mock.FieldHandler;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.machinegun.msgpack.Value;
import lombok.SneakyThrows;
import org.apache.thrift.TBase;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtil {

    private static final ThreadLocal<TSerializer> serializerLocal =
            ThreadLocal.withInitial(() -> {
                try {
                    return new TSerializer(new TBinaryProtocol.Factory());
                } catch (TTransportException e) {
                    throw new RuntimeException(e);
                }
            });
    private static final MockTBaseProcessor mockTBaseProcessor;

    static {
        mockTBaseProcessor = new MockTBaseProcessor(MockMode.REQUIRED_ONLY, 15, 1);
        Map.Entry<FieldHandler, String[]> timeFields = Map.entry(
                structHandler -> structHandler.value(Instant.now().toString()),
                new String[] {"created_at", "at", "due"}
        );
        mockTBaseProcessor.addFieldHandler(timeFields.getKey(), timeFields.getValue());
    }

    public static Party createParty(String partyId, String shopId, int paymentInstitutionId) {
        Party party = fillTBaseObject(new Party(), Party.class);
        party.setId(partyId);

        String contractId = generateRandomStringId();
        Contract contract = fillTBaseObject(new Contract(), Contract.class);
        contract.setId(contractId);

        PayoutTool payoutTool = fillTBaseObject(new PayoutTool(), PayoutTool.class);
        payoutTool.setId(generateRandomStringId());

        PayoutToolInfo payoutToolInfo = fillTBaseObject(new PayoutToolInfo(), PayoutToolInfo.class);
        PaymentInstitutionAccount paymentInstitutionAccount =
                fillTBaseObject(new PaymentInstitutionAccount(), PaymentInstitutionAccount.class);
        payoutToolInfo.setPaymentInstitutionAccount(paymentInstitutionAccount);
        payoutTool.setPayoutToolInfo(payoutToolInfo);

        contract.setPayoutTools(List.of(payoutTool));
        PaymentInstitutionRef institutionRef =
                fillTBaseObject(new PaymentInstitutionRef(), PaymentInstitutionRef.class);
        institutionRef.setId(paymentInstitutionId);
        contract.setPaymentInstitution(institutionRef);

        Shop shop = fillTBaseObject(new Shop(), Shop.class);
        shop.setContractId(contractId);
        shop.setPayoutToolId(payoutTool.getId());

        Blocking blocking = fillTBaseObject(new Blocking(), Blocking.class);
        Unblocked unblocked = fillTBaseObject(new Unblocked(), Unblocked.class);
        blocking.setUnblocked(unblocked);
        shop.setBlocking(blocking);

        ShopAccount account = fillTBaseObject(new ShopAccount(), ShopAccount.class);
        account.setCurrency(fillTBaseObject(new CurrencyRef(), CurrencyRef.class));
        shop.setAccount(account);


        party.setShops(Map.of(shopId, shop));
        party.setContracts(Map.of(contractId, contract));
        return party;
    }

    @SneakyThrows
    public static SinkEvent shopCreatedEvent(String partyId, String shopId) {

        ShopEffect shopEffect = fillTBaseObject(new ShopEffect(), ShopEffect.class);
        Shop shop = fillTBaseObject(new Shop(), Shop.class);
        shop.setId(shopId);
        BusinessScheduleRef scheduleRef = fillTBaseObject(new BusinessScheduleRef(), BusinessScheduleRef.class);
        shop.setPayoutSchedule(scheduleRef);
        shopEffect.setCreated(shop);
        MachineEvent machineEvent = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        machineEvent.setSourceId(partyId);
        PartyEventData partyEventData = fillTBaseObject(new PartyEventData(), PartyEventData.class);
        partyEventData.addToChanges(
                PartyChange.claim_created(new Claim().setStatus(ClaimStatus.accepted(new ClaimAccepted().setEffects(
                        List.of(ClaimEffect.shop_effect(new ShopEffectUnit().setShopId(shopId).setEffect(shopEffect)))
                ))).setChangeset(List.of())
                        .setCreatedAt("")));
        machineEvent.setData(Value.bin(serializerLocal.get().serialize(partyEventData)));
        SinkEvent sinkEvent = fillTBaseObject(new SinkEvent(), SinkEvent.class);
        sinkEvent.setEvent(machineEvent);
        return sinkEvent;
    }

    @SneakyThrows
    public static SinkEvent invoiceCreatedEvent(String partyId, String shopId, String invoiceId) {
        Invoice invoice = fillTBaseObject(new Invoice(), Invoice.class);
        invoice.setId(invoiceId);
        invoice.setOwnerId(partyId);
        invoice.setShopId(shopId);

        InvoiceCreated invoiceCreated = fillTBaseObject(new InvoiceCreated(), InvoiceCreated.class);
        InvoiceChange invoiceChange = fillTBaseObject(new InvoiceChange(), InvoiceChange.class);
        invoiceChange.setInvoiceCreated(invoiceCreated.setInvoice(invoice));

        EventPayload eventPayload = fillTBaseObject(new EventPayload(), EventPayload.class);
        eventPayload.setInvoiceChanges(List.of(invoiceChange));

        MachineEvent machineEvent = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        machineEvent.setData(Value.bin(serializerLocal.get().serialize(eventPayload)));

        SinkEvent sinkEvent = fillTBaseObject(new SinkEvent(), SinkEvent.class);
        sinkEvent.setEvent(machineEvent);
        return sinkEvent;
    }

    @SneakyThrows
    public static SinkEvent paymentCreatedEvent(String paymentId, String invoiceId, String createdAt, long amount) {

        InvoiceChange invoiceChange = fillTBaseObject(new InvoiceChange(), InvoiceChange.class);
        InvoicePaymentChange paymentChange = fillTBaseObject(new InvoicePaymentChange(), InvoicePaymentChange.class);

        InvoicePaymentChangePayload payload =
                fillTBaseObject(new InvoicePaymentChangePayload(), InvoicePaymentChangePayload.class);
        paymentChange.setPayload(payload);
        InvoicePaymentStarted paymentStarted =
                fillTBaseObject(new InvoicePaymentStarted(), InvoicePaymentStarted.class);
        payload.setInvoicePaymentStarted(paymentStarted);
        invoiceChange.setInvoicePaymentChange(paymentChange);
        FinalCashFlowPosting cashFlowPosting = fillTBaseObject(new FinalCashFlowPosting(), FinalCashFlowPosting.class);
        paymentStarted.setCashFlow(List.of(cashFlowPosting));

        InvoicePayment invoicePayment = fillTBaseObject(new InvoicePayment(), InvoicePayment.class);
        invoicePayment.setId(paymentId);
        paymentStarted.setPayment(invoicePayment);
        invoicePayment.setCreatedAt(createdAt);
        Cash cash = fillTBaseObject(new Cash(), Cash.class);
        invoicePayment.setCost(cash);
        cash.setAmount(amount);

        EventPayload eventPayload = fillTBaseObject(new EventPayload(), EventPayload.class);
        eventPayload.setInvoiceChanges(List.of(invoiceChange));

        MachineEvent machineEvent = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        machineEvent.setSourceId(invoiceId);
        machineEvent.setData(Value.bin(serializerLocal.get().serialize(eventPayload)));

        SinkEvent sinkEvent = fillTBaseObject(new SinkEvent(), SinkEvent.class);
        sinkEvent.setEvent(machineEvent);
        return sinkEvent;
    }

    @SneakyThrows
    public static SinkEvent paymentCapturedEvent(String invoiceId, String paymentId, String createdAt) {
        InvoiceChange invoiceChange = fillTBaseObject(new InvoiceChange(), InvoiceChange.class);
        InvoicePaymentChange invoicePaymentChange =
                fillTBaseObject(new InvoicePaymentChange(), InvoicePaymentChange.class);
        invoicePaymentChange.setId(paymentId);
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);
        InvoicePaymentChangePayload
                invoicePaymentChangePayload =
                fillTBaseObject(new InvoicePaymentChangePayload(), InvoicePaymentChangePayload.class);
        invoicePaymentChange.setPayload(invoicePaymentChangePayload);
        InvoicePaymentStatusChanged statusChanged =
                fillTBaseObject(new InvoicePaymentStatusChanged(), InvoicePaymentStatusChanged.class);
        invoicePaymentChangePayload.setInvoicePaymentStatusChanged(statusChanged);
        InvoicePaymentStatus
                invoicePaymentStatus =
                fillTBaseObject(new InvoicePaymentStatus(), InvoicePaymentStatus.class);
        statusChanged.setStatus(invoicePaymentStatus);
        InvoicePaymentCaptured captured =
                fillTBaseObject(new InvoicePaymentCaptured(), InvoicePaymentCaptured.class);
        invoicePaymentStatus.setCaptured(captured);

        EventPayload eventPayload = fillTBaseObject(new EventPayload(), EventPayload.class);
        eventPayload.setInvoiceChanges(List.of(invoiceChange));

        MachineEvent machineEvent = fillTBaseObject(new MachineEvent(), MachineEvent.class);
        machineEvent.setSourceId(invoiceId);
        machineEvent.setData(Value.bin(serializerLocal.get().serialize(eventPayload)));
        machineEvent.setCreatedAt(createdAt);

        SinkEvent sinkEvent = fillTBaseObject(new SinkEvent(), SinkEvent.class);
        sinkEvent.setEvent(machineEvent);
        return sinkEvent;
    }

    public static VersionedObject createVersionedObject() {
        DomainObject domainObject = fillTBaseObject(new DomainObject(), DomainObject.class);
        PaymentInstitutionObject paymentInstitutionObject =
                fillTBaseObject(new PaymentInstitutionObject(), PaymentInstitutionObject.class);
        PaymentInstitution paymentInstitution = fillTBaseObject(new PaymentInstitution(), PaymentInstitution.class);
        CalendarRef calendar = fillTBaseObject(new CalendarRef(), CalendarRef.class);
        paymentInstitution.setCalendar(calendar);
        paymentInstitutionObject.setData(paymentInstitution);
        domainObject.setPaymentInstitution(paymentInstitutionObject);

        VersionedObject versionedObject = fillTBaseObject(new VersionedObject(), VersionedObject.class);
        versionedObject.setObject(domainObject);
        return versionedObject;
    }

    public static String generateRandomStringId() {
        return String.valueOf(generateRandomIntId());
    }

    public static Integer generateRandomIntId() {
        return ThreadLocalRandom.current().nextInt(1, 10001);
    }


    @SneakyThrows
    public static <T extends TBase> T fillTBaseObject(T tbase, Class<T> type) {
        return mockTBaseProcessor.process(tbase, new TBaseHandler<>(type));
    }

}

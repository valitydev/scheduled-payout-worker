package com.rbkmoney.scheduledpayoutworker.integration.data;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.serializer.kit.mock.FieldHandler;
import com.rbkmoney.geck.serializer.kit.mock.MockMode;
import com.rbkmoney.geck.serializer.kit.mock.MockTBaseProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.machinegun.msgpack.Value;
import lombok.SneakyThrows;
import org.apache.thrift.TBase;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TestData {

    private static MockTBaseProcessor mockTBaseProcessor;

    static {
        mockTBaseProcessor = new MockTBaseProcessor(MockMode.REQUIRED_ONLY, 15, 1);
        Map.Entry<FieldHandler, String[]> timeFields = Map.entry(
                structHandler -> structHandler.value(Instant.now().toString()),
                new String[] {"created_at", "at", "due"}
        );
        mockTBaseProcessor.addFieldHandler(timeFields.getKey(), timeFields.getValue());
    }

    private static final ThreadLocal<TSerializer> serializerLocal =
            ThreadLocal.withInitial(() -> new TSerializer(new TBinaryProtocol.Factory()));

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
    public static SinkEvent invoiceCreatedEvent(String partyId, String shopId) {
        Invoice invoice = fillTBaseObject(new Invoice(), Invoice.class);
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

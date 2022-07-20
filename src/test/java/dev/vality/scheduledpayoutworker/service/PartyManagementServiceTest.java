package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.Contract;
import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain.PaymentInstitutionRef;
import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.scheduledpayoutworker.service.impl.PartyManagementServiceImpl;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.StringUtils;

import java.util.Map;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static dev.vality.scheduledpayoutworker.util.TestUtil.generateRandomStringId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PartyManagementServiceTest {

    @Mock
    private PartyManagementSrv.Iface partyManagementClient;

    private PartyManagementService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {partyManagementClient};
        service = new PartyManagementServiceImpl(partyManagementClient);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void getParty() throws TException {
        String partyId = generateRandomStringId();
        Party party = prepareParty(partyId, null, null);
        when(partyManagementClient.get(eq(partyId))).thenReturn(party);
        Party actualParty = service.getParty(partyId);
        assertEquals(party, actualParty);
        verify(partyManagementClient, times(1)).get(eq(partyId));
    }

    @Test
    void getShop() throws TException {
        String partyId = generateRandomStringId();
        String shopId = generateRandomStringId();
        Party party = prepareParty(partyId, shopId, null);
        Shop shop = party.getShops().get(shopId);

        when(partyManagementClient.get(eq(partyId))).thenReturn(party);
        Shop actualShop = service.getShop(partyId, shopId);
        assertEquals(shop, actualShop);
        verify(partyManagementClient, times(1)).get(eq(partyId));
    }

    @Test
    void getPaymentInstitutionRef() throws TException {
        String partyId = generateRandomStringId();
        String contractId = generateRandomStringId();
        Party party = prepareParty(partyId, null, contractId);
        PaymentInstitutionRef paymentInstitutionRef = party.getContracts().get(contractId).getPaymentInstitution();
        when(partyManagementClient.get(eq(partyId))).thenReturn(party);
        PaymentInstitutionRef actualPaymentInstitutionRef = service.getPaymentInstitutionRef(partyId, contractId);
        assertEquals(paymentInstitutionRef, actualPaymentInstitutionRef);
        verify(partyManagementClient, times(1)).get(eq(partyId));
    }

    private Party prepareParty(String partyId, String shopId, String contractId) {
        Party party = fillTBaseObject(new Party(), Party.class);
        party.setId(partyId);

        if (StringUtils.hasLength(shopId)) {
            Shop shop = fillTBaseObject(new Shop(), Shop.class);
            shop.setId(shopId);
            party.setShops(Map.of(shopId, shop));
        }

        if (StringUtils.hasLength(contractId)) {
            Contract contract = fillTBaseObject(new Contract(), Contract.class);
            contract.setId(contractId);
            party.setContracts(Map.of(contractId, contract));
            var paymentInstitutionRef = fillTBaseObject(new PaymentInstitutionRef(), PaymentInstitutionRef.class);
            contract.setPaymentInstitution(paymentInstitutionRef);
        }

        return party;
    }

}
package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.domain.Contract;
import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain.PaymentInstitutionRef;
import dev.vality.damsel.domain.Shop;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.damsel.payment_processing.PartyNotFound;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.service.PartyManagementService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartyManagementServiceImpl implements PartyManagementService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PartyManagementSrv.Iface partyManagementClient;

    @Autowired
    public PartyManagementServiceImpl(
            PartyManagementSrv.Iface partyManagementClient
    ) {
        this.partyManagementClient = partyManagementClient;
    }

    public Party getParty(String partyId) throws NotFoundException {
        log.info("Trying to get party, partyId='{}'", partyId);
        try {
            Party party = partyManagementClient.get(partyId);
            log.info("Party has been found, partyId='{}'", partyId);
            return party;
        } catch (PartyNotFound ex) {
            throw new NotFoundException(
                    String.format("Party not found, partyId='%s'",
                            partyId), ex);
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get party, partyId='%s'",
                            partyId), ex);
        }
    }

    @Override
    public Shop getShop(String partyId, String shopId) throws NotFoundException {

        Party party = getParty(partyId);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', " +
                    "shopId='%s'", partyId, shopId));
        }
        log.info("Shop has been found, partyId='{}', shopId='{}'",
                partyId, shopId);
        return shop;
    }

    private Contract getContract(String partyId, String contractId)
            throws NotFoundException {
        log.info("Trying to get contract, partyId='{}', contractId='{}'",
                partyId, contractId);
        Party party = getParty(partyId);

        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new NotFoundException(
                    String.format("Shop not found, partyId='%s', contractId='%s'", partyId, contractId));
        }
        log.info("Contract has been found, partyId='{}', contractId='{}'",
                partyId, contractId);
        return contract;
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId)
            throws NotFoundException {
        log.debug("Trying to get paymentInstitutionRef, partyId='{}', contractId='{}'",
                partyId, contractId);
        Contract contract = getContract(partyId, contractId);

        if (!contract.isSetPaymentInstitution()) {
            throw new NotFoundException(String.format("PaymentInstitutionRef not found, partyId='%s', " +
                    "contractId='%s'", partyId, contractId));
        }

        PaymentInstitutionRef paymentInstitutionRef = contract.getPaymentInstitution();
        log.info("PaymentInstitutionRef has been found, partyId='{}', contractId='{}', paymentInstitutionRef='{}'",
                partyId, contractId, paymentInstitutionRef);
        return paymentInstitutionRef;
    }

}

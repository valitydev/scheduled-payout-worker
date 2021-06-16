package com.rbkmoney.scheduledpayoutworker.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.Party;
import com.rbkmoney.damsel.domain.PaymentInstitutionRef;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;
import com.rbkmoney.scheduledpayoutworker.service.PartyManagementService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;

@Service
public class PartyManagementServiceImpl implements PartyManagementService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final UserInfo userInfo = new UserInfo("admin", UserType.internal_user(new InternalUser()));

    private final PartyManagementSrv.Iface partyManagementClient;

    private final Cache<Map.Entry<String, PartyRevisionParam>, Party> partyCache;

    @Autowired
    public PartyManagementServiceImpl(
            PartyManagementSrv.Iface partyManagementClient,
            @Value("${cache.maxSize}") long cacheMaximumSize
    ) {
        this.partyManagementClient = partyManagementClient;
        this.partyCache = Caffeine.newBuilder()
                .maximumSize(cacheMaximumSize)
                .build();
    }

    @Override
    public Party getParty(String partyId) throws NotFoundException {
        PartyRevisionParam partyRevisionParam = PartyRevisionParam
                .timestamp(TypeUtil.temporalToString(Instant.now()));
        return getParty(partyId, partyRevisionParam);
    }

    private Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.info("Trying to get party, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
        Party party = partyCache.get(
                new AbstractMap.SimpleEntry<>(partyId, partyRevisionParam),
                key -> {
                    try {
                        return partyManagementClient.checkout(userInfo, partyId, partyRevisionParam);
                    } catch (PartyNotFound ex) {
                        throw new NotFoundException(
                                String.format("Party not found, partyId='%s', partyRevisionParam='%s'",
                                        partyId, partyRevisionParam), ex);
                    } catch (InvalidPartyRevision ex) {
                        throw new NotFoundException(
                                String.format("Invalid party revision, partyId='%s', partyRevisionParam='%s'",
                                        partyId, partyRevisionParam), ex);
                    } catch (TException ex) {
                        throw new RuntimeException(
                                String.format("Failed to get party, partyId='%s', partyRevisionParam='%s'",
                                        partyId, partyRevisionParam), ex);
                    }
                });
        log.info("Party has been found, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
        return party;
    }

    @Override
    public Shop getShop(String partyId, String shopId) throws NotFoundException {
        log.info("Trying to get shop, partyId='{}', shopId='{}'",
                partyId, shopId);

        PartyRevisionParam partyRevisionParam = PartyRevisionParam
                .timestamp(TypeUtil.temporalToString(Instant.now()));

        Party party = getParty(partyId, partyRevisionParam);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', " +
                    "shopId='%s', partyRevisionParam='%s'", partyId, shopId, partyRevisionParam));
        }
        log.info("Shop has been found, partyId='{}', shopId='{}', partyRevisionParam='{}'",
                partyId, shopId, partyRevisionParam);
        return shop;
    }

    @Override
    public Contract getContract(String partyId, String contractId) throws NotFoundException {
        PartyRevisionParam partyRevisionParam = PartyRevisionParam
                .timestamp(TypeUtil.temporalToString(Instant.now()));
        return getContract(partyId, contractId, partyRevisionParam);
    }

    private Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam)
            throws NotFoundException {
        log.info("Trying to get contract, partyId='{}', contractId='{}', partyRevisionParam='{}'",
                partyId, contractId, partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', contractId='%s', " +
                    "partyRevisionParam='%s'", partyId, contractId, partyRevisionParam));
        }
        log.info("Contract has been found, partyId='{}', contractId='{}', partyRevisionParam='{}'",
                partyId, contractId, partyRevisionParam);
        return contract;
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException {
        PartyRevisionParam partyRevisionParam = PartyRevisionParam
                .timestamp(TypeUtil.temporalToString(Instant.now()));
        return getPaymentInstitutionRef(partyId, contractId, partyRevisionParam);
    }

    private PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId,
                                                           PartyRevisionParam revisionParam)
            throws NotFoundException {
        log.debug("Trying to get paymentInstitutionRef, partyId='{}', contractId='{}', partyRevisionParam='{}'",
                partyId, contractId, revisionParam);
        Contract contract = getContract(partyId, contractId, revisionParam);

        if (!contract.isSetPaymentInstitution()) {
            throw new NotFoundException(String.format("PaymentInstitutionRef not found, partyId='%s', " +
                    "contractId='%s', partyRevisionParam='%s'", partyId, contractId, revisionParam));
        }

        PaymentInstitutionRef paymentInstitutionRef = contract.getPaymentInstitution();
        log.info("PaymentInstitutionRef has been found, partyId='{}', contractId='{}', paymentInstitutionRef='{}', " +
                "partyRevisionParam='{}'", partyId, contractId, paymentInstitutionRef, revisionParam);
        return paymentInstitutionRef;
    }

}

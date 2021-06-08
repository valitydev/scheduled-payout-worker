package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.Party;
import com.rbkmoney.damsel.domain.PaymentInstitutionRef;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.PartyRevisionParam;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;

import java.time.Instant;

public interface PartyManagementService {

    Party getParty(String partyId) throws NotFoundException;

    Party getParty(String partyId, Instant timestamp) throws NotFoundException;

    Party getParty(String partyId, long partyRevision) throws NotFoundException;

    Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws NotFoundException;

    Shop getShop(String partyId, String shopId) throws NotFoundException;

    Shop getShop(String partyId, String shopId, long partyRevision) throws NotFoundException;

    Shop getShop(String partyId, String shopId, Instant timestamp) throws NotFoundException;

    Shop getShop(String partyId, String shopId, PartyRevisionParam partyRevisionParam) throws NotFoundException;

    Contract getContract(String partyId, String contractId) throws NotFoundException;

    Contract getContract(String partyId, String contractId, long partyRevision) throws NotFoundException;

    Contract getContract(String partyId, String contractId, Instant timestamp) throws NotFoundException;

    Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam)
            throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, long partyRevision)
            throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, Instant timestamp)
            throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, PartyRevisionParam revisionParam)
            throws NotFoundException;

    long getPartyRevision(String partyId) throws NotFoundException;

}

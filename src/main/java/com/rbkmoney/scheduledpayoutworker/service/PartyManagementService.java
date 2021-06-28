package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.domain.Contract;
import com.rbkmoney.damsel.domain.Party;
import com.rbkmoney.damsel.domain.PaymentInstitutionRef;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;

public interface PartyManagementService {

    Party getParty(String partyId) throws NotFoundException;

    Shop getShop(String partyId, String shopId) throws NotFoundException;

    Contract getContract(String partyId, String contractId) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException;

}

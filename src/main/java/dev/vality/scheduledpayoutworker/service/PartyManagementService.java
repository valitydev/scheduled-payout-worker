package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.Party;
import dev.vality.damsel.domain.PaymentInstitutionRef;
import dev.vality.damsel.domain.Shop;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;

public interface PartyManagementService {

    Party getParty(String partyId) throws NotFoundException;

    Shop getShop(String partyId, String shopId) throws NotFoundException;

    PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException;

}

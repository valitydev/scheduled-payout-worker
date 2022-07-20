package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.BusinessSchedule;
import dev.vality.damsel.domain.BusinessScheduleRef;
import dev.vality.damsel.domain.PaymentInstitution;
import dev.vality.damsel.domain.PaymentInstitutionRef;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;

public interface DominantService {
    BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef) throws NotFoundException;

}

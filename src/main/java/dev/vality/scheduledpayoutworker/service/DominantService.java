package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.*;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;

public interface DominantService {
    BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef) throws NotFoundException;

}

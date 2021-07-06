package com.rbkmoney.scheduledpayoutworker.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.scheduledpayoutworker.exception.NotFoundException;

public interface DominantService {
    BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef) throws NotFoundException;

    PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef) throws NotFoundException;

    Calendar getCalendar(CalendarRef calendarRef) throws NotFoundException;

}

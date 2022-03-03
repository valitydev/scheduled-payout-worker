package dev.vality.scheduledpayoutworker.service.impl;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.domain_config.Reference;
import dev.vality.damsel.domain_config.*;
import dev.vality.scheduledpayoutworker.exception.NotFoundException;
import dev.vality.scheduledpayoutworker.service.DominantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DominantServiceImpl implements DominantService {

    private final RepositoryClientSrv.Iface dominantClient;

    private final RetryTemplate retryTemplate;

    @Override
    public BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef) throws NotFoundException {
        return getBusinessSchedule(scheduleRef, Reference.head(new Head()));
    }

    private BusinessSchedule getBusinessSchedule(BusinessScheduleRef scheduleRef, Reference revisionReference)
            throws NotFoundException {
        log.info("Trying to get schedule, scheduleRef='{}', revisionReference='{}'", scheduleRef, revisionReference);
        try {
            dev.vality.damsel.domain.Reference reference = new dev.vality.damsel.domain.Reference();
            reference.setBusinessSchedule(scheduleRef);
            VersionedObject versionedObject = checkoutObject(revisionReference, reference);
            BusinessSchedule schedule = versionedObject.getObject().getBusinessSchedule().getData();
            log.info("Schedule has been found, scheduleRef='{}', revisionReference='{}', schedule='{}'",
                    scheduleRef, revisionReference, schedule);
            return schedule;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, scheduleRef='%s', revisionReference='%s'",
                    scheduleRef, revisionReference), ex);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to get schedule, scheduleRef='%s', revisionReference='%s'",
                    scheduleRef, revisionReference), ex);
        }
    }

    @Override
    public PaymentInstitution getPaymentInstitution(PaymentInstitutionRef paymentInstitutionRef)
            throws NotFoundException {
        return getPaymentInstitution(paymentInstitutionRef, Reference.head(new Head()));
    }

    private PaymentInstitution getPaymentInstitution(
            PaymentInstitutionRef paymentInstitutionRef,
            Reference revisionReference) throws NotFoundException {
        log.info("Trying to get payment institution, paymentInstitutionRef='{}', revisionReference='{}'",
                paymentInstitutionRef, revisionReference);
        try {
            dev.vality.damsel.domain.Reference reference = new dev.vality.damsel.domain.Reference();
            reference.setPaymentInstitution(paymentInstitutionRef);
            VersionedObject versionedObject = checkoutObject(revisionReference, reference);
            PaymentInstitution paymentInstitution = versionedObject.getObject().getPaymentInstitution().getData();
            log.info("Payment institution has been found, PaymentInstitutionRef='{}', revisionReference='{}', " +
                    "paymentInstitution='{}'", paymentInstitutionRef, revisionReference, paymentInstitution);
            return paymentInstitution;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(
                    String.format("Version not found, paymentInstitutionRef='%s', revisionReference='%s'",
                            paymentInstitutionRef, revisionReference), ex);
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get payment institution, paymentInstitutionRef='%s', " +
                            "revisionReference='%s'", paymentInstitutionRef, revisionReference), ex);
        }
    }

    private VersionedObject checkoutObject(Reference revisionReference, dev.vality.damsel.domain.Reference reference)
            throws TException {
        return retryTemplate.execute(
                context -> dominantClient.checkoutObject(revisionReference, reference)
        );
    }
}

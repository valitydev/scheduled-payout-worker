package dev.vality.scheduledpayoutworker.service;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.domain_config.RepositoryClientSrv;
import dev.vality.damsel.domain_config.VersionedObject;
import dev.vality.scheduledpayoutworker.service.impl.DominantServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.support.RetryTemplate;

import static dev.vality.scheduledpayoutworker.util.TestUtil.fillTBaseObject;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DominantServiceTest {

    @Mock
    private RepositoryClientSrv.Iface dominantClient;

    @Mock
    private RetryTemplate retryTemplate;

    private DominantService service;

    private AutoCloseable mocks;

    private Object[] preparedMocks;

    @BeforeEach
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
        preparedMocks = new Object[] {dominantClient, retryTemplate};
        service = new DominantServiceImpl(dominantClient, retryTemplate);
    }

    @AfterEach
    public void clean() throws Exception {
        verifyNoMoreInteractions(preparedMocks);
        mocks.close();
    }

    @Test
    void getBusinessSchedule() {
        BusinessScheduleRef scheduleRef = prepareBusinessScheduleRef();
        when(retryTemplate.execute(any(), any(), any())).thenReturn(prepareScheduleVersionedObject());
        BusinessSchedule schedule = service.getBusinessSchedule(scheduleRef);
        assertNotNull(schedule);
        verify(retryTemplate, times(1)).execute(any(), any(), any());
    }

    @Test
    void getPaymentInstitution() {
        PaymentInstitutionRef institutionRef = preparePaymentInstitutionRef();
        when(retryTemplate.execute(any(), any(), any())).thenReturn(preparePaymentInstitutionVersionedObject());
        PaymentInstitution paymentInstitution = service.getPaymentInstitution(institutionRef);
        assertNotNull(paymentInstitution);
        verify(retryTemplate, times(1)).execute(any(), any(), any());
    }

    private BusinessScheduleRef prepareBusinessScheduleRef() {
        return fillTBaseObject(new BusinessScheduleRef(), BusinessScheduleRef.class);
    }

    private PaymentInstitutionRef preparePaymentInstitutionRef() {
        return fillTBaseObject(new PaymentInstitutionRef(), PaymentInstitutionRef.class);
    }

    private CalendarRef prepareCalendarRef() {
        return fillTBaseObject(new CalendarRef(), CalendarRef.class);
    }

    private VersionedObject prepareScheduleVersionedObject() {
        VersionedObject versionedObject = fillTBaseObject(new VersionedObject(), VersionedObject.class);
        DomainObject domainObject = fillTBaseObject(new DomainObject(), DomainObject.class);
        versionedObject.setObject(domainObject);
        BusinessScheduleObject scheduleObject =
                fillTBaseObject(new BusinessScheduleObject(), BusinessScheduleObject.class);
        domainObject.setBusinessSchedule(scheduleObject);
        return versionedObject;
    }

    private VersionedObject preparePaymentInstitutionVersionedObject() {
        VersionedObject versionedObject = fillTBaseObject(new VersionedObject(), VersionedObject.class);
        DomainObject domainObject = fillTBaseObject(new DomainObject(), DomainObject.class);
        versionedObject.setObject(domainObject);
        PaymentInstitutionObject paymentInstitutionObject =
                fillTBaseObject(new PaymentInstitutionObject(), PaymentInstitutionObject.class);
        domainObject.setPaymentInstitution(paymentInstitutionObject);
        return versionedObject;
    }

    private VersionedObject prepareCalendarVersionedObject() {
        VersionedObject versionedObject = fillTBaseObject(new VersionedObject(), VersionedObject.class);
        DomainObject domainObject = fillTBaseObject(new DomainObject(), DomainObject.class);
        versionedObject.setObject(domainObject);
        CalendarObject calendarObject =
                fillTBaseObject(new CalendarObject(), CalendarObject.class);
        domainObject.setCalendar(calendarObject);
        return versionedObject;
    }
}
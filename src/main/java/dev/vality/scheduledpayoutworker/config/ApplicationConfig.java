package dev.vality.scheduledpayoutworker.config;

import dev.vality.damsel.domain_config.RepositoryClientSrv;
import dev.vality.damsel.payment_processing.PartyManagementSrv;
import dev.vality.damsel.schedule.SchedulatorSrv;
import dev.vality.payout.manager.PayoutManagementSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class ApplicationConfig {

    @Bean
    public PartyManagementSrv.Iface partyManagementClient(
            @Value("${service.partyManagement.url}") Resource resource,
            @Value("${service.partyManagement.networkTimeout}") int networkTimeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(PartyManagementSrv.Iface.class);
    }

    @Bean
    public SchedulatorSrv.Iface schedulatorClient(
            @Value("${service.schedulator.url}") Resource resource,
            @Value("${service.schedulator.networkTimeout}") int networkTimeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(SchedulatorSrv.Iface.class);
    }

    @Bean
    public RepositoryClientSrv.Iface dominantClient(
            @Value("${service.dominant.url}") Resource resource,
            @Value("${service.dominant.networkTimeout}") int networkTimeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(RepositoryClientSrv.Iface.class);
    }

    @Bean
    public PayoutManagementSrv.Iface payoutManagerClient(
            @Value("${service.payoutManager.url}") Resource resource,
            @Value("${service.payoutManager.networkTimeout}") int networkTimeout
    ) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(PayoutManagementSrv.Iface.class);
    }


}

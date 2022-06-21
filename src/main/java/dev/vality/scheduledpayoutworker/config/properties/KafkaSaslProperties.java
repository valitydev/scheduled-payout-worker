package dev.vality.scheduledpayoutworker.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka.sasl")
public class KafkaSaslProperties {
    private String mechanism;
    private String jaasConfig;
    private String clientCallbackHandlerClass;
    private boolean enabled;
}

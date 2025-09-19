package JYBank.JYBank.config;

import JYBank.JYBank.support.audit.AuditEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, AuditEvent> auditProducerFactory(KafkaProperties props) {
        Map<String, Object> cfg = props.buildProducerProperties();
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 값은 spring.kafka.producer.value-serializer 에서 JsonSerializer 지정
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaTemplate<String, AuditEvent> auditKafkaTemplate(ProducerFactory<String, AuditEvent> pf) {
        return new KafkaTemplate<>(pf);
    }
}

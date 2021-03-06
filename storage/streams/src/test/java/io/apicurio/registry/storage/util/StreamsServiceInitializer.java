package io.apicurio.registry.storage.util;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.test.utils.KafkaTestContainerManager;
import io.apicurio.registry.util.ClusterInitializer;
import io.apicurio.registry.util.ServiceInitializer;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * @author Ales Justin
 */
@QuarkusTestResource(KafkaTestContainerManager.class)
public class StreamsServiceInitializer implements ServiceInitializer, ClusterInitializer {
    private static final Logger log = LoggerFactory.getLogger(StreamsServiceInitializer.class);

    private KafkaTestContainerManager manager;

    @Override
    public void beforeAll(@Observes @Initialized(ApplicationScoped.class) Object event) throws Exception {
        Properties properties = new Properties();
        properties.put(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                System.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        );
        properties.put("connections.max.idle.ms", 10000);
        properties.put("request.timeout.ms", 5000);
        try (AdminClient client = AdminClient.create(properties)) {
            ListTopicsResult topics = client.listTopics();
            Set<String> names = topics.names().get();
            log.info("Kafka is running - {} ...", names);
        }
    }

    @Inject
    KafkaStreams streams;

    @Override
    public void beforeEach() {
        while (streams.state() != KafkaStreams.State.RUNNING) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        log.debug("Streams is RUNNING ...");
    }

    @Override
    public Map<String, String> startCluster() {
        manager = new KafkaTestContainerManager();
        return manager.start();
    }

    @Override
    public void stopCluster() {
        if (manager != null) {
            manager.stop();
        }
    }
}

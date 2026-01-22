package com.mesh.realtime;

import com.mesh.TestcontainersConfiguration;
import com.mesh.message.dto.ChatMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Simulates two separate app instances subscribed to the same Redis channel - exactly
 * what happens when two real JVMs sit behind a load balancer. A single publish must
 * reach both, which is what actually makes this chat "distributed" rather than just
 * broadcasting to whichever clients happen to be on the same node as the sender.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ChatBroadcastDistributionTest {

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    RedisChannelProperties channelProperties;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publishOnOneNodeIsDeliveredToListenersOnAllNodes() throws InterruptedException {
        SimpMessagingTemplate templateOnNodeA = mock(SimpMessagingTemplate.class);
        SimpMessagingTemplate templateOnNodeB = mock(SimpMessagingTemplate.class);

        RedisMessageListenerContainer containerA = startListenerContainer(
                new ChatMessageSubscriber(templateOnNodeA, objectMapper));
        RedisMessageListenerContainer containerB = startListenerContainer(
                new ChatMessageSubscriber(templateOnNodeB, objectMapper));

        try {
            // Redis subscriptions are established asynchronously; give the containers a
            // moment to finish SUBSCRIBE-ing before we publish.
            Thread.sleep(500);

            UUID roomId = UUID.randomUUID();
            ChatMessageResponse message = new ChatMessageResponse(
                    UUID.randomUUID(), roomId, UUID.randomUUID(), "alice", "hello from node-a", Instant.now());

            ChatMessagePublisher publisher = new ChatMessagePublisher(
                    new StringRedisTemplate(redisConnectionFactory),
                    channelProperties,
                    new NodeProperties("node-a"),
                    objectMapper);

            publisher.publish(message);

            String destination = "/topic/rooms/" + roomId;
            verify(templateOnNodeA, timeout(3000)).convertAndSend(eq(destination), any(Object.class));
            verify(templateOnNodeB, timeout(3000)).convertAndSend(eq(destination), any(Object.class));
        } finally {
            containerA.stop();
            containerB.stop();
        }
    }

    private RedisMessageListenerContainer startListenerContainer(MessageListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(listener, new ChannelTopic(channelProperties.chatChannel()));
        container.afterPropertiesSet();
        container.start();
        return container;
    }
}

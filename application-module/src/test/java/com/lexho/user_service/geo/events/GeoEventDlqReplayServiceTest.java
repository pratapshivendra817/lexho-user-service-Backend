package com.lexho.user_service.geo.events;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeoEventDlqReplayServiceTest {

    @Test
    void replaysUntilQueueIsEmptyAndClampsRequestedMaximum() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        GeoEventDlqReplayService service = new GeoEventDlqReplayService(rabbit, "dlq", "exchange", "routing");
        when(rabbit.receiveAndConvert("dlq")).thenReturn("one", "two", null);

        int replayed = service.replay(0);

        assertThat(replayed).isEqualTo(1);
        verify(rabbit).convertAndSend("exchange", "routing", "one");
    }
}

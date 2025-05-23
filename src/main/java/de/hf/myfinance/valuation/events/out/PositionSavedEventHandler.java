package de.hf.myfinance.valuation.events.out;

import de.hf.myfinance.event.Event;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static de.hf.myfinance.event.Event.Type.START;

@Component
public class PositionSavedEventHandler {
    private final StreamBridge streamBridge;

    public PositionSavedEventHandler(StreamBridge streamBridge){
                this.streamBridge = streamBridge;
            }

    public void sendPositionSavedEvent(String securityId, String depotId) {
        sendMessage("positionSaved-out-0",
                new Event(START, securityId, depotId));
    }

    private void sendMessage(String bindingName, Event event) {
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
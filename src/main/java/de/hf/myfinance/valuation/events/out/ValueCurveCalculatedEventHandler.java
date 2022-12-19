package de.hf.myfinance.valuation.events.out;

import de.hf.myfinance.event.Event;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static de.hf.myfinance.event.Event.Type.START;

public class ValueCurveCalculatedEventHandler {
    private final StreamBridge streamBridge;

    public ValueCurveCalculatedEventHandler(StreamBridge streamBridge){
        this.streamBridge = streamBridge;
    }

    public void sendInstrumentApprovedEvent(String businesskey){
        sendMessage("valueCurveCalculated-out-0",
                new Event(START, businesskey, businesskey));
    }

    private void sendMessage(String bindingName, Event event) {
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
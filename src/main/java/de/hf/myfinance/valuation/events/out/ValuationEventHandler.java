package de.hf.myfinance.valuation.events.out;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.valuation.service.MessageDuplicationFilter;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static de.hf.myfinance.event.Event.Type.START;

@Component
public class ValuationEventHandler {
    private final StreamBridge streamBridge;
    private final MessageDuplicationFilter messageDuplicationFilter;

    public ValuationEventHandler(StreamBridge streamBridge, MessageDuplicationFilter messageDuplicationFilter){
        this.streamBridge = streamBridge;
        this.messageDuplicationFilter = messageDuplicationFilter;
    }

    public void sendValuationEvent(String businesskey){
        if(!messageDuplicationFilter.isAlreadyQueued(businesskey)){
            sendMessage("valuationDataChanged-out-0",
            new Event(START, businesskey, businesskey));
        }
    }

    private void sendMessage(String bindingName, Event event) {
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}

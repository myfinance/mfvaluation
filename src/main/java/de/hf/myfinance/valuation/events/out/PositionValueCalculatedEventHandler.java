package de.hf.myfinance.valuation.events.out;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static de.hf.myfinance.event.Event.Type.CREATE;

@Component
public class PositionValueCalculatedEventHandler {
    private final StreamBridge streamBridge;
    
        public PositionValueCalculatedEventHandler(StreamBridge streamBridge){
            this.streamBridge = streamBridge;
        }
    
        public void sendPositionValueCalculatedEvent(ValueCurve positionValueCurve){
            sendMessage("positionValueCalculated-out-0",
                    new Event(CREATE, positionValueCurve.getInstrumentBusinesskey(), positionValueCurve));
        }
    
        private void sendMessage(String bindingName, Event event) {
            Message message = MessageBuilder.withPayload(event)
                    .setHeader("partitionKey", event.getKey())
                    .build();
            streamBridge.send(bindingName, message);
        }
}
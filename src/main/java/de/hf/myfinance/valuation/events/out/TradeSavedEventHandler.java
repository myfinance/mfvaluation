package de.hf.myfinance.valuation.events.out;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Trade;

import static de.hf.myfinance.event.Event.Type.CREATE;

@Component
public class TradeSavedEventHandler {
    private final StreamBridge streamBridge;
    
        public TradeSavedEventHandler(StreamBridge streamBridge){
            this.streamBridge = streamBridge;
        }
    
        public void sendExtractedTradeEvent(Trade trade){
            sendMessage("tradeSaved-out-0",
                    new Event(CREATE, trade.getSecurityBusinessKey(), trade));
        }
    
        private void sendMessage(String bindingName, Event event) {
            Message message = MessageBuilder.withPayload(event)
                    .setHeader("partitionKey", event.getKey())
                    .build();
            streamBridge.send(bindingName, message);
        }
}
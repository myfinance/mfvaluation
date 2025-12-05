package de.hf.myfinance.valuation.events.out;


import de.hf.myfinance.event.Event;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static de.hf.myfinance.event.Event.Type.START;

@Component
public class PortfolioMetricsEventHandler {

private final StreamBridge streamBridge;

    public PortfolioMetricsEventHandler(StreamBridge streamBridge){
        this.streamBridge = streamBridge;
    }

    public void sendPortfolioMetricsEvent(){
        sendMessage("portfolioMetrics-out-0",
                new Event(START, "all", "all"));
    }

    private void sendMessage(String bindingName, Event event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}

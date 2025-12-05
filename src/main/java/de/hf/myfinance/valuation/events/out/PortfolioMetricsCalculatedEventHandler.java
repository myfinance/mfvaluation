package de.hf.myfinance.valuation.events.out;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.PortfolioMetrics;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static de.hf.myfinance.event.Event.Type.CREATE;

@Component
public class PortfolioMetricsCalculatedEventHandler {
    private final StreamBridge streamBridge;

    public PortfolioMetricsCalculatedEventHandler(StreamBridge streamBridge){
        this.streamBridge = streamBridge;
    }

    public void sendPortfolioMetricsCalculatedEvent(PortfolioMetrics portfolioMetrics){
        sendMessage("portfolioMetricsCalculated-out-0",
                new Event(CREATE, portfolioMetrics.getPortfolio(), portfolioMetrics));
    }

    private void sendMessage(String bindingName, Event event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}

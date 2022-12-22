package de.hf.myfinance.valuation.service;

import reactor.core.publisher.Mono;

public interface ValueHandler {
    Mono<Void> calcValueCurve();
}

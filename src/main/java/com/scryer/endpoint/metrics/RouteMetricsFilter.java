package com.scryer.endpoint.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RouteMetricsFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final MeterRegistry meterRegistry;

    @Autowired
    public RouteMetricsFilter(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        var counter = meterRegistry.counter(request.methodName() +
                              "/" +
                              request.attribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).get(), Tags.empty());
        counter.increment();
        return next.handle(request);
    }

    @Override
    public HandlerFilterFunction<ServerResponse, ServerResponse> andThen(HandlerFilterFunction<ServerResponse,
            ServerResponse> after) {
        return HandlerFilterFunction.super.andThen(after);
    }

    @Override
    public HandlerFunction<ServerResponse> apply(HandlerFunction<ServerResponse> handler) {
        return HandlerFilterFunction.super.apply(handler);
    }
}

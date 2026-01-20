package com.okbank.fintech.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    private static final String CONTEXT_START_TIME_KEY = "startTime";

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(Duration.ofMillis(30000))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(30000, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30000, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(this.requestIdPropagationFilter())
                .build();
    }

    /**
     * RequestId 전파 필터
     * - MDC에서 추출 또는 신규 생성
     * - 요청 헤더에 추가
     */
    private ExchangeFilterFunction requestIdPropagationFilter() {
        return (request, next) -> {
            long startTime = System.currentTimeMillis();

            //MDC에 없으면 새로 생성(외부 API 호출이 최초 진입점인 경우)
            String requestId = Optional.ofNullable(MDC.get(MDC_REQUEST_ID_KEY))
                    .filter(id -> !id.isBlank())
                    .orElse("EXTERNAL-" + UUID.randomUUID());

            //RequestId를 헤더에 추가
            ClientRequest newRequest = ClientRequest.from(request)
                    .header(REQUEST_ID_HEADER, requestId)
                    .build();

            log.info(">>> External API Request [{}] {} {} - Headers: {}",
                    requestId,
                    newRequest.method(),
                    newRequest.url(),
                    newRequest.headers()
            );

            //Reactor Context에도 저장(하위 체인에서 사용 가능)
            return next.exchange(newRequest)
                    .doOnSuccess(response -> {
                        // 응답 로깅
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("<<< External API Response [{}]: status={}, duration={}ms",
                                requestId,
                                response.statusCode().value(),
                                duration
                        );
                    })
                    .doOnError(error -> {
                        // 에러 로깅
                        long duration = System.currentTimeMillis() - startTime;
                        log.error("Error Response [{}]: error={}, duration={}ms",
                                requestId,
                                error.getMessage(),
                                duration
                        );
                    })
                    .flatMap(response -> {
                        if(response.statusCode().is4xxClientError() ||
                        response.statusCode().is5xxServerError()){
                            return this.logErrorResponse(response, requestId);
                        }
                        return Mono.just(response);
                    })
                    .contextWrite(ctx -> ctx
                            .put(MDC_REQUEST_ID_KEY, requestId)
                            .put(CONTEXT_START_TIME_KEY, startTime)
                    );
        };
    }

    /**
     * 에러 응답 body 로깅
     */
    private Mono<ClientResponse> logErrorResponse(ClientResponse response, String reqeustId) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    log.error("Error Response [{}]: status={}, body={}",
                            reqeustId,
                            response.statusCode().value(),
                            body
                    );

                    //body를 다시 response에 설정(downstream에서 사용 가능하도록)
                    ClientResponse newResponse = response.mutate()
                            .body(body)
                            .build();

                    return Mono.just(newResponse);
                })
                .onErrorResume(e -> {
                    log.warn("Failed to read error response body: {}", e.getMessage());
                    return Mono.just(response);
                });
    }

}

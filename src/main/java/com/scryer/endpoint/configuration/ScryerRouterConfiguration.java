package com.scryer.endpoint.configuration;

import com.scryer.endpoint.handler.LoginHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class ScryerRouterConfiguration {
//    @Bean
//    public RouterFunction<ServerResponse> mainRoute() {
//        return RouterFunctions.route().build();
//    }
//
    @Bean
    public RouterFunction<ServerResponse> authRoute(final LoginHandler loginHandler) {
        return RouterFunctions.route(RequestPredicates.POST("/login")
                                                      .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                     loginHandler::login
                              )
                              .andRoute(RequestPredicates.POST("/logout")
                                                         .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                        loginHandler::logout);
    }

}

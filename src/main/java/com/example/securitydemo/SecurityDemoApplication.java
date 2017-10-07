package com.example.securitydemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;

@SpringBootApplication
public class SecurityDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityDemoApplication.class, args);
	}
}

@Configuration
class WebConfiguration {

    Mono<ServerResponse> message(ServerRequest serverRequest) {
        Mono<String> personalizedGreeting = serverRequest.principal().map(p -> "Hello " + p.getName() + "!\n");
        return ServerResponse.ok().body(personalizedGreeting,String.class);
    }

    Mono<ServerResponse> username(ServerRequest serverRequest) {
        Mono<UserDetails> userDetailsMono = serverRequest.principal()
                .map(p -> UserDetails.class.cast(Authentication.class.cast(p).getPrincipal()));
        return ServerResponse.ok().body(userDetailsMono,UserDetails.class);
    }

    @Bean
    RouterFunction<?> routes(){
        return route(GET("/message"), this::message)
                .andRoute(GET("/users/{username}"),this::username);
    }
}

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration{

    @Bean
    UserDetailsRepository userDetailsRepository(){
        UserDetails brian = User.withUsername("brian").roles("USER").password("password").build();
        UserDetails mark = User.withUsername("mark").roles("USER", "ADMIN").password("password").build();
        return new MapUserDetailsRepository(brian,mark);
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(HttpSecurity httpSecurity){
        return httpSecurity
                .authorizeExchange()
                    .pathMatchers("/users/{username}")
                            .access((mono, context) -> mono
                                    .map(auth -> auth.getName().equals(context.getVariables().get("username")))
                                    .map(AuthorizationDecision::new))
                    .anyExchange().authenticated()
                .and()
                .build();
    }
}
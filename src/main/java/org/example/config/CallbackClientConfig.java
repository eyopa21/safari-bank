package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
public class CallbackClientConfig {

    @Bean
    RestClient callbackRestClient(PayoutProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.callback().responseTimeout());
        factory.setReadTimeout(props.callback().responseTimeout());
        return RestClient.builder().requestFactory(factory).build();
    }

    // posts run here so duplicate copies really go out concurrently
    @Bean
    ThreadPoolTaskExecutor callbackExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix("callback-");
        return ex;
    }
}

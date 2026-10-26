package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;

@Configuration
public class CallbackClientConfig {

    @Bean
    RestClient callbackRestClient(PayoutProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.callback().responseTimeout());
        factory.setReadTimeout(props.callback().responseTimeout());
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    TaskScheduler callbackScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("callback-");
        return scheduler;
    }
}

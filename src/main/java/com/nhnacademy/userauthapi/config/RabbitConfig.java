package com.nhnacademy.userauthapi.config;

import com.nhnacademy.userauthapi.config.properties.RabbitAccountProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final RabbitAccountProperties accountProperties;

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    // Account 권한 변경 이벤트 수신용 큐 및 익스체인지
    @Bean
    public Queue accountQueue() {
        return QueueBuilder.durable(accountProperties.getQueue())
                .withArgument("x-dead-letter-exchange", accountProperties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", accountProperties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public TopicExchange accountExchange() {
        return new TopicExchange(accountProperties.getExchange());
    }

    @Bean
    public Binding bindingAccountQueue(Queue accountQueue, TopicExchange accountExchange) {
        return BindingBuilder.bind(accountQueue).to(accountExchange).with(accountProperties.getRoutingKey());
    }

    // DLQ 설정
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(accountProperties.getDeadLetterQueue()).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(accountProperties.getDeadLetterExchange());
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(accountProperties.getDeadLetterRoutingKey());
    }
}

package com.example.wallet_service.Config;

import com.example.wallet_service.DTO.OtpVerifiedEvent;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_CREATED_QUEUE = "transaction.created";
    public static final String OTP_VERIFIED_QUEUE = "otp.verified";
    public static final String TRANSACTION_COMPLETED_QUEUE = "transaction.completed";

    @Bean
    public Queue transactionCreatedQueue() {
        return new Queue(TRANSACTION_CREATED_QUEUE, true); // true = durable
    }

    @Bean
    public Queue otpVerifiedQueue() {
        return new Queue(OTP_VERIFIED_QUEUE, true);
    }

    @Bean
    public Queue transactionCompletedQueue() {
        return new Queue(TRANSACTION_COMPLETED_QUEUE, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        DefaultClassMapper classMapper = new DefaultClassMapper() {
            @Override
            public Class<?> toClass(MessageProperties properties) {
                String typeId = (String) properties.getHeaders().get("__TypeId__");

                if (typeId != null) {
                    if (typeId.equals("com.example.transaction_service.DTO.OtpVerifiedEvent")) {
                        return OtpVerifiedEvent.class;
                    }
                }

                return super.toClass(properties);
            }
        };

        classMapper.setTrustedPackages(
                "com.example.transaction_service.DTO",
                "com.example.wallet_service.DTO");

        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());

        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(1);

        return factory;
    }
}

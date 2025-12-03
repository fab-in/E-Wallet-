package com.example.transaction_service.Config;

import com.example.transaction_service.DTO.TransactionCreatedEvent;
import com.example.transaction_service.DTO.TransactionCompletedEvent;
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

/**
 * RabbitMQ Configuration for Transaction Service
 * Defines queues and message converters for async communication
 */
@Configuration
public class RabbitMQConfig {
    
    // Queue names - must match Wallet Service
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
        
        // Create a custom class mapper that maps wallet-service types to transaction-service types
        DefaultClassMapper classMapper = new DefaultClassMapper() {
            @Override
            public Class<?> toClass(MessageProperties properties) {
                // Get the type ID from message properties
                String typeId = (String) properties.getHeaders().get("__TypeId__");
                
                // Map wallet-service DTOs to transaction-service DTOs
                if (typeId != null) {
                    if (typeId.equals("com.example.wallet_service.DTO.TransactionCreatedEvent")) {
                        return TransactionCreatedEvent.class;
                    }
                    if (typeId.equals("com.example.wallet_service.DTO.TransactionCompletedEvent")) {
                        return TransactionCompletedEvent.class;
                    }
                }
                
                // Fall back to default behavior
                return super.toClass(properties);
            }
        };
        
        // Add trusted packages
        classMapper.setTrustedPackages(
            "com.example.wallet_service.DTO",
            "com.example.transaction_service.DTO"
        );
        
        converter.setClassMapper(classMapper);
        return converter;
    }
    
    /**
     * RabbitTemplate with JSON message converter
     * Converts Java objects to JSON automatically
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
    
    /**
     * RabbitListenerContainerFactory with custom message converter
     * This ensures listeners can deserialize messages from wallet-service
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        
        // Configure to prevent infinite retries
        // Set acknowledgment mode to manual so we can control when to acknowledge
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.AUTO);
        
        // Set default requeue rejected to false - prevents infinite retry loop
        factory.setDefaultRequeueRejected(false);
        
        // Set prefetch count to 1 to process messages one at a time
        factory.setPrefetchCount(1);
        
        return factory;
    }
}


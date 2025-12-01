package com.bookingapp.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.bookingapp.config.RabbitConfig;

@Component
public class BookingProducer {

    private final RabbitTemplate rabbitTemplate;

    public BookingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBookingMessage(String email) {
        rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_NAME, email);
        System.out.println("📩 Sent to Queue → " + email);
    }
}

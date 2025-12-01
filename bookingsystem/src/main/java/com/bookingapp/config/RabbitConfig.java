package com.bookingapp.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String QUEUE_NAME = "booking_email_queue";

	@Bean
	public Queue queue() {
		return new Queue(QUEUE_NAME, true);
	}
}

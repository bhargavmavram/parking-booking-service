package com.parking.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "parking.jwt")
public record JwtProperties(String secret) {
}
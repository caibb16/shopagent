package com.example.shopagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class StoreConfig {
    // Dev profile uses @Repository-annotated in-memory impls.
    // This class exists so additional dev-only wiring has a home.
}

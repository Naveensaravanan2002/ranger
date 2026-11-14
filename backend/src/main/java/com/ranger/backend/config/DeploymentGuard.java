package com.ranger.backend.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Prevent a deployed container from silently accepting data into disposable in-memory H2. */
@Component
public class DeploymentGuard {
    public DeploymentGuard(Environment environment, @Value("${ranger.require-persistent:false}") boolean required) {
        if (required && Arrays.stream(environment.getActiveProfiles()).noneMatch("neon"::equals))
            throw new IllegalStateException("Persistent deployment requires SPRING_PROFILES_ACTIVE=neon and a configured PostgreSQL database. H2 is for local demo only.");
    }
}

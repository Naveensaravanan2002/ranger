package com.ranger.backend;

import static org.junit.jupiter.api.Assertions.*;
import com.ranger.backend.config.DeploymentGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DeploymentGuardTests {
    @Test void persistentDeploymentRejectsTheDefaultH2Profile() {
        var env=new MockEnvironment();
        assertThrows(IllegalStateException.class,()->new DeploymentGuard(env,true));
        assertDoesNotThrow(()->new DeploymentGuard(env,false));
        env.setActiveProfiles("neon");
        assertDoesNotThrow(()->new DeploymentGuard(env,true));
    }
}

package com.javadai.agent.execution;

import java.time.Duration;

public record ExecutionResult(
        boolean successful,
        boolean timedOut,
        Integer exitCode,
        String stdout,
        String stderr,
        Duration duration
) {
}
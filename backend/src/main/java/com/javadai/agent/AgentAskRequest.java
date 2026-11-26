package com.javadai.agent;

public record AgentAskRequest(
        String instruction,
        String activeFilePath,
        String activeFileContent
) {}
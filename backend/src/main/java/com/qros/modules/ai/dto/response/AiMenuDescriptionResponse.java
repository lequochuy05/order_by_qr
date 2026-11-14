package com.qros.modules.ai.dto.response;

import java.util.List;

public record AiMenuDescriptionResponse(String shortDescription, String engagingDescription, List<String> tasteTags) {}

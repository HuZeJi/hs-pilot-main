package com.huggingsoft.pilot_main.shared;

import lombok.Data;

@Data
public class RequestContext {
    private final String requestId; // Final makes it immutable after creation
    private final long startTimeMillis;
    private String authorizationHeader;
    // Add other fields as needed, e.g., principal object
}

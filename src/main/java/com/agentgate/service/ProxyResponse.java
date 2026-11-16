package com.agentgate.service;

import lombok.Data;

@Data
public class ProxyResponse {

    private int statusCode;
    private String contentType;
    private byte[] body;
}

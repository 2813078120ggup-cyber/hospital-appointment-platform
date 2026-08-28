package com.atguigu.java.ai.langchain4j.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

@Service
public class ModelReadinessService {

    private final URI modelBaseUri;

    public ModelReadinessService(
            @Value("${langchain4j.open-ai.chat-model.base-url:https://api.deepseek.com}") String modelBaseUrl) {
        this.modelBaseUri = URI.create(modelBaseUrl);
    }

    public boolean isReachable() {
        String host = modelBaseUri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        int port = modelBaseUri.getPort();
        if (port < 0) {
            port = "https".equalsIgnoreCase(modelBaseUri.getScheme()) ? 443 : 80;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 800);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}

package com.fubt.service;

import org.java_websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>@Description: </p>
 *
 * @author Sue
 * @date 2019/6/185:21 PM
 */
@Component
public class ScoketClient {
    @Autowired
    private WebSocketClient webSocketClient;

    public void groupSending(String message) {
        webSocketClient.send(message);
    }
}

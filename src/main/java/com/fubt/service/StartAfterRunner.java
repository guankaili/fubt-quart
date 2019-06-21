package com.fubt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * <p>@Description: </p>
 *
 * @author Sue
 * @date 2019/6/206:58 PM
 */
@Component
public class StartAfterRunner implements CommandLineRunner {

    @Autowired
    private ScoketClient scoketClient;

    @Override
    public void run(String... args) throws Exception {
        new Thread(() -> {
            while (true) {
                try {
                    scoketClient.groupSending("{\"id\":1,\"method\":\"today.query\",\"params\":[\"OTSCUSDT\"]}");
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

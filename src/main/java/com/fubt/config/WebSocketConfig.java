package com.fubt.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fubt.utils.Constant;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * <p>@Description: </p>
 *
 * @author Sue
 * @date 2019/6/185:20 PM
 */
@Component
public class WebSocketConfig {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public WebSocketClient webSocketClient() {
        try {
            WebSocketClient webSocketClient = new WebSocketClient(new URI("wss://wss.shanliani.com/wss"),new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("[websocket] 连接成功");
                }

                @Override
                public void onMessage(String message) {
                    logger.info("[websocket] 收到消息={}",message);
                    JSONObject jsonObject = JSONObject.parseObject(message);

                    String method = jsonObject.getString("method");
                    if ("depth.update".equals(method)) {
                        // 盘口数据
//                        {"method": "depth.update", "params": [true, {"asks": [["4.93939657", "10.42"], ["4.94159498", "2.43"], ["4.94160512", "21.11"], ["4.94161199", "22.7"], ["4.94161343", "30.7"], ["4.94161375", "28.93"], ["4.94161474", "37.97"], ["4.94161672", "21.81"], ["4.94162043", "22.06"], ["4.94162207", "19.55"]], "bids": [["4.9376875", "14.06"], ["4.93591112", "40.84"], ["4.93477092", "12.09"], ["4.93459507", "38.34"], ["4.93454819", "27.05"], ["4.93454415", "36.39"], ["4.93442349", "20.41"], ["4.93439907", "14.52"], ["4.93438879", "36.25"], ["4.93424129", "14.19"]]}, "OTSCUSDT"], "id": null}
                        // true 表示全量更新，false表示增量更新
                        if (jsonObject.getJSONArray("params").getBoolean(0)) {
                            JSONObject askBids = jsonObject.getJSONArray("params").getJSONObject(1);
                            JSONArray asks = askBids.getJSONArray("asks");
                            JSONArray bids = askBids.getJSONArray("bids");

                            Constant.BUY_ONE_PRICE = bids.getJSONArray(0).getDouble(0);
                            Constant.SELL_ONE_PRICE = asks.getJSONArray(0).getDouble(0);

                            System.out.println();
                        }


                    } else {
                        // 最新价格
                        Constant.LAST_PRICE = jsonObject.getJSONObject("result").getDouble("last");
                    }

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("[websocket] 退出连接");
                }

                @Override
                public void onError(Exception ex) {
                    logger.info("[websocket] 连接错误={}",ex.getMessage());
                }
            };
            webSocketClient.connect();

            return webSocketClient;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


//    public static void main(String[] args) {
//        try {
//            WebSocketClient webSocketClient = new WebSocketClient(new URI("wss://wss.shanliani.com/wss"),new Draft_6455()) {
//                @Override
//                public void onOpen(ServerHandshake handshakedata) {
//                    System.out.println("[websocket] 连接成功");
//                }
//
//                @Override
//                public void onMessage(String message) {
//                    System.out.println("[websocket] 收到消息=" + message);
//
//                }
//
//                @Override
//                public void onClose(int code, String reason, boolean remote) {
//                    System.out.println("[websocket] 退出连接");
//                }
//
//                @Override
//                public void onError(Exception ex) {
//                    System.out.println("[websocket] 连接错误=" + ex.getMessage());
//                }
//            };
//            webSocketClient.connect();
//
//            while(!webSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)){
//                System.out.println("还没有打开");
//            }
//
////            webSocketClient.send("{\"id\":1,\"method\":\"server.ping\",\"params\":[\"\"]}".getBytes("utf-8"));
////            webSocketClient.send("{\"id\":1,\"method\":\"server.auth\",\"params\":[\"iWe-bUXtkVmLjb6Y7ZFeIsxk-Gy9xgo9_1560598261|web\",\"web\"]}".getBytes("utf-8"));
//
//            for (int i=0; i<10 ;i++) {
//                webSocketClient.send("{\"id\":1,\"method\":\"today.query\",\"params\":[\"OTSCUSDT\"]}".getBytes("utf-8"));
//                Thread.sleep(2000L);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

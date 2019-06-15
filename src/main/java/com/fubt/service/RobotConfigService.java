package com.fubt.service;

import com.alibaba.fastjson.JSONObject;
import com.fubt.dao.RobotConfigDao;
import com.fubt.dao.UserDao;
import com.fubt.entity.RobotConfig;
import com.fubt.entity.User;
import com.fubt.utils.NumberUtils;
import com.fubt.utils.SignUtils;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1410:27 PM
 */
@Service
public class RobotConfigService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RobotConfigDao robotConfigDao;
    @Autowired
    private UserDao userDao;

    public static final AtomicInteger maxSellOrders = new AtomicInteger(0);
    public static final AtomicInteger maxBuyOrders = new AtomicInteger(0);

    private static final String FUBT_TICKER = "https://api.fubt.co/v1/market/ticker";
    private static final String FUBT_ENTRUST = "https://api.fubt.co/v1/order/saveEntrust ";
    public static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";

    public static OkHttpClient client =new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                // 使用ConcurrentMap存储cookie信息，因为数据在内存中，所以只在程序运行阶段有效，程序结束后即清空
                private ConcurrentMap<String, List<Cookie>> storage = new ConcurrentHashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    String host = url.host();
                    if (cookies != null && !cookies.isEmpty()) {
                        storage.put(host, cookies);
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    String host = url.host();
                    List<Cookie> list = storage.get(host);
                    return list == null ? new ArrayList<>() : list;
                }
            })
            .build();

    /**
     * 计数器清零
     */
    public void cleanAtomic() {
        maxSellOrders.set(0);
        maxBuyOrders.set(0);
    }

    /**
     * 获取所有机器人配置
     * @return
     */
    public List<RobotConfig> allRobots() {
        return robotConfigDao.all();
    }

    /**
     * 根据id获取机器人信息
     * @param id
     * @return
     */
    public RobotConfig getRobotConfigById(int id) {
        return robotConfigDao.single(id);
    }

    /**
     * 启动机器人
     * @param id
     */
    public void start(int id) {
        RobotConfig robotConfig = robotConfigDao.single(id);
        robotConfig.setStatus(1);
        robotConfigDao.updateById(robotConfig);

        new Thread(() -> {
            try {
                while (true) {
                    RobotConfig robot = robotConfigDao.single(id);
                    if (robot.getStatus() == 0) {
                        logger.info("{} 市场的机器人 {} 已经停止！", robot.getSymbol(), robot.getName());
                        return ;
                    }

                    try {
                        doEntrust(robot);
                    } catch (Exception e) {
                        logger.error("刷量异常....", e);
                    }

                    long sleep = robot.getFrequence();
//                    if (sleep < 2000) {
//                        sleep = 2000;
//                    }

                    Thread.sleep(sleep);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

    }

    /**
     * 执行委托操作
     */
    private void doEntrust(RobotConfig robotConfig) throws Exception {
        if (maxBuyOrders.get() >= robotConfig.getMaxBuyOrders() || maxSellOrders.get() >= robotConfig.getMaxSellOrders()) {
            logger.warn("{} 市场的机器人 {} 已经停止，挂卖单总笔数：{}，挂买单总笔数：{}",
                    robotConfig.getSymbol(), robotConfig.getName(), maxSellOrders.get(), maxBuyOrders.get());
            return;
        }

        User user = userDao.single(robotConfig.getUserId());

        // get请求accesskey需要进行url编码
//        String accessKey = java.net.URLEncoder.encode(user.getAccessKey().trim(), "UTF-8");
//
//        // 1. 获取买卖价格
//        Request request = new Request.Builder()
//                .url(FUBT_TICKER + "?symbol=" + robotConfig.getSymbol() + "&accessKey=" + accessKey)
//                .get()
//                .addHeader("cache-control", "no-cache")
//                .addHeader("User-Agent", userAgent)
//                .addHeader("Host", "api.fubt.co")
//                .build();
//
//        Response response = client.newCall(request).execute();
//        String tickerStr = response.body().string();
//
//        JSONObject tickerJson = JSONObject.parseObject(tickerStr).getJSONObject("data");
//
//        double buy = tickerJson.getDouble("buy");
//        double sell = tickerJson.getDouble("sell");
//        double minPrice = buy == 0 ? robotConfig.getMinPrice() : buy;
//        double maxPrice = sell == 0 ? robotConfig.getMaxPrice() : sell;

        double minPrice = robotConfig.getMinPrice();
        double maxPrice = robotConfig.getMaxPrice();

        //对2进去取余  0卖 1买
        int isBuy = new java.util.Random().nextInt(10) % 2;
        //委托价格在盘口买卖一偏离10%进行，确保及时成交
//        if(isBuy==0){
//            //卖
//            minPrice = NumberUtils.mul(minPrice, 0.95, 4);
//        }else if(isBuy==1){
//            //买
//            maxPrice = NumberUtils.mul(maxPrice, 1.05, 4);
//        }

        // 2. 计算出挂单价格和数量
        double price = NumberUtils.getRandom(minPrice, maxPrice);
        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 4);
        number = NumberUtils.mul(number, 1, 2);

        // 3. 签名信息，拼接参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("count", String.valueOf(number));
        jsonObject.put("accessKey", user.getAccessKey().trim());
        jsonObject.put("matchType", "limit");
        jsonObject.put("payPwd", user.getTransPwd().trim());
        jsonObject.put("price", String.valueOf(price));
        jsonObject.put("type", isBuy == 0 ? "sell" : "buy");
        jsonObject.put("symbol", robotConfig.getSymbol());

        String signString = SignUtils.jsonToString(jsonObject.toJSONString());
        String signature = SignUtils.sha256_HMAC(signString, user.getSecretKey().trim());
        jsonObject.put("signature",signature);

        // 4. 发送交易
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toJSONString());
        Request.Builder requestBuilder = new Request.Builder();
        Request entrustRequest = requestBuilder.post(requestBody)
                .url(FUBT_ENTRUST)
//                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("Host", "api.fubt.co")
                .build();

        Response entrustResponse = client.newCall(entrustRequest).execute();
        String entrustResultStr = entrustResponse.body().string();
        logger.info("{} 市场的机器人 {} 成功委托一笔交易 [{}, price:{}, num:{}], 委托结果: {}",
                robotConfig.getSymbol(), robotConfig.getName(), isBuy == 0 ? "sell" : "buy", price, number, entrustResultStr);

        if(isBuy==0){
            // 卖
            maxSellOrders.incrementAndGet();
        }else if(isBuy==1){
            //买
            maxBuyOrders.incrementAndGet();
        }
    }

    public void add(RobotConfig robotConfig) {
        robotConfigDao.insert(robotConfig);
    }

    public void update(RobotConfig robotConfig) {
        robotConfigDao.updateById(robotConfig);
    }

    public void deleteById(Integer id) {
        robotConfigDao.deleteById(id);
    }

    public void cleanRobotStatus() {
        List<RobotConfig> robotConfigs = allRobots();
        for (RobotConfig robotConfig : robotConfigs) {
            robotConfig.setStatus(0);
            robotConfigDao.updateById(robotConfig);
        }
    }
}

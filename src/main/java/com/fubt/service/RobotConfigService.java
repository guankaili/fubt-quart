package com.fubt.service;

import com.alibaba.fastjson.JSONObject;
import com.fubt.dao.RobotConfigDao;
import com.fubt.dao.UserDao;
import com.fubt.entity.RobotConfig;
import com.fubt.entity.User;
import com.fubt.utils.Constant;
import com.fubt.utils.NumberUtils;
import com.fubt.utils.TraineeTrader;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
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
    // 小数点保留位数
    public static final int SCALE = 4;
    private static final double MAX_RATIO = 0.0004d;
    private static Random random = new Random();
    public static final AtomicInteger maxSellOrders = new AtomicInteger(0);
    public static final AtomicInteger maxBuyOrders = new AtomicInteger(0);

    private static final String SHANLIAN_ENTRUST = "https://www.shanliani.com/api/bargain/order-limit";
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
        int[] adr = new int[2];
        adr[0] = 2;
        adr[1] = 1;
        // 刷k线
        new Thread(() -> {
            try {
                while (true) {
                    RobotConfig robot = robotConfigDao.single(id);
                    if (robot.getStatus() == 0) {
                        logger.info("{} 市场的机器人 {} 已经停止！", robot.getSymbol(), robot.getName());
                        return ;
                    }

                    try {
                        sellBuyBatch(robot);
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
     * 自买自卖
     * @param robotConfig
     * @throws Exception
     */
    private void sellBuyBatch(RobotConfig robotConfig) throws Exception {

        User user = userDao.single(robotConfig.getUserId());


        // 2. 计算出挂单价格和数量
        double price = NumberUtils.getRandom(Constant.BUY_ONE_PRICE, Constant.SELL_ONE_PRICE);
        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 3);
        number = NumberUtils.mul(number, 1, 2);

        // 3. 签名信息，拼接参数

        // 4. 发送交易
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MediaType.parse("multipart/form-data"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"amount\""), RequestBody.create(null, String.valueOf(number)))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"market\""), RequestBody.create(null, robotConfig.getSymbol()))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"side\""), RequestBody.create(null, "1"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"pride\""), RequestBody.create(null, String.valueOf(price)))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"access_token\""), RequestBody.create(null, user.getAccessKey().trim()))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"chain_network\""), RequestBody.create(null, "main_network"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"os\""), RequestBody.create(null, "web"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"os_ver\""), RequestBody.create(null, "1.0.0"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"soft_ver\""), RequestBody.create(null, "1.0.0"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"language\""), RequestBody.create(null, "zh_cn"))
                .build();

        Request.Builder requestBuilder = new Request.Builder();
        Request entrustRequest = requestBuilder.post(requestBody)
                .url(SHANLIAN_ENTRUST)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("user-agent", userAgent)
                .build();

        Response entrustResponse = client.newCall(entrustRequest).execute();



        // 4. 发送交易
        RequestBody requestBody1 = new MultipartBody.Builder()
                .setType(MediaType.parse("multipart/form-data"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"amount\""), RequestBody.create(null, String.valueOf(number)))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"market\""), RequestBody.create(null, robotConfig.getSymbol()))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"side\""), RequestBody.create(null, "2"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"pride\""), RequestBody.create(null, String.valueOf(price)))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"access_token\""), RequestBody.create(null, user.getAccessKey().trim()))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"chain_network\""), RequestBody.create(null, "main_network"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"os\""), RequestBody.create(null, "web"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"os_ver\""), RequestBody.create(null, "1.0.0"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"soft_ver\""), RequestBody.create(null, "1.0.0"))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"language\""), RequestBody.create(null, "zh_cn"))
                .build();

        Request.Builder requestBuilder1 = new Request.Builder();
        Request entrustRequest1 = requestBuilder1.post(requestBody1)
                .url(SHANLIAN_ENTRUST)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("user-agent", userAgent)
                .build();

        Response entrustResponse1 = client.newCall(entrustRequest1).execute();
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
        double lastPrice = Constant.LAST_PRICE;
//        double minPrice = robotConfig.getMinPrice();
        double maxPrice = robotConfig.getMaxPrice();
//        double finalPrice = robotConfig.getTargetPrice();
        double range = robotConfig.getRange();
//        int cycle = robotConfig.getCycle();

        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());
        int[] adr = new int[2];
        adr[0] = 2;
        adr[1] = 1;
        //对2进去取余  0买  1卖
        int isBuy = new java.util.Random().nextInt(10) % 2;
//        double targetPrice = NumberUtils.getRandom(minPrice, maxPrice);
        double targetPrice = 0;
        int minute = getMinute(new Date());
        if((minute>=25 && minute<35) || (minute>=5 && minute<10) || (minute>=50 && minute<55)){
            if(maxPrice>=lastPrice){
                targetPrice = lastPrice - range;
                isBuy = 1;
            }else{
                targetPrice = lastPrice + range;
                isBuy = 0;
            }
        }else{
            if(maxPrice>=lastPrice){
                targetPrice = lastPrice + range;
                isBuy = 0;
            }else{
                targetPrice = lastPrice - range;
                isBuy = 1;
            }

        }

        List<BigDecimal> prices = TraineeTrader.randomPrices(new BigDecimal(String.valueOf(lastPrice)),new BigDecimal(String.valueOf(targetPrice)),5,adr,4);
        logger.info("开始时间："+new Date()+"目标价格"+ String.valueOf(targetPrice));
        int i = 0;
        for(BigDecimal priceB : prices){
//            isBuy = new java.util.Random().nextInt(10) % 2;
            double price = priceB.doubleValue();
            price = NumberUtils.mul(price, 1, 4);
            number = NumberUtils.mul(number, 1, 2);
            // 3. 签名信息，拼接参数
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("amount", String.valueOf(number));
            jsonObject.put("market", robotConfig.getSymbol());
            // buy:2;sell:1
            jsonObject.put("side", isBuy==0 ? 2 : 1);
            jsonObject.put("price", String.valueOf(price));
            jsonObject.put("access_token", user.getAccessKey().trim());
            jsonObject.put("chain_network", "main_network");
            jsonObject.put("os", "web");
            jsonObject.put("os_ver", "1.0.0");
            jsonObject.put("soft_ver", "1.0.0");
            jsonObject.put("language", "zh_cn");

            // 4. 发送交易
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MediaType.parse("multipart/form-data"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"amount\""), RequestBody.create(null, String.valueOf(number)))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"market\""), RequestBody.create(null, robotConfig.getSymbol()))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"side\""), RequestBody.create(null, isBuy==0 ? "2" : "1"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"pride\""), RequestBody.create(null, String.valueOf(price)))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"access_token\""), RequestBody.create(null, user.getAccessKey().trim()))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"chain_network\""), RequestBody.create(null, "main_network"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"os\""), RequestBody.create(null, "web"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"os_ver\""), RequestBody.create(null, "1.0.0"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"soft_ver\""), RequestBody.create(null, "1.0.0"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"language\""), RequestBody.create(null, "zh_cn"))
                    .build();

            Request.Builder requestBuilder = new Request.Builder();
            Request entrustRequest = requestBuilder.post(requestBody)
                    .url(SHANLIAN_ENTRUST)
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .addHeader("user-agent", userAgent)
                    .build();

            Response entrustResponse = client.newCall(entrustRequest).execute();
            String entrustResultStr = entrustResponse.body().string();
            logger.info("第{}次，{} 市场的机器人 {} 成功委托一笔交易 [{}, price:{}, num:{}], 委托结果: {}",
                    i++,robotConfig.getSymbol(), robotConfig.getName(), isBuy==0 ? "buy" : "sell", price, number, entrustResultStr);
            Thread.sleep(3500);
        }
        logger.info("结束时间："+new Date()+"目标价格"+ String.valueOf(targetPrice));
        if(isBuy==0){
            // 卖
            maxSellOrders.incrementAndGet();
        }else if(isBuy==1){
            //买
            maxBuyOrders.incrementAndGet();
        }
    }

    public void doTrading(RobotConfig robot){
        try {
            doEntrust(robot);

        } catch (Exception e) {
            logger.info("出错了："+e.getMessage());
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

    /**
     * 功能描述：返回分
     *
     * @param date
     *            日期
     * @return 返回分钟
     */
    public int getMinute(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }
    /**
     * 功能描述：返回小时
     *
     * @param date
     *            日期
     * @return 返回小时
     */
    public int getHour(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

//    public static void main(String[] args) {
//        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
//                new BasicThreadFactory.Builder().namingPattern("example-schedule-pool-%d").daemon(true).build());
//        executorService.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                //do something
//            }
//        },initialDelay,period, TimeUnit.MINUTES);
//    }
}

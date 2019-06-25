package com.fubt.service;

import com.alibaba.fastjson.JSONObject;
import com.fubt.dao.RobotConfigDao;
import com.fubt.dao.UserDao;
import com.fubt.entity.RobotConfig;
import com.fubt.entity.User;
import com.fubt.utils.Constant;
import com.fubt.utils.NumberUtils;
import com.fubt.utils.SignUtils;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.applet.Main;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        double lastPrice = Constant.LAST_PRICE;
        double targetPrice = 4.89D;
        int cycle = 15;
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
                        doEntrust(robot);
//
//                        Thread.sleep(500);

//                        randomCycleTargetPrices(robot,new BigDecimal(String.valueOf(lastPrice)), cycle, adr, 1,new BigDecimal(String.valueOf(targetPrice)));
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
     * 执行委托操作
     */
    private void doEntrust(RobotConfig robotConfig) throws Exception {
        if (maxBuyOrders.get() >= robotConfig.getMaxBuyOrders() || maxSellOrders.get() >= robotConfig.getMaxSellOrders()) {
            logger.warn("{} 市场的机器人 {} 已经停止，挂卖单总笔数：{}，挂买单总笔数：{}",
                    robotConfig.getSymbol(), robotConfig.getName(), maxSellOrders.get(), maxBuyOrders.get());
            return;
        }

        User user = userDao.single(robotConfig.getUserId());

        double minPrice = robotConfig.getMinPrice();
        double maxPrice = robotConfig.getMaxPrice();

        //对2进去取余  0买  1卖
        int isBuy = new java.util.Random().nextInt(10) % 2;

        // 2. 计算出挂单价格和数量
        double price = NumberUtils.getRandom(minPrice, maxPrice);
        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 3);
        number = NumberUtils.mul(number, 1, 2);
        int hour = getHour(new Date());
        // 3. 签名信息，拼接参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("amount", String.valueOf(number));
        jsonObject.put("market", robotConfig.getSymbol());
        // buy:2;sell:1
        jsonObject.put("side", isBuy == 0 ? 1 : 2);
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
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"side\""), RequestBody.create(null, isBuy == 0 ? "1" : "2"))
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

    /**
     * 自买自卖
     * @param robotConfig
     * @throws Exception
     */
    private void sellBuyBatch(RobotConfig robotConfig) throws Exception {

        User user = userDao.single(robotConfig.getUserId());

        double heightPrice =  Constant.HEIGHT_PRICE;
        double lowPrice = Constant.LOW_PRICE;
        double lastPrice = Constant.LAST_PRICE;
        double openPrice = Constant.OPEN_PRICE;
        double price = NumberUtils.getRandom(Constant.BUY_ONE_PRICE, Constant.SELL_ONE_PRICE);
        // 2. 计算出挂单价格和数量
        if(openPrice >= lastPrice){
            if(openPrice >= heightPrice){
                price = Constant.SELL_MIDDLE_PRICE;
            }
            if(lastPrice <= lowPrice){
                price = lowPrice+0.01D;
            }
        }else{
            if(lastPrice >= heightPrice){
                price = Constant.SELL_MIDDLE_PRICE;
            }
            if(openPrice <= lowPrice){
                price = openPrice - 0.01D;
            }
        }
        int hour = getHour(new Date());
////        int minute = getMinute(new Date());
        if(hour%2 > 0){
            price = price - 0.005D;
//            if(minute > 30){
//            }
        }
//        price = NumberUtils.getRandom(Constant.BUY_ONE_PRICE, Constant.SELL_ONE_PRICE);
        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 4);
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
     * 自买自卖
     * @param robotConfig
     * @throws Exception
     */
    private void batch(RobotConfig robotConfig,double price) throws Exception {

        User user = userDao.single(robotConfig.getUserId());

        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 4);
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
     * 买币交易
     * @param robotConfig
     * @throws Exception
     */
    private void buyCoinTrade(RobotConfig robotConfig) throws Exception {

        User user = userDao.single(robotConfig.getUserId());

        double price = Constant.SELL_MIDDLE_PRICE;

        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 4);
        number = NumberUtils.mul(number, 1, 2);

        // 3. 签名信息，拼接参数

        // 4. 发送交易
        RequestBody requestBody = new MultipartBody.Builder()
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

        Request.Builder requestBuilder = new Request.Builder();
        Request entrustRequest = requestBuilder.post(requestBody)
                .url(SHANLIAN_ENTRUST)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("user-agent", userAgent)
                .build();

        Response entrustResponse = client.newCall(entrustRequest).execute();

    }

    /**
     * 卖币交易
     * @param robotConfig
     * @throws Exception
     */
    private void sellCoinTrade(RobotConfig robotConfig) throws Exception {

        User user = userDao.single(robotConfig.getUserId());

        double price = Constant.BUY_MIDDLE_PRICE;

        double number = NumberUtils.getRandom(robotConfig.getMinNum(), robotConfig.getMaxNum());

        price = NumberUtils.mul(price, 1, 4);
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

    /**
     * 随机生成各个阶段的目标价格
     * @param marketPrice 市价
     * @param cycle 周期
     * @param adr 涨跌比
     * @param minCycle 最小周期单位
     * @param targetPrice 目标价格
     * @return 各个阶段的目标价格
     */
    public void randomCycleTargetPrices(RobotConfig robotConfig,BigDecimal marketPrice, int cycle, int[] adr, int minCycle, BigDecimal targetPrice) {
        //
        List<BigDecimal> ranges = randomCycleRanges(minCycle, cycle, adr);

        BigDecimal curPrice = marketPrice;
        BigDecimal priceDiff = targetPrice.compareTo(marketPrice) != 0 ? targetPrice.subtract(marketPrice) : marketPrice.multiply(getRatio()).setScale(SCALE, BigDecimal.ROUND_DOWN);
        BigDecimal minPrice;
        BigDecimal maxPrice;

        if (ranges.size() > 0) {
            // 各阶段平均价格
            BigDecimal avgDiff = priceDiff.divide(new BigDecimal(ranges.size()), SCALE, BigDecimal.ROUND_DOWN);

            if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
                minPrice = marketPrice;
                maxPrice = targetPrice;
            } else {
                minPrice = targetPrice;
                maxPrice = marketPrice;
            }
            BigDecimal curDiff;

            BigDecimal adjustDiff = BigDecimal.ZERO;// 调整价差
            int len = ranges.size() > 8 ? ranges.size() / 4 : ranges.size();
            for (int i = 0, size = ranges.size(); i < size; i++) {
                curDiff = priceDiff.multiply(ranges.get(i)).setScale(SCALE, BigDecimal.ROUND_DOWN);
                curPrice = curPrice.add(curDiff);
                logger.info("成交价格为："+curPrice.toPlainString());
                if (marketPrice.compareTo(targetPrice) != 0) {
                    BigDecimal curMin;
                    BigDecimal curMax;
//                     设置阶段的最大价格和最小价格
                    if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
                        curMin = marketPrice.add(avgDiff.multiply(new BigDecimal(i - len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                        curMax = marketPrice.add(avgDiff.multiply(new BigDecimal(i + len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                    } else {
                        curMin = marketPrice.add(avgDiff.multiply(new BigDecimal(i + len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                        curMax = marketPrice.add(avgDiff.multiply(new BigDecimal(i - len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                    }
//                     存在调整价差
                    if (adjustDiff.compareTo(BigDecimal.ZERO) != 0) {
                        curPrice = curPrice.subtract(adjustDiff);
                        adjustDiff = BigDecimal.ZERO;
                    }

                    BigDecimal adjustedPrice = adjustPrice(
                            // 最低价格不能低于周期的最低价格
                            minPrice.compareTo(curMin) < 0 ? curMin : minPrice,
                            // 最搞价格不能高于周期的最高价格
                            maxPrice.compareTo(curMax) > 0 ? curMax : maxPrice,
                            curPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : curPrice,
                            3);
                    adjustDiff = curPrice.subtract(adjustedPrice);
                    try {
//                        doEntrust(robotConfig);
                        batch(robotConfig,curPrice.doubleValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                }
            }
        }

    }

    private static BigDecimal adjustPrice(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal curPrice, int num) {
        double randomMax = curPrice.multiply(getRatio()).setScale(SCALE, BigDecimal.ROUND_DOWN).doubleValue();
        if (num == 0) {
            return curPrice;
        }

        num--;

        if (curPrice.compareTo(minPrice) < 0) {
            // 超过最小值
            BigDecimal curDiff = new BigDecimal(random.nextDouble() * randomMax).setScale(SCALE, BigDecimal.ROUND_DOWN);

            return adjustPrice(minPrice, maxPrice, curPrice.add(curDiff.abs()), num);
        }

        if (curPrice.compareTo(maxPrice) > 0) {
            // 超过最大值
            BigDecimal curDiff = new BigDecimal(random.nextDouble() * randomMax).setScale(SCALE, BigDecimal.ROUND_DOWN);
            return adjustPrice(minPrice, maxPrice, curPrice.subtract(curDiff.abs()), num);
        }

        return curPrice;
    }
    private static BigDecimal getRatio() {
        return new BigDecimal(random.nextDouble() * MAX_RATIO).setScale(SCALE, BigDecimal.ROUND_DOWN);
    }

    /**
     * 随机生成周期各阶段涨跌幅
     * @param minCycle 最小周期分钟数
     * @param cycle 周期分钟数
     * @param adr 涨跌比
     * @return 阶段价格列表
     */
    private static List<BigDecimal> randomCycleRanges(int minCycle,int cycle, int[] adr) {
        int stage = cycle >= minCycle ? (cycle -1)/ minCycle : 1;
        if (stage == 0) {
            return new ArrayList<>();
        }
        double range = (double) minCycle /cycle;

        List<BigDecimal> ranges = new ArrayList<>();

        List<Integer> symbols = randomCycleTrend(stage, adr);

        BigDecimal totalRange = BigDecimal.ZERO;
        double tempRange;
        int forwardNum = 0;
        int reverseNum = 0;

        for (Integer symbol : symbols) {
            // 生成各阶段的随机涨跌幅
            tempRange = symbol * (random.nextDouble() * range + range);
            if (tempRange > 0) {
                forwardNum++;
            }
            if (tempRange < 0) {
                reverseNum++;
            }
            BigDecimal curRange = new BigDecimal(tempRange).setScale(SCALE, BigDecimal.ROUND_DOWN);

            ranges.add(curRange);
            totalRange = totalRange.add(curRange);
        }

        // 调整
//        BigDecimal diff = BigDecimal.ONE.subtract(totalRange);
//        boolean needAdjust = diff.compareTo(BigDecimal.ZERO) != 0;
//
//        if (diff.compareTo(BigDecimal.ZERO) > 0) {
//            if (forwardNum > 0) {
//                // 低了，需要拉升
//                BigDecimal trim = diff.divide(new BigDecimal(forwardNum), SCALE, BigDecimal.ROUND_DOWN);
//                BigDecimal over = diff.subtract(trim.multiply(new BigDecimal(forwardNum)));
//                ranges = ranges.stream().map(e -> e.compareTo(BigDecimal.ZERO) > 0 ? e.add(trim) : e).collect(Collectors.toList());
//                int randomIndex = random.nextInt(ranges.size());
//                ranges.set(randomIndex, ranges.get(randomIndex).add(over));
//                needAdjust = false;
//            }
//        } else {
//            if (reverseNum > 0) {
//                // 高了，需要压低
//                BigDecimal trim = diff.divide(new BigDecimal(reverseNum), SCALE, BigDecimal.ROUND_DOWN);
//                BigDecimal over = diff.subtract(trim.multiply(new BigDecimal(reverseNum)));
//                ranges = ranges.stream().map(e -> e.compareTo(BigDecimal.ZERO) < 0 ? e.add(trim) : e).collect(Collectors.toList());
//
//                int randomIndex = random.nextInt(ranges.size());
//                ranges.set(randomIndex, ranges.get(randomIndex).add(over));
//                needAdjust = false;
//            }
//        }
//
//        // 平缓调整
//        if (needAdjust) {
//            // 判断调整后的结果是否还需要调整
//            totalRange = BigDecimal.ZERO;
//            for (BigDecimal temp : ranges) {
//                totalRange = totalRange.add(temp);
//            }
//            diff = BigDecimal.ONE.subtract(totalRange);
//            needAdjust = diff.compareTo(BigDecimal.ZERO) != 0;
//        }
//
//        // 平缓调整
//        if (needAdjust) {
//            BigDecimal trim = diff.divide(new BigDecimal(ranges.size()), SCALE, BigDecimal.ROUND_DOWN);
//            BigDecimal over = diff.subtract(trim.multiply(new BigDecimal(ranges.size())));
//            ranges = ranges.stream().map(e -> e.add(trim)).collect(Collectors.toList());
//
//            int randomIndex = random.nextInt(ranges.size());
//            ranges.set(randomIndex, ranges.get(randomIndex).add(over));
//        }
        return ranges;
    }

    /**
     * 随机结算趋势
     * @param stage
     * @param adr
     * @return
     */
    private static List<Integer> randomCycleTrend(int stage, int[] adr) {
        List<Integer> symbols = new ArrayList<>();
        if (stage == 1) {
            symbols.add(1);
            return symbols;
        }
        if (stage == 2) {
            symbols.add(1);
            symbols.add(1);
            return symbols;
        }

        int riseNum = (stage * adr[1]) / (adr[0] + adr[1]) - 2;
        int dropNum = stage - 2 - riseNum;

        for (int i = 0, size = stage - 2; i < size; i++) {
            if (size - i < riseNum) {
                // 涨
                symbols.add(1);
                riseNum--;
            } else {
                if (dropNum > 0 && random.nextBoolean()) {
                    // 跌
                    symbols.add(-1);
                    dropNum--;
                    continue;
                }
                // 横盘
                symbols.add(0);
            }
        }
        Collections.shuffle(symbols);
        symbols.add(0, 1);
        symbols.add(1);
        return symbols;
    }

    public static void main(String[] args) {
        int[] adr = new int[2];
        adr[0] = 2;
        adr[1] = 1;
//        randomCycleRanges(1,15,adr);
//        randomCycleTargetPrices(null,new BigDecimal("4.83"),15,adr,1,new BigDecimal("4.85"));
    }

}

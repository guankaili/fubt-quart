package com.fubt.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 见习操盘手
 *
 * @author Jack
 * @since 2019-04-08
 */
public class TraineeTrader {

    private static Random random = new Random();
    // 小数点保留位数
    public static final int SCALE = 9;
    private static final double MAX_RATIO = 0.004d;

    /**
     * 价格间隔【单位：秒】
     */
    public static final int PRICE_INTERVAL_SECONDS = 5;

    /**
     * 价差间隔【单位：秒】
     */
    public static final int DIFF_INTERVAL_SECONDS = 60;

    /**
     * 每分钟价格个数
     */
    public static final int PRICE_SIZE_FOR_ONE_MINUTE = 60 / PRICE_INTERVAL_SECONDS;

//    public static void main(String[] args) {
//        BigDecimal targetPrice = new BigDecimal("0.000464");
//        int cycle = 134;
//        int level = 0;
//        for (int i = 0; i < cycle; i++) {
//            if (Math.pow(3, i) * 5 <= cycle) {
//                level=i;
//            } else {
//                break;
//            }
//        }

//        for (int q = 0; q < 5; q++) {
//        BigDecimal marketPrice = new BigDecimal("0.000453");
//        List<BigDecimal> prices = randomPrices(marketPrice, targetPrice, cycle, new int[]{2, 1}, 6);
//        BigDecimal open = BigDecimal.ZERO;
//        BigDecimal high = BigDecimal.ZERO;
//        BigDecimal low = BigDecimal.ZERO;
//        BigDecimal close = BigDecimal.ZERO;
//        BigDecimal price;
//        for (int i = 0, size = prices.size(); i < size; ) {
//            for (int j = 0; j < 12; j++) {
//                price = prices.get(i);
//                if (j == 0) {
//                    open = price;
//                    high = price;
//                    low = price;
//                }
//                if (high.compareTo(price) < 0) {
//                    high = price;
//                }
//                if (low.compareTo(price) > 0) {
//                    low = price;
//                }
//                close = price;
//                i++;
//            }
//            System.out.println(open+ "," + high + "," + low + "," + close);
//        }
//        }
//    }

    /**
     * 随机恢复
     * @param sysMarket 平台市价
     * @param outsideMarket 外网市价
     * @param cycle 恢复周期
     * @return 各阶段价差
     */
    public static List<BigDecimal> randomRecover(BigDecimal sysMarket, BigDecimal outsideMarket, int cycle) {
        BigDecimal marketDiff = sysMarket.subtract(outsideMarket);
        return randomCycleTargetPrices(marketDiff, cycle * 60, new int[]{1, 1}, DIFF_INTERVAL_SECONDS, BigDecimal.ZERO);
    }

    /**
     * 均值恢复
     * @param sysMarket 平台市价
     * @param outsideMarket 外网市价
     * @param cycle 恢复周期
     * @return 各阶段价差
     */
    public static List<BigDecimal> avgRecover(BigDecimal sysMarket, BigDecimal outsideMarket, long cycle) {
        BigDecimal marketDiff = sysMarket.subtract(outsideMarket);
        long size = cycle * 60 / DIFF_INTERVAL_SECONDS;
        BigDecimal cycleRecover = marketDiff.divide(new BigDecimal(size), 8, BigDecimal.ROUND_DOWN);
        List<BigDecimal> marketDiffs = new ArrayList<>();
        marketDiffs.add(marketDiff);
        BigDecimal curDiff = marketDiff;
        for (int i = 0; i < size; i++) {
            curDiff = curDiff.subtract(cycleRecover);
            marketDiffs.add(curDiff);
        }
        return marketDiffs;
    }

    /**
     * 生成价格列表
     * @param marketPrice 当前市价
     * @param targetPrice 目标价格
     * @param cycle 周期
     * @param adr 涨跌比
     * @param scale 小数点位数
     * @return 价格列表
     */
    public static List<BigDecimal> randomPrices(BigDecimal marketPrice, BigDecimal targetPrice, int cycle, int[] adr, int scale) {
        List<BigDecimal> prices = randomCycleTargetPrices(marketPrice, cycle, adr, 1, targetPrice);
         List<BigDecimal> pricesList = randomFillPrice(adr, prices, scale);
         return pricesList;
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
    private static List<BigDecimal> randomCycleTargetPrices(BigDecimal marketPrice, int cycle, int[] adr, int minCycle, BigDecimal targetPrice) {
        //
        List<BigDecimal> ranges = randomCycleRanges(minCycle, cycle, adr);

        BigDecimal curPrice = marketPrice;
        BigDecimal priceDiff = targetPrice.compareTo(marketPrice) != 0 ? targetPrice.subtract(marketPrice) : marketPrice.multiply(getRatio()).setScale(SCALE, BigDecimal.ROUND_DOWN);
        BigDecimal minPrice;
        BigDecimal maxPrice;

        List<BigDecimal> prices = new ArrayList<>();
        prices.add(marketPrice);
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
                if (marketPrice.compareTo(targetPrice) != 0) {
                    BigDecimal curMin;
                    BigDecimal curMax;
                    // 设置阶段的最大价格和最小价格
                    if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
                        curMin = marketPrice.add(avgDiff.multiply(new BigDecimal(i - len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                        curMax = marketPrice.add(avgDiff.multiply(new BigDecimal(i + len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                    } else {
                        curMin = marketPrice.add(avgDiff.multiply(new BigDecimal(i + len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                        curMax = marketPrice.add(avgDiff.multiply(new BigDecimal(i - len))).setScale(SCALE, BigDecimal.ROUND_DOWN);
                    }
                    // 存在调整价差
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

                    prices.add(adjustedPrice);
                } else {
                    prices.add(curPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : curPrice);
                }
            }
        }
        prices.add(targetPrice);

        return prices;
    }

    /**
     * 价格填充
     * @param adr 涨跌比
     * @param prices 各个阶段的目标价格
     * @return 填充后的价格列表
     */
    private static List<BigDecimal> randomFillPrice(int[] adr, List<BigDecimal> prices, int scale) {
        // 每分钟价格数
        List<BigDecimal> filledPrices = new ArrayList<>( );
        for (int i = 1, size = prices.size(); i < size; i++) {
            filledPrices.addAll(randomCyclePrices(prices.get(i-1), prices.get(i), scale));
        }
        return filledPrices;
    }

    /**
     * 随机生成某个阶段的价格列表【一分钟12个价格】
     * @param openPrice 开盘价
     * @param closePrice 收盘价
     * @return 填充后的价格列表
     */
    private static List<BigDecimal> randomCyclePrices(BigDecimal openPrice, BigDecimal closePrice, int scale) {
        if (openPrice.compareTo(closePrice) == 0) {
            // T线图或者十字星
            switch (random.nextInt(3)) {
                case 0:// 十字星
                    return randomDefaultPoint(openPrice, closePrice, getRatio(), getRatio(), scale);
                case 1:// T线
                    return randomDefaultPoint(openPrice, closePrice, BigDecimal.ZERO, getRatio(), scale);
                default:// 倒T线
                    return randomDefaultPoint(openPrice, closePrice, getRatio(), BigDecimal.ZERO, scale);
            }
        }

        switch (random.nextInt(4)) {
            case 0:// 光头
                return randomDefaultPoint(openPrice, closePrice, BigDecimal.ZERO, BigDecimal.ZERO, scale);
            case 1:
                return randomDefaultPoint(openPrice, closePrice, getRatio(), getRatio(), scale);
            case 2:// 榔头
                return randomDefaultPoint(openPrice, closePrice, BigDecimal.ZERO, getRatio(), scale);
            default:// 倒榔头
                return randomDefaultPoint(openPrice, closePrice, getRatio(), BigDecimal.ZERO, scale);
        }
    }

    /**
     * 生成点位
     * @param openPrice 开盘价
     * @param closePrice 收盘价
     * @param riseRatio 涨幅
     * @param fallRatio 跌幅
     * @param scale 小数点精度
     * @return 点位价格列表
     */
    private static List<BigDecimal> randomDefaultPoint(BigDecimal openPrice, BigDecimal closePrice,
                                                       BigDecimal riseRatio, BigDecimal fallRatio, int scale) {
        BigDecimal highPrice;
        BigDecimal lowPrice;
        openPrice = openPrice.setScale(scale, BigDecimal.ROUND_HALF_UP);
        closePrice = closePrice.setScale(scale, BigDecimal.ROUND_HALF_UP);
        // 最小价位
        BigDecimal minDiff = new BigDecimal("0.1").pow(scale);
        // 价位
        int price = 2;
        if (openPrice.compareTo(closePrice) == 0) {
            // 随机调整
            BigDecimal adjustDiff = minDiff.multiply(new BigDecimal(random.nextInt(price*2 + 1) - price));
            openPrice = openPrice.add(adjustDiff).setScale(scale, BigDecimal.ROUND_HALF_UP);
        }

        // 计算最高价、最低价
        if (openPrice.compareTo(closePrice) > 0) {
            highPrice = openPrice.multiply(BigDecimal.ONE.add(riseRatio)).setScale(scale, BigDecimal.ROUND_HALF_UP);
            lowPrice = closePrice.multiply(BigDecimal.ONE.subtract(fallRatio)).setScale(scale, BigDecimal.ROUND_HALF_UP);
        } else {
            highPrice = closePrice.multiply(BigDecimal.ONE.add(riseRatio)).setScale(scale, BigDecimal.ROUND_HALF_UP);
            lowPrice = openPrice.multiply(BigDecimal.ONE.subtract(fallRatio)).setScale(scale, BigDecimal.ROUND_HALF_UP);
        }

        if (highPrice.compareTo(lowPrice) == 0) {
            // 最高价=最低价
            BigDecimal addDiff = minDiff.multiply(new BigDecimal(random.nextInt(price) + 1));
            highPrice = highPrice.add(addDiff).setScale(scale, BigDecimal.ROUND_HALF_UP);
            BigDecimal subDiff = minDiff.multiply(new BigDecimal(random.nextInt(price) + 1));
            lowPrice = lowPrice.compareTo(subDiff) > 0 ? lowPrice.subtract(subDiff).setScale(scale, BigDecimal.ROUND_HALF_UP) : lowPrice;
        }



        return randomPointPrices(openPrice, highPrice, lowPrice, closePrice, scale);
    }

    /**
     * 随机产生点位的价格列表
     * @param open 开盘价
     * @param high 最高
     * @param low 最低
     * @param close 收盘价
     */
    private static List<BigDecimal> randomPointPrices(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, int scale) {
        List<BigDecimal> prices = new ArrayList<>();

        // TODO 可细分为5至6个阶段
        // 分为3个阶段，每个阶段的价格数
        int firstSize = random.nextInt(3) + 1;
        int lastSize = firstSize == 3 ? 1 : random.nextInt(3) + 1;
        // 去掉开盘、收盘、最高、最低四个价格
        int middleSize = PRICE_SIZE_FOR_ONE_MINUTE - 4 - firstSize - lastSize;

        prices.add(open);
        if (open.compareTo(close) > 0) {
            prices.addAll(genPointStagePrices(open, high, firstSize, scale));
            prices.addAll(genPointStagePrices(high, low, middleSize, scale));
            prices.addAll(genPointStagePrices(low, close, lastSize, scale));
        } else {
            prices.addAll(genPointStagePrices(open, low, firstSize, scale));
            prices.addAll(genPointStagePrices(low, high, middleSize, scale));
            prices.addAll(genPointStagePrices(high, close, lastSize, scale));
        }
        return prices;
    }

    /**
     * 生成各个阶段的价格列表
     * @param start 开始价格
     * @param end 结束价格
     * @param size 价格数
     * @return 价格列表
     */
    private static List<BigDecimal> genPointStagePrices(BigDecimal start, BigDecimal end, int size, int scale) {
        List<BigDecimal> prices = new ArrayList<>();
        BigDecimal diff = end.subtract(start).divide(new BigDecimal(size), scale, BigDecimal.ROUND_DOWN);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            // 最小价位
            BigDecimal minDiff = new BigDecimal("0.1").pow(scale);
            diff = minDiff.multiply(new BigDecimal(random.nextInt(7) - 3)).setScale(scale, BigDecimal.ROUND_HALF_UP);
        }
        BigDecimal curPrice = start;
        for (int i = 0; i < size; i++) {
            BigDecimal curDiff = diff.multiply(new BigDecimal(random.nextDouble() * 0.6 + 0.3)).setScale(scale, BigDecimal.ROUND_DOWN);
            curPrice = curPrice.add(curDiff);
            prices.add(curPrice);
        }
        prices.add(end);
        return prices;
    }

    private static BigDecimal getRatio() {
        return new BigDecimal(random.nextDouble() * MAX_RATIO).setScale(SCALE, BigDecimal.ROUND_DOWN);
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
        BigDecimal diff = BigDecimal.ONE.subtract(totalRange);
        boolean needAdjust = diff.compareTo(BigDecimal.ZERO) != 0;

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            if (forwardNum > 0) {
                // 低了，需要拉升
                BigDecimal trim = diff.divide(new BigDecimal(forwardNum), SCALE, BigDecimal.ROUND_DOWN);
                BigDecimal over = diff.subtract(trim.multiply(new BigDecimal(forwardNum)));
                ranges = ranges.stream().map(e -> e.compareTo(BigDecimal.ZERO) > 0 ? e.add(trim) : e).collect(Collectors.toList());
                int randomIndex = random.nextInt(ranges.size());
                ranges.set(randomIndex, ranges.get(randomIndex).add(over));
                needAdjust = false;
            }
        } else {
            if (reverseNum > 0) {
                // 高了，需要压低
                BigDecimal trim = diff.divide(new BigDecimal(reverseNum), SCALE, BigDecimal.ROUND_DOWN);
                BigDecimal over = diff.subtract(trim.multiply(new BigDecimal(reverseNum)));
                ranges = ranges.stream().map(e -> e.compareTo(BigDecimal.ZERO) < 0 ? e.add(trim) : e).collect(Collectors.toList());

                int randomIndex = random.nextInt(ranges.size());
                ranges.set(randomIndex, ranges.get(randomIndex).add(over));
                needAdjust = false;
            }
        }

        // 平缓调整
        if (needAdjust) {
            // 判断调整后的结果是否还需要调整
            totalRange = BigDecimal.ZERO;
            for (BigDecimal temp : ranges) {
                totalRange = totalRange.add(temp);
            }
            diff = BigDecimal.ONE.subtract(totalRange);
            needAdjust = diff.compareTo(BigDecimal.ZERO) != 0;
        }

        // 平缓调整
        if (needAdjust) {
            BigDecimal trim = diff.divide(new BigDecimal(ranges.size()), SCALE, BigDecimal.ROUND_DOWN);
            BigDecimal over = diff.subtract(trim.multiply(new BigDecimal(ranges.size())));
            ranges = ranges.stream().map(e -> e.add(trim)).collect(Collectors.toList());

            int randomIndex = random.nextInt(ranges.size());
            ranges.set(randomIndex, ranges.get(randomIndex).add(over));
        }
        return ranges;
    }

    public static void main(String[] args) {
        int[] adr = new int[2];
        adr[0] = 2;
        adr[1] = 1;
        randomPrices(new BigDecimal("4.881"),new BigDecimal("4.89"),5,adr,8);
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
}

package com.fubt.utils;

import java.math.BigDecimal;
import java.util.Random;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1411:17 PM
 */
public class NumberUtils {

    /**
     * 获取指定区间的随机数
     * @param min
     * @param max
     * @return
     */
    public static double getRandom(double min,double max){
        double num = 0d;
        try{
            java.util.Random rand = new Random();
            num = rand.nextDouble()*(max-min)+min;
        }catch(Exception e){
            e.printStackTrace();
        }
        return num;
    }

    public static double mul(double v1,double v2, int rounddown){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2).setScale(rounddown, BigDecimal.ROUND_DOWN).doubleValue();
    }
}

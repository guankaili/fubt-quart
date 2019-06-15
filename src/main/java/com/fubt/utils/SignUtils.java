package com.fubt.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1411:37 PM
 */
public class SignUtils {

    public Logger logger = LoggerFactory.getLogger(getClass());

    public static String jsonToString(String json){
        JSONObject jsonObject = JSON.parseObject(json);
        Map<String, Object> treeMap = new TreeMap<>();
        jsonObject.forEach((s, o) -> treeMap.put(s, o));
        StringBuilder result = new StringBuilder();
        treeMap.forEach((s, o) -> {
            StringBuilder stringBuilder = new StringBuilder();
            if (o instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) o;
                jsonArray.forEach(o1 -> {
                    stringBuilder.append(o1).append(",");
                });
                stringBuilder.deleteCharAt(stringBuilder.length() -1);
            } else if (o instanceof JSONObject) {
                stringBuilder.append(jsonToString(((JSONObject) o).toJSONString()));
            } else {
                stringBuilder.append(o);
            }
            result.append(s).append("=").append(stringBuilder.toString()).append("&");
        });
        result.deleteCharAt(result.length() -1);
        return result.toString();
    }

    /**
     * sha256_HMAC加密
     *
     * @param message 消息
     * @param secret 秘钥
     * @return 加密后字符串
     */
    public static String sha256_HMAC(String message, String secret) {
        String hash = "";
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes());
            hash = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hash;
    }

    public static void main(String[] args) {
        String url = "https://xxx/v1/order/saveEntrust";
        String accessKey = "accessKey xxx";
        String secretKey = "secretKey xxx";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("count", "100.0");
        jsonObject.put("accessKey", accessKey.trim());
        jsonObject.put("matchType", "limit");
        jsonObject.put("payPwd", "123456");
        jsonObject.put("price", 0.206);
        jsonObject.put("type", "buy");
        jsonObject.put("symbol", "fucfbt");

        String signString = SignUtils.jsonToString(jsonObject.toJSONString());
        System.out.println("访问秘钥:" + accessKey);
        System.out.println("私钥secretKey: "+secretKey);
        System.out.println("被加密的字符串: " + signString);
        String signature = SignUtils.sha256_HMAC(signString, secretKey);
        System.out.println("签名: " + signature);
        jsonObject.put("signature",signature);
        System.out.println("最终json数据是: " + jsonObject.toJSONString());
    }

}

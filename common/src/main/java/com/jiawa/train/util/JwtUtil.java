package com.jiawa.train.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JwtUtil.class);

    private static final String key = "Jiawa12306";

    public static String createToken(Long id, String mobile){
        // 设置时间
        // token签发时间，token过期时间，token生效时间
        DateTime now = DateTime.now();
        DateTime expTime = now.offsetNew(DateField.SECOND, 10);


        // playload：上述三个时间，id，mobile
        Map<String, Object> playLoad = new HashMap<>();

        playLoad.put(JWTPayload.ISSUED_AT, now);
        playLoad.put(JWTPayload.EXPIRES_AT, expTime);
        playLoad.put(JWTPayload.NOT_BEFORE, now);

        playLoad.put("id", id);
        playLoad.put("mobile", mobile);

        String token = JWTUtil.createToken(playLoad, key.getBytes(StandardCharsets.UTF_8));
        LOG.info("createToken: {}", token);
        return token;
    }

    public static boolean validate(String token){
        JWT jwt = JWTUtil.parseToken(token).setKey(key.getBytes());
        boolean validate = jwt.validate(0);
        LOG.info("validate: {}", validate);
        return validate;
    }

    public static JSONObject getJwtPlayLoads(String token){
        JWT jwt = JWTUtil.parseToken(token).setKey(key.getBytes());
        JSONObject payloads = jwt.getPayloads();
        payloads.remove(JWTPayload.ISSUED_AT);
        payloads.remove(JWTPayload.EXPIRES_AT);
        payloads.remove(JWTPayload.NOT_BEFORE);
        LOG.info("getJwtPlayLoads: {}", payloads);
        return payloads;
    }

    public static void main(String[] args) {
        String token = createToken(1L, "1309276845");
        boolean validate = validate(token);
        JSONObject jwtPlayLoads = getJwtPlayLoads(token);

    }

}

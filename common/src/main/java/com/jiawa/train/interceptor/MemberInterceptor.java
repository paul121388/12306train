package com.jiawa.train.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.resp.MemberLoginResp;
import com.jiawa.train.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 会员拦截器，用于获取登录后的会员的信息
 */
@Component
public class MemberInterceptor implements HandlerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(MemberInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if(StrUtil.isNotBlank(token)){
            LOG.info("获得member的登录信息：{}",token);
            JSONObject loginMember = JwtUtil.getJSONObject(token);
            LOG.info("当前登录会员：{}",loginMember);
            MemberLoginResp memberLoginResp = JSONUtil.toBean(loginMember, MemberLoginResp.class);
            LoginMemberContext.setMember(memberLoginResp);
        }
        return true;
    }
}

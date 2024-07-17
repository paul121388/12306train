package com.jiawa.train.gateway.config;

import com.jiawa.train.gateway.Util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoginMemberFilter implements GlobalFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LoginMemberFilter.class);

    // 1. 获取请求的路径
    // 2. 根据路径判断是否需要登录，排除不需要拦截的请求
    // 3. 获取token，判断token是否为空
    // 4. 对token进行校验
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.contains("/admin")
                || path.contains("/business")
                || path.contains("/batch")
                || path.contains("/member/member/hello")
                || path.contains("/member/member/login")
                || path.contains("/member/member/send-code")) {
            LOG.info("不需要登录校验: {}", path);
            return chain.filter(exchange);
        } else {
            LOG.info("需要登录校验: {}", path);
        }

        String token = exchange.getRequest().getHeaders().getFirst("token");
        LOG.info("会员登录验证开始，token: {}", token);
        if (token == null || token.isEmpty()) {
            LOG.info("会员登录验证失败，token为空,拦截");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        boolean validate = JwtUtil.validate(token);
        if (validate) {
            LOG.info("token有效，放行该请求");
            return chain.filter(exchange);
        } else {
            LOG.info("token无效，拦截");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

    }


}

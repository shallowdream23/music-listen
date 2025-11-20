//package com.yi.musiclisten.interceptor;
//
//import com.yi.musiclisten.utils.JWTUtils;
//import io.jsonwebtoken.Claims;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * JWT 登录拦截器
// */
//@Component
//public class JwtInterceptor implements HandlerInterceptor {
//
//    // 可以在这里配置不拦截的路径
//    private static final String[] EXCLUDE_PATHS = {
//            "auth/login",
//            "auth/register",
//    };
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String requestURI = request.getRequestURI();
//
//        // 放行部分路径
//        for (String path : EXCLUDE_PATHS) {
//            if (requestURI.startsWith(path)) {
//                return true;
//            }
//        }
//
//        String token = request.getHeader("Authorization");
//        if (token == null || token.isEmpty()) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().write("缺少 Token");
//            return false;
//        }
//
//        try {
//            Claims claims = JWTUtils.parseToken(token);
//
//            Map<String, Object> newClaims = new HashMap<>(claims);
//            String newToken = JWTUtils.generateToken(newClaims);
//            response.setHeader("Authorization", newToken);
//
//            request.setAttribute("userId", claims.get("userId"));
//            request.setAttribute("role", claims.get("role"));
//
//            return true;
//
//        } catch (Exception e) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().write("Token 无效或已过期");
//            return false;
//        }
//    }
//}

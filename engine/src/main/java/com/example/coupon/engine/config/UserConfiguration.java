package com.example.coupon.engine.config;


import com.example.coupon.engine.common.context.UserContext;
import com.example.coupon.engine.common.context.UserInfoDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Nullable;

@Configuration
public class UserConfiguration implements WebMvcConfigurer {

    @Bean
    public UserTransmitInterceptor userTransmitInterceptor() {
        return new UserTransmitInterceptor();
    }

    /**
     * 添加拦截器拦截路径
     * @param registry
     */
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new UserTransmitInterceptor()).addPathPatterns("/**");
    }

    /**
     * 用户信息传输拦截器
     */
    static class UserTransmitInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
            // 用户属于非核心功能，这里先通过模拟的形式代替。后续如果需要后管展示，会重构该代码
            UserInfoDTO userInfoDTO = new UserInfoDTO("1810518709471555585", "pdd45305558318", 1810714735922956666L);
            UserContext.setUser(userInfoDTO);
            return true;
        }

        @Override
        public void afterCompletion(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, @Nullable Exception ex) throws Exception {
            UserContext.removeUser();
        }


    }

}

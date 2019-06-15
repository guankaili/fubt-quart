package com.fubt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>@Description: </p>
 *
 * @date 2018/8/15上午10:54
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

//    @Bean
//    public TokenAuthInterceptor getTokenAuthInterceptor() {
//        return new TokenAuthInterceptor();
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(getTokenAuthInterceptor());
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
    }

}
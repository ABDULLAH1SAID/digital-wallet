package com.example.digitalwallet.common.security;

import com.example.digitalwallet.common.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        String header = webRequest.getHeader("X-User-Id");

        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("X-User-Id header is missing");
        }
        try {
            long userId = Long.parseLong(header.trim());
            if (userId <= 0) {
                throw new UnauthorizedException("X-User-Id must be a positive number");
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("X-User-Id must be a valid number");
        }
    }
}
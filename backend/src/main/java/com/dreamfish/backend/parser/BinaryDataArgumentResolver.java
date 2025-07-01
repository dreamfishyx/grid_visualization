package com.dreamfish.backend.parser;

import com.dreamfish.backend.parser.annotation.BinaryRequestBody;
import com.dreamfish.backend.parser.exception.ProtocolException;
import com.dreamfish.backend.parser.exception.ProtocolParseException;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.InputStream;
import java.util.Objects;


/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2025/4/3 16:43
 */
public class BinaryDataArgumentResolver implements HandlerMethodArgumentResolver {
    // 初始化一个解析器 BinaryDataParser
    private final BinaryDataParser parser = new BinaryDataParser();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 参数含有 BinaryRequestBody 注解,则支持解析该参数
        return parameter.hasParameterAnnotation(BinaryRequestBody.class);
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        byte[] body = null;
        if (request != null) {
            try (InputStream inputStream = request.getInputStream()) {
                body = inputStream.readAllBytes();
            }
        }
        // 判断注解是否标注在自定义的类上
        if (body == null || body.length == 0) {
            throw new ProtocolParseException("Request body is null or empty when parsing parameter: " + parameter.getParameterName());
        }
        // 判断参数类型是否为自定义的类: 后面会判断是否使用了 BinaryRequestBody 注解,略


        // 获取 BinaryRequestBody 注解
        BinaryRequestBody annotation = parameter.getParameterAnnotation(BinaryRequestBody.class);
        BinaryRequestBody.ByteOrder order = Objects.requireNonNull(annotation).byteOrder();
        // 获取当前分组
        Class<?> group = annotation.group();
        if (group == null) {
            throw new ProtocolException("Annotation " + BinaryRequestBody.class.getName() + " group cannot be null");
        }
        // 存在默认值,不会为 null
        return parser.parse(body, parameter.getParameterType(), group, order);
    }
}
package com.agentgate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示用上游服务：供 /openapi/** 转发，不参与鉴权。
 */
@RestController
@RequestMapping("/mock-api")
public class MockApiController {

    @GetMapping("/weather")
    public Map<String, Object> weather(@RequestParam(defaultValue = "beijing") String city) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("city", city);
        data.put("weather", "sunny");
        data.put("temperature", 26);
        data.put("humidity", 45);
        data.put("provider", "mock-upstream");
        return data;
    }

    @GetMapping("/order/{id}")
    public Map<String, Object> order(@PathVariable String id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", id);
        data.put("status", "PAID");
        data.put("amount", 199.00);
        data.put("phone", "13812345678");
        data.put("email", "buyer@example.com");
        data.put("address", "北京市朝阳区某某街道 100 号");
        return data;
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> user(@PathVariable String id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", id);
        data.put("name", "张三");
        data.put("mobile", "13900001111");
        data.put("idCard", "110101199001011234");
        data.put("email", "zhangsan@example.com");
        data.put("bankCard", "6222020202020202020");
        data.put("address", "上海市浦东新区某某路 88 号");
        return data;
    }
}

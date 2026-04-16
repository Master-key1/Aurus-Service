package com.auruspay.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/aurus")
public class ZabaxCallController {

    @PostMapping("/api")
    public String callApi() {

        RestTemplate restTemplate = new RestTemplate();

        String url = "http://192.168.187.177:8888/get_logs";

        Map<String, String> request = new HashMap();
        request.put("ip", "192.168.180.51");
        request.put("txn_id", "1c3ced78-c720-4d8f-8016-0aa0ff08a69e");
        request.put("log_file", "/opt/auruspay_switch/log/trace/trace.log-2026-04-03");

        return restTemplate.postForObject(url, request, String.class);
    }
}
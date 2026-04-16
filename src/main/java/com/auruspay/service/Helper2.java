package com.auruspay.service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auruspay.decryptor.AurusDecryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Service
public class Helper2 {

    private static final String DATE_PATTERN = "";

    private static final Map<String, String> TXN_HOST_MAP = new HashMap<>();

    static {
        TXN_HOST_MAP.put("95", "192.168.50.155");
        TXN_HOST_MAP.put("97", "192.168.50.69");
    }

    @Value("${ssh.jump.host}")
    private String jumpHost;

    @Value("${ssh.jump.port}")
    private int jumpPort;

    @Value("${ssh.jump.user}")
    private String jumpUser;

    @Value("${ssh.jump.pass}")
    private String jumpPass;

    @Value("${ssh.target.user}")
    private String targetUser;

    @Value("${ssh.target.pass}")
    private String targetPass;

    public void printConfig() {
        System.out.println("Jump Host: " + jumpHost);
        System.out.println("Jump Port: " + jumpPort);
        System.out.println("Jump User: " + jumpUser);
        System.out.println("Target User: " + targetUser);
    }

    public StringBuffer process(String txnId) {

        printConfig();

        String node = txnId.substring(1, 3);
        String targetHost = TXN_HOST_MAP.get(node);

        if (targetHost == null) {
            return new StringBuffer("Invalid Transaction ID...");
        }

        JSch jsch = new JSch();
        Session jumpSession = null;
        Session targetSession = null;

        try {
            // 🔹 CONNECT JUMP HOST
            jumpSession = jsch.getSession(jumpUser, jumpHost, jumpPort);
            jumpSession.setPassword(jumpPass);
            jumpSession.setConfig("StrictHostKeyChecking", "no");
            jumpSession.connect(15000);

            // 🔹 PORT FORWARDING
            int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, 22);

            // 🔹 CONNECT TARGET
            targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
            targetSession.setPassword(targetPass);
            targetSession.setConfig("StrictHostKeyChecking", "no");
            targetSession.connect(15000);

            String logPath = "/opt/auruspay_switch/log/auruspay/auruspay.log" + DATE_PATTERN;

            // 🔹 STEP 1: SEARCH TXN
            String txnCmd = "zgrep --text '" + txnId + "' " + logPath;
            StringBuffer txnOutput = executeCommand(targetSession, txnCmd);

            if (txnOutput.isEmpty()) {
                return new StringBuffer("Transaction not found");
            }

            // 🔹 STEP 2: EXTRACT UUID
            Matcher matcher = Pattern.compile("([a-f0-9\\-]{36})").matcher(txnOutput);
            String uuid = matcher.find() ? matcher.group() : null;

            if (uuid == null) {
                return new StringBuffer("UUID not found");
            }

            // 🔹 STEP 3: FETCH UUID LOG
            String uuidCmd = "zgrep --text '" + uuid + "' " + logPath;
            StringBuffer uuidOutput = executeCommand(targetSession, uuidCmd);

            // 🔹 STEP 4: PARSE LOG
            Map<String, Object> dataMap = parseLog(uuidOutput, uuid, txnId);
            dataMap.put("LogDetails", uuidOutput.toString());

            // 🔹 JSON OUTPUT
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(dataMap);

            return new StringBuffer(json);

        } catch (Exception e) {
            e.printStackTrace();
            return new StringBuffer("System Error: " + e.getMessage());

        } finally {
            if (targetSession != null && targetSession.isConnected()) {
                targetSession.disconnect();
            }
            if (jumpSession != null && jumpSession.isConnected()) {
                jumpSession.disconnect();
            }
        }
    }

    private StringBuffer executeCommand(Session session, String command) throws Exception {

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        StringBuffer output = new StringBuffer();

        try (InputStream in = channel.getInputStream()) {
            channel.connect();

            byte[] buffer = new byte[1024];

            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer);
                    if (i < 0) break;
                    output.append(new String(buffer, 0, i));
                }

                if (channel.isClosed()) break;
                Thread.sleep(50);
            }

        } finally {
            channel.disconnect();
        }

        return output;
    }

    private Map<String, Object> parseLog(StringBuffer logs, String uuid, String txnId) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("TIMESTAMP", new Date().toString());
        map.put("TXNID", txnId);
        map.put("UUID", uuid);

        String[] logLines = logs.toString().split("\\r?\\n");

        List<String> issues = new ArrayList<>();

        for (String line : logLines) {
            if (!line.contains(uuid)) continue;

            processLine(line, "AURUSPAY ENCRYPTED REQUEST :", "AurusReq", map);
            processLine(line, "[STPL-GRAY-STREAM]- REQUEST :", "ProcReq", map);
            processLine(line, "[STPL-GRAY-STREAM]-FINAL RESPONSE :", "ProcRes", map);
            processLine(line, "AURUSPAY ENCRYPTED RESPONSE :", "AurusRes", map);

            if (line.matches(".*(ERROR|Exception|Timeout|Declined|Failed).*")) {
                issues.add(line.trim());
            }
        }

        map.put("IssuesCount", issues.size());
        return map;
    }

    private void processLine(String line, String marker, String mapKey, Map<String, Object> map) {

        if (line.contains(marker)) {

            String encrypted = line.substring(line.indexOf(marker) + marker.length()).trim();
            String sanitized = encrypted.replaceAll("\\s", "");

            try {
                String decrypted = AurusDecryptor.decryptor(sanitized);
                map.put(mapKey + "Decrypt", decrypted);
            } catch (Exception e) {
                map.put(mapKey + "Decrypt", "DECRYPTION_ERROR: " + e.getMessage());
            }
        }
    }
}
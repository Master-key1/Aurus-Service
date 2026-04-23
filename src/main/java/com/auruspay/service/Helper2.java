package com.auruspay.service;

import com.auruspay.decryptor.AurusDecryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;

import java.io.InputStream;
import java.util.*;
import java.util.regex.*;

import org.springframework.stereotype.Service;

@Service
public class Helper2 {

    private static final String DATE_PATTERN = "";

    public static Map<String, Object> main(String txnId) {

        String jumpHost = "uat42.auruspay.com";
        String jumpUser = "vchavan";
        String jumpPass = "D!g@mb@r$3_2k26!";

        String targetHost = null;

        if (txnId.substring(1, 3).equals("95")) {
            targetHost = "192.168.50.155";
        } else if (txnId.substring(1, 3).equals("91")) {
            targetHost = "192.168.50.152";
        } else if (txnId.substring(1, 3).equals("92")) {
            targetHost = "192.168.50.153";
        } else if (txnId.substring(1, 3).equals("94")) {
            targetHost = "192.168.50.72";
        } else if (txnId.substring(1, 3).equals("93")) {
            targetHost = "192.168.50.172";
        } else if (txnId.substring(1, 3).equals("97")) {
            targetHost = "192.168.50.69";
        } else {
            return Map.of("ERROR", "Invalid Transaction ID");
        }

        String targetUser = "vchavan";
        String targetPass = "K0yN@$3$_2k26!";

        JSch jsch = new JSch();
        Session jumpSession = null;
        Session targetSession = null;

        try {
            jumpSession = jsch.getSession(jumpUser, jumpHost, 22);
            jumpSession.setPassword(jumpPass);
            jumpSession.setConfig("StrictHostKeyChecking", "no");
            jumpSession.connect(15000);

            int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, 22);

            targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
            targetSession.setPassword(targetPass);
            targetSession.setConfig("StrictHostKeyChecking", "no");
            targetSession.connect(15000);

            String logPath = "/opt/auruspay_switch/log/auruspay/auruspay.log" + DATE_PATTERN;

            String txnCmd = "zgrep --text '" + txnId + "' " + logPath;
            StringBuffer txnOutput = executeCommand(targetSession, txnCmd);

            if (txnOutput.isEmpty()) {
                return Map.of("ERROR", "Transaction not found");
            }

            Matcher matcher = Pattern.compile("([a-f0-9\\-]{36})").matcher(txnOutput);
            String uuid = matcher.find() ? matcher.group() : null;

            if (uuid == null) {
                return Map.of("ERROR", "UUID not found");
            }

            String uuidCmd = "zgrep --text -C20 '" + uuid + "' " + logPath;
            StringBuffer uuidOutput = executeCommand(targetSession, uuidCmd);

            Map<String, Object> dataMap = parseLog(uuidOutput, uuid, txnId);
            dataMap.put("LogDetails", uuidOutput.toString());

            return dataMap;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("ERROR", e.getMessage());

        } finally {
            if (targetSession != null) targetSession.disconnect();
            if (jumpSession != null) jumpSession.disconnect();
        }
    }

    private static StringBuffer executeCommand(Session session, String command) throws Exception {

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

    private static Map<String, Object> parseLog(StringBuffer logs, String uuid, String txnId) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("TXNID", txnId);
        map.put("UUID", uuid);

        String[] logLines = logs.toString().split("\\r?\\n");

        for (String line : logLines) {
            if (!line.contains(uuid)) continue;

            processLine(line, "AURUSPAY ENCRYPTED REQUEST :", "AurusReq", map);
            processLine(line, "[STPL-GRAY-STREAM]-PROCESSOR REQUEST :", "ProcReq", map);
            processLine(line, "[STPL-GRAY-STREAM]-PROCESSOR RESPONSE :", "ProcRes", map);
            processLine(line, "AURUSPAY ENCRYPTED RESPONSE :", "AurusRes", map);
        }

        return map;
    }

    private static void processLine(String line, String marker, String mapKey, Map<String, Object> map) {

        if (line.contains(marker)) {

            String encrypted = line.substring(line.indexOf(marker) + marker.length()).trim();
            String sanitized = encrypted.replaceAll("\\s", "");

            try {
                String decrypted = AurusDecryptor.decryptor(sanitized);

                decrypted = decrypted.replace("\\\"", "\"").replaceAll("^\"|\"$", "");

                Map<String, String> parsed = convertStringToMap(decrypted);

                map.put(mapKey + "Raw", decrypted);
                map.put(mapKey + "Parsed", parsed);

            } catch (Exception e) {
                map.put(mapKey + "Raw", "ERROR: " + e.getMessage());
                map.put(mapKey + "Parsed", Collections.emptyMap());
            }
        }
    }

    private static Map<String, String> convertStringToMap(String input) {

        try {
            if (input == null || input.trim().isEmpty()) {
                return Collections.emptyMap();
            }

            input = input.trim();

            if (!input.endsWith("}")) {
                input = input + "}";
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(input, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}
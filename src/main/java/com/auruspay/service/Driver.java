package com.auruspay.service;

import com.auruspay.AurusServiceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

import org.springframework.stereotype.Service;

@Service
public class Driver {

    private final AurusServiceApplication aurusServiceApplication;

    Driver(AurusServiceApplication aurusServiceApplication) {
        this.aurusServiceApplication = aurusServiceApplication;
    }

    public static StringBuffer main(String txnId) {

        String jumpHost = "uat42.auruspay.com";
        String jumpUser = "vchavan";
        String jumpPass = "Sh!rd!$3_2k26!";

        String targetHost = getTargetHost(txnId);
        if (targetHost == null) {
            return new StringBuffer("Invalid Transaction ID...");
        }

        String targetUser = "vchavan";
        String targetPass = "T!rup@t!_2k26!";

        JSch jsch = new JSch();
        Session jumpSession = null;
        Session targetSession = null;

        try {
            // 🔐 CONNECT JUMP
            jumpSession = jsch.getSession(jumpUser, jumpHost, 22);
            jumpSession.setPassword(jumpPass);
            jumpSession.setConfig("StrictHostKeyChecking", "no");
            jumpSession.connect(15000);

            int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, 22);

            // 🔐 CONNECT TARGET
            targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
            targetSession.setPassword(targetPass);
            targetSession.setConfig("StrictHostKeyChecking", "no");
            targetSession.connect(15000);

            String logPath = "/opt/auruspay_switch/log/auruspay/auruspay.log";

            // 🔍 STEP 1: SEARCH TXN
            String txnCmd = "zgrep --text -C10 '" + txnId + "' " + logPath;
            StringBuffer txnOutput = executeCommand(targetSession, txnCmd);

            if (txnOutput.length() == 0) {
                return new StringBuffer("Transaction not found");
            }

            // 🔍 STEP 2: EXTRACT UUID
            Matcher matcher = Pattern.compile("([a-f0-9\\-]{36})").matcher(txnOutput);
            String uuid = matcher.find() ? matcher.group() : null;

            if (uuid == null) {
                return new StringBuffer("UUID not found");
            }

            // 🔍 STEP 3: FETCH UUID LOGS
            String uuidCmd = "zgrep --text '" + uuid + "' " + logPath;
            StringBuffer uuidOutput = executeCommand(targetSession, uuidCmd);

            // 🔍 STEP 4: PARSE LOG
            Map<String, Object> dataMap = parseLog(uuidOutput, uuid, txnId);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dataMap);

            // 🔥 STEP 5: FLOW ANALYSIS
            String flowAnalysis = analyzeFlow(uuidOutput);

            // ✅ FINAL RESPONSE
            return new StringBuffer(jsonOutput + "\n" + flowAnalysis);

        } catch (Exception e) {
            e.printStackTrace();
            return new StringBuffer("Error: " + e.getMessage());
        } finally {
            if (targetSession != null) targetSession.disconnect();
            if (jumpSession != null) jumpSession.disconnect();
        }
    }

    // ===============================
    // 🔥 FLOW ANALYSIS
    // ===============================
    private static String analyzeFlow(StringBuffer logs) {

        Set<String> flowSet = new LinkedHashSet<>();
        Map<String, Integer> freqMap = new LinkedHashMap<>();
        List<String> currentFlow = new ArrayList<>();

        StringBuilder result = new StringBuilder();

        List<String> expectedFlow = Arrays.asList(
                "class :ProcessAurusTransaction Method :validateAurusRestRequest",
                "class :ContextMapper Method :createContext",
                "class :AurusISOMux Method :sendMsg"
        );

        String[] lines = logs.toString().split("\\r?\\n");

        for (String line : lines) {
            String extracted = extract(line);

            if (extracted != null) {
                currentFlow.add(extracted);
                flowSet.add(extracted);
                freqMap.put(extracted, freqMap.getOrDefault(extracted, 0) + 1);
            }
        }

        result.append("\n========== FLOW ANALYSIS ==========\n");

        result.append("\n📌 Unique Flow:\n");
        flowSet.forEach(s -> result.append(s).append("\n"));

        result.append("\n📊 Frequency:\n");
        freqMap.forEach((k, v) -> result.append(k).append(" -> ").append(v).append("\n"));

        result.append("\n🔍 Missing Steps:\n");
        for (String expected : expectedFlow) {
            if (!currentFlow.contains(expected)) {
                result.append("⚠️ Missing: ").append(expected).append("\n");
            }
        }

        result.append("\n🤖 Anomalies:\n");
        for (int i = 0; i < currentFlow.size() - 1; i++) {

            String current = currentFlow.get(i);
            String next = currentFlow.get(i + 1);

            if (current.contains("validateAurusRestRequest") &&
                    !next.contains("createContext")) {

                result.append("🚨 createContext skipped after validate\n");
            }

            if (current.contains("createContext") &&
                    !next.contains("sendMsg")) {

                result.append("🚨 sendMsg skipped after createContext\n");
            }
        }

        return result.toString();
    }

    // ===============================
    // 🔥 EXTRACT METHOD
    // ===============================
    private static String extract(String line) {

        try {
            int start = line.indexOf("[");
            int end = line.indexOf("]");

            if (start == -1 || end == -1) return null;

            String content = line.substring(start + 1, end);

            String className = content.substring(0, content.indexOf("."));
            String methodName = content.substring(
                    content.indexOf(".") + 1,
                    content.indexOf("(")
            );

            return "class :" + className + " Method :" + methodName;

        } catch (Exception e) {
            return null;
        }
    }

    // ===============================
    // 🔥 EXECUTE COMMAND
    // ===============================
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

    // ===============================
    // 🔥 HOST MAPPING
    // ===============================
    private static String getTargetHost(String txnId) {

        String node = txnId.substring(1, 3);

        switch (node) {
            case "95": return "192.168.50.155";
            case "91": return "192.168.50.152";
            case "92": return "192.168.50.153";
            case "93": return "192.168.50.72";
            case "94": return "192.168.50.172";
            case "97": return "192.168.50.69";
            default: return null;
        }
    }

    // ===============================
    // 🔥 PARSE LOG (SIMPLIFIED)
    // ===============================
    private static Map<String, Object> parseLog(StringBuffer logs, String uuid, String txnId) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("TXNID", txnId);
        map.put("UUID", uuid);

        List<String> issues = new ArrayList<>();

        for (String line : logs.toString().split("\\r?\\n")) {

            if (line.contains("ERROR") || line.contains("Exception")) {
                issues.add(line);
            }
        }

        map.put("IssuesCount", issues.size());
        return map;
    }
}
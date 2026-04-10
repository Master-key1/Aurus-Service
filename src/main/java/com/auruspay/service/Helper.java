package com.auruspay.service;

import com.auruspay.AurusServiceApplication;
import com.auruspay.decryptor.AurusDecryptor;
import com.auruspay.filewriter.ExcelWriter;
import com.auruspay.filewriter.TxtWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

import org.springframework.stereotype.Service;

@Service
public class Helper {

    private final AurusServiceApplication aurusServiceApplication;

    private static final String DATE_PATTERN = "";

    Helper(AurusServiceApplication aurusServiceApplication) {
        this.aurusServiceApplication = aurusServiceApplication;
    }
    
    private static final Map<String, String> TXN_HOST_MAP = new HashMap<>();

    static {
        TXN_HOST_MAP.put("95", "192.168.50.155");
        TXN_HOST_MAP.put("97", "192.168.50.69");
    }


    public static StringBuffer main(String txnId ) {
        // Use Environment Variables or Input for Security - DO NOT HARDCODE IN PROD
        String jumpHost = "uat42.auruspay.com";
        String jumpUser = "vchavan";
        String jumpPass = "Bh@nDup$3_2k26!"; 

        String targetHost = null;
        System.out.println("Node: "+txnId.substring(1,3));
        if(txnId.substring(1,3).equals("95")) {
        	targetHost= "192.168.50.155";
        }else  if(txnId.substring(1,3).equals("97")) {
        	targetHost= "192.168.50.69";
        }else {
        	return new StringBuffer().append("Invalid Transaction ID...");
        }
        
        
        
        
        String targetUser = "vchavan";
        String targetPass = "Ch!nchP0kl!_2k26!";
        StringBuffer uuidOutput =null;

        // "295260853503884501";

        JSch jsch = new JSch();
        Session jumpSession = null;
        Session targetSession = null;

        try {
            // --- CONNECT JUMP HOST ---
            jumpSession = jsch.getSession(jumpUser, jumpHost, 22);
            jumpSession.setPassword(jumpPass);
            jumpSession.setConfig("StrictHostKeyChecking", "no");
            jumpSession.connect(15000); // 15s timeout

            // --- LOCAL PORT FORWARDING ---
            int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, 22);
            System.out.println("Port Forwarded through: " + forwardedPort);

            // --- CONNECT TARGET HOST ---
            targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
            targetSession.setPassword(targetPass);
            targetSession.setConfig("StrictHostKeyChecking", "no");
            targetSession.connect(15000);

            // --- STEP 1 & 2: DYNAMIC TXN SEARCH ---
            // Uses current date to avoid manual updates every day
            String logPathPattern = "/opt/auruspay_switch/log/auruspay/auruspay.log" + DATE_PATTERN ;
            String txnCmd = "zgrep --text '" + txnId + "' " + logPathPattern;
            System.out.println(txnCmd);
            
            StringBuffer txnOutput = executeCommand(targetSession, txnCmd);
            if (txnOutput.isEmpty()) {
                System.err.println("Transaction ID not found in logs for " + DATE_PATTERN);
                return new StringBuffer().append("Transaction ID not found in logs for ").append( DATE_PATTERN );
            }

            // Extract UUID
            Matcher matcher = Pattern.compile("([a-f0-9\\-]{36})").matcher(txnOutput);
            String uuid = matcher.find() ? matcher.group() : null;

            if (uuid == null) {
                System.err.println("UUID could not be parsed from transaction output.");
                return new StringBuffer().append("UUID could not be parsed from transaction output.");
            }
            System.out.println("Target UUID: " + uuid);

            // --- STEP 3: FETCH FULL UUID LOG ---
            String uuidCmd = "zgrep --text '" + uuid + "' " + logPathPattern;
             uuidOutput = executeCommand(targetSession, uuidCmd);
             System.out.println("Log Details: " + uuidOutput);

            // --- STEP 4: PARSE & DECRYPT ---
            Map<String, Object> dataMap = parseLog(uuidOutput, uuid, txnId);
            dataMap.put("LogDetails",uuidOutput );

            // --- STEP 5: PERSISTENCE ---
            String baseFilePath = "C:\\Users\\nkharose\\Documents\\AI Testing task\\" + txnId;
            
            // Excel Output
            try {
                ExcelWriter excelWriter = new ExcelWriter(baseFilePath + ".xlsx");
                excelWriter.addMapAsSingleRow(dataMap);
                excelWriter.save();
                System.out.println("Excel Report Generated.");
            } catch (Exception e) {
                System.err.println("Failed to write Excel: " + e.getMessage());
            }

            // Text Output
            try {
                TxtWriter txtWriter = new TxtWriter(baseFilePath + ".txt");
                txtWriter.writeMapWithLogs(dataMap, uuidOutput);
                System.out.println("Text Logs Generated.");
            } catch (Exception e) {
                System.err.println("Failed to write TXT: " + e.getMessage());
            }
            
       //     Map<String, Object> dataMap = parseLog(uuidOutput, uuid, txnId);

            ObjectMapper objectMapper = new ObjectMapper();

            // Convert to JSON String
            String jsonOut = objectMapper.writerWithDefaultPrettyPrinter()
                                           .writeValueAsString(dataMap);

            System.out.println(jsonOut);
            StringBuffer jsonOutput = new StringBuffer(jsonOut);
            return jsonOutput ;

        } catch (Exception e) {
            System.err.println("Critical System Failure: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (targetSession != null && targetSession.isConnected()) targetSession.disconnect();
            if (jumpSession != null && jumpSession.isConnected()) jumpSession.disconnect();
            System.out.println("SSH Sessions Closed safely.");
        }
        
        return uuidOutput;
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
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(50);
            }
        } finally {
            channel.disconnect();
        }
        return output;
    }

    private static Map<String, Object> parseLog(StringBuffer logs, String uuid, String txnId) {

        Map<String, Object> map = new LinkedHashMap<>(); // preserves order
        map.put("TIMESTAMP", new Date().toString());
        map.put("TXNID", txnId);
        map.put("UUID", uuid);

        // ✅ Convert StringBuffer → String
        String[] logLines = logs.toString().split("\\r?\\n");

        List<String> issues = new ArrayList<>();
        for (String line : logLines) {
            if (!line.contains(uuid)) continue;

            // Helper to extract and decrypt
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

    private static void processLine(String line, String marker, String mapKey, Map<String, Object> map) {
        if (line.contains(marker)) {
            String encrypted = line.substring(line.indexOf(marker) + marker.length()).trim();
            // Critical Fix: Remove whitespace before decryption
            String sanitized = encrypted.replaceAll("\\s", "");
          //  map.put(mapKey + "Encrypt", sanitized);
            try {
                String decrypted = AurusDecryptor.decryptor(sanitized);
                map.put(mapKey + "Decrypt", decrypted);
            } catch (Exception e) {
                map.put(mapKey + "Decrypt", "DECRYPTION_ERROR: " + e.getMessage());
            }
        }
    }
}
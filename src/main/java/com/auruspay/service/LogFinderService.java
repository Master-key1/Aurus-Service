package com.auruspay.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.*;

import com.auruspay.exceptionhandler.NodeNotFountException;

import java.util.logging.Logger;

public class LogFinderService {

    private static final Logger logger = Logger.getLogger(LogFinderService.class.getName());

    private static final String USERNAME = "kevalin";
    private static final String LOG_PATH = "/opt/auruspay_switch/log/auruspay/auruspay.log";

    private final Map<String, String> nodeMap = new HashMap<>();

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F\\-]{36}");

    public LogFinderService() {

        nodeMap.put("91", "192.168.50.152");
        nodeMap.put("92", "192.168.50.153");
        nodeMap.put("93", "192.168.50.172");
        nodeMap.put("94", "192.168.50.72");
        nodeMap.put("95", "192.168.50.155");
        nodeMap.put("96", "192.168.50.196");
        nodeMap.put("97", "192.168.50.69");
    }

    // ================= NODE RESOLUTION =================
    public String resolveNodeIP(String txnId) {

        if (txnId == null || txnId.isEmpty())
            throw new NodeNotFountException("TxnId is missing");

        String nodeKey = txnId.substring(1, 3);

        String ip = nodeMap.get(nodeKey);

        if (ip == null)
            throw new NodeNotFountException("No node mapping found");

        return ip;
    }

    // ================= COMMAND BUILDER =================
    private String buildCommand(String searchValue, String ip,
                                boolean withinOneHour, String formattedHour) {

        if (!withinOneHour) {
            String archivedLog = LOG_PATH + "-" + formattedHour + "*";
            return String.format(
                    "ssh %s@%s \"zgrep --text -C5 '%s' %s\"",
                    USERNAME, ip, searchValue, archivedLog);
        } else {
            return String.format(
                    "ssh %s@%s \"grep --text -C5 '%s' %s\"",
                    USERNAME, ip, searchValue, LOG_PATH);
        }
    }

    // ================= FIND UUID =================
    public String findUUID(String txnId, String ip,
                           boolean withinOneHour, String formattedHour) throws Exception {

        String cmd = buildCommand(txnId, ip, withinOneHour, formattedHour);

        Process process = new ProcessBuilder("bash", "-c", cmd)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.contains(txnId)) {

                    Matcher matcher = UUID_PATTERN.matcher(line);

                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        }

        return null;
    }

    // ================= TRANSACTION MODEL =================
    static class Transaction {

        String uuid;

        String aurusEncryptedRequest;
        String processorRequest;
        String processorResponse;
        String aurusEncryptedResponse;

        String responseCode;
        String responseMessage;
        String authCode;

        String finalStatus;
        String failureReason;

        List<String> issues = new ArrayList<>();
    }

    // ================= MAIN PARSER =================
    public List<Transaction> searchFullTransaction(String uuid,
                                                   String ip,
                                                   boolean withinOneHour,
                                                   String formattedHour) throws Exception {

        List<Transaction> transactions = new ArrayList<>();
        Transaction currentTxn = null;

        boolean captureEncReq = false;
        boolean captureReq = false;
        boolean captureResp = false;
        boolean captureEncResp = false;

        StringBuilder encReq = new StringBuilder();
        StringBuilder req = new StringBuilder();
        StringBuilder resp = new StringBuilder();
        StringBuilder encResp = new StringBuilder();

        String cmd = buildCommand(uuid, ip, withinOneHour, formattedHour);

        Process process = new ProcessBuilder("bash", "-c", cmd)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                Matcher m = UUID_PATTERN.matcher(line);

                if (m.find()) {

                    currentTxn = new Transaction();
                    currentTxn.uuid = m.group();
                    transactions.add(currentTxn);

                    // Reset flags
                    captureEncReq = captureReq = captureResp = captureEncResp = false;
                }

                if (currentTxn == null) continue;

                boolean isTarget = line.contains(currentTxn.uuid);

                // ===== ENCRYPTED REQUEST =====
                if (line.contains("[STPL-GRAY-STREAM]-AURUSPAY ENCRYPTED REQUEST") && isTarget) {

                    captureEncReq = true;
                    encReq.setLength(0);
                    encReq.append(line).append("\n");
                    continue;
                }

                if (captureEncReq) {

                    encReq.append(line).append("\n");

                    if (line.trim().isEmpty()) {
                        captureEncReq = false;
                        currentTxn.aurusEncryptedRequest = encReq.toString();
                    }
                    continue;
                }

                // ===== PROCESSOR REQUEST =====
                if (line.contains("[STPL-GRAY-STREAM]- REQUEST") && isTarget) {

                    captureReq = true;
                    req.setLength(0);
                    req.append(line).append("\n");
                    continue;
                }

                if (captureReq) {

                    req.append(line).append("\n");

                    if (line.contains("</Request>")) {
                        captureReq = false;
                        currentTxn.processorRequest = req.toString();
                    }
                    continue;
                }

                // ===== PROCESSOR RESPONSE =====
                if (line.contains("[STPL-GRAY-STREAM]-FINAL RESPONSE") && isTarget) {

                    captureResp = true;
                    resp.setLength(0);
                    resp.append(line).append("\n");
                    continue;
                }

                if (captureResp) {

                    resp.append(line).append("\n");

                    if (line.contains("<RespCode>"))
                        currentTxn.responseCode = extractXML(line, "RespCode");

                    if (line.contains("<AuthID>"))
                        currentTxn.authCode = extractXML(line, "AuthID");

                    if (line.contains("<AddtlRespData>"))
                        currentTxn.responseMessage = extractXML(line, "AddtlRespData");

                    if (line.contains("</GMF>")) {
                        captureResp = false;
                        currentTxn.processorResponse = resp.toString();
                    }
                    continue;
                }

                // ===== ENCRYPTED RESPONSE =====
                if (line.contains("[STPL-GRAY-STREAM]-AURUSPAY ENCRYPTED RESPONSE") && isTarget) {

                    captureEncResp = true;
                    encResp.setLength(0);
                    encResp.append(line).append("\n");
                    continue;
                }

                if (captureEncResp) {

                    encResp.append(line).append("\n");

                    if (line.trim().isEmpty()) {
                        captureEncResp = false;
                        currentTxn.aurusEncryptedResponse = encResp.toString();
                    }
                    continue;
                }

                // ===== ERROR DETECTION =====
                if (line.matches(".*(ERROR|Exception|Timeout|Declined|Failed).*")) {
                    currentTxn.issues.add(line);
                }
            }
        }

        process.waitFor();

        // ================= AI ANALYSIS =================
        for (Transaction t : transactions) {

            if ("000".equals(t.responseCode)) {
                t.finalStatus = "APPROVED";
                t.failureReason = "NONE";
            }
            else if (t.responseCode != null) {
                t.finalStatus = "DECLINED";
                t.failureReason = "ISSUER_DECLINE";
            }
            else if (!t.issues.isEmpty()) {

                String issue = t.issues.get(0).toLowerCase();

                if (issue.contains("timeout"))
                    t.failureReason = "PROCESSOR_TIMEOUT";
                else if (issue.contains("connection"))
                    t.failureReason = "NETWORK_ISSUE";
                else if (issue.contains("exception"))
                    t.failureReason = "APPLICATION_ERROR";
                else
                    t.failureReason = "UNKNOWN_ERROR";

                t.finalStatus = "FAILED";
            }
            else {
                t.finalStatus = "UNKNOWN";
                t.failureReason = "NO_RESPONSE";
            }
        }

        logger.info("Total transactions parsed: " + transactions.size());

        return transactions;
    }

    // ================= XML HELPER =================
    private String extractXML(String line, String tag) {

        Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(line);

        return m.find() ? m.group(1) : null;
    }
}
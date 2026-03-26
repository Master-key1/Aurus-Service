package com.auruspay.parser;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class AurusAdvancedParser {

    static class Transaction {

        String date;
        String time;

        String uuid;
        String aurusTxnId;

        String merchantId;
        String terminalId;

        String responseCode;
        String responseMsg;
        String authCode;

        String emvData;

        String aurusEncryptedRequest;
        String processorRequest;
        String processorResponse;
        String aurusEncryptedResponse;

        List<String> issues = new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {

        String file = "transaction_log.txt";

        String uuidFilter = "f5ec3aad-e2e8-4bdd-8858-16f111478b2b";
        String txnIdFilter = "192250421621657007";

        Transaction txn = parseLog(file, uuidFilter, txnIdFilter);

        print(txn);
    }

    public static Transaction parseLog(String file, String uuid, String txnId) throws Exception {

        Transaction t = new Transaction();

        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;

        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})");

        boolean captureEncReq = false;
        boolean captureReq = false;
        boolean captureResp = false;
        boolean captureEncResp = false;

        StringBuilder encReq = new StringBuilder();
        StringBuilder req = new StringBuilder();
        StringBuilder resp = new StringBuilder();
        StringBuilder encResp = new StringBuilder();

        while ((line = br.readLine()) != null) {

            if (!line.contains(uuid) && !line.contains(txnId))
                continue;

            // DATE TIME
            Matcher dt = datePattern.matcher(line);
            if (dt.find()) {
                t.date = dt.group(1);
                t.time = dt.group(2);
            }

            // UUID
            if (line.contains(uuid))
                t.uuid = uuid;

            // TRANSACTION ID
            if (line.contains("Generated Aurus Transaction ID"))
                t.aurusTxnId = extractValue(line);

            // RESPONSE CODE
            if (line.contains("<RespCode>"))
                t.responseCode = extractXML(line, "RespCode");

            // RESPONSE MESSAGE
            if (line.contains("<AddtlRespData>"))
                t.responseMsg = extractXML(line, "AddtlRespData");

            // AUTH CODE
            if (line.contains("<AuthID>"))
                t.authCode = extractXML(line, "AuthID");

            // EMV DATA
            if (line.contains("<EMVData>"))
                t.emvData = extractXML(line, "EMVData");

            // ===== ENCRYPTED REQUEST =====
            if (line.contains("[STPL-GRAY-STREAM]-AURUSPAY ENCRYPTED REQUEST")) {
                captureEncReq = true;
                encReq.append(line).append("\n");
                continue;
            }

            if (captureEncReq) {
                encReq.append(line).append("\n");

                if (line.trim().isEmpty()) {
                    captureEncReq = false;
                    t.aurusEncryptedRequest = encReq.toString();
                }
            }

            // ===== PROCESSOR REQUEST =====
            if (line.contains("[STPL-GRAY-STREAM]- REQUEST")) {
                captureReq = true;
                req.append(line).append("\n");
                continue;
            }

            if (captureReq) {
                req.append(line).append("\n");

                if (line.contains("</Request>")) {
                    captureReq = false;
                    t.processorRequest = req.toString();
                }
            }

            // ===== PROCESSOR RESPONSE =====
            if (line.contains("[STPL-GRAY-STREAM]-FINAL RESPONSE")) {
                captureResp = true;
                resp.append(line).append("\n");
                continue;
            }

            if (captureResp) {
                resp.append(line).append("\n");

                if (line.contains("</GMF>")) {
                    captureResp = false;
                    t.processorResponse = resp.toString();
                }
            }

            // ===== ENCRYPTED RESPONSE =====
            if (line.contains("[STPL-GRAY-STREAM]-AURUSPAY ENCRYPTED RESPONSE")) {
                captureEncResp = true;
                encResp.append(line).append("\n");
                continue;
            }

            if (captureEncResp) {
                encResp.append(line).append("\n");

                if (line.trim().isEmpty()) {
                    captureEncResp = false;
                    t.aurusEncryptedResponse = encResp.toString();
                }
            }

            // ===== ERROR DETECTION =====
            if (line.matches(".*(ERROR|Exception|Timeout|Declined|Failed).*")) {
                t.issues.add(line);
            }
        }

        br.close();

        return t;
    }

    static String extractValue(String line) {
        if (line.contains(":"))
            return line.substring(line.indexOf(":") + 1).trim();
        return "";
    }

    static String extractXML(String line, String tag) {

        Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(line);

        if (m.find())
            return m.group(1);

        return "";
    }

    static void print(Transaction t) {

        System.out.println("\n=========== TRANSACTION DETAILS ===========");

        System.out.println("DATE : " + t.date);
        System.out.println("TIME : " + t.time);

        System.out.println("UUID : " + t.uuid);
        System.out.println("TXN ID : " + t.aurusTxnId);

        System.out.println("RESPONSE CODE : " + t.responseCode);
        System.out.println("RESPONSE MESSAGE : " + t.responseMsg);
        System.out.println("AUTH CODE : " + t.authCode);

        System.out.println("EMV DATA : " + t.emvData);

        System.out.println("\n===== AURUSPAY ENCRYPTED REQUEST =====");
        System.out.println(t.aurusEncryptedRequest);

        System.out.println("\n===== PROCESSOR REQUEST =====");
        System.out.println(t.processorRequest);

        System.out.println("\n===== PROCESSOR RESPONSE =====");
        System.out.println(t.processorResponse);

        System.out.println("\n===== AURUSPAY ENCRYPTED RESPONSE =====");
        System.out.println(t.aurusEncryptedResponse);

        System.out.println("\n===== ISSUES =====");

        if (t.issues.isEmpty())
            System.out.println("NO ISSUES");
        else
            t.issues.forEach(System.out::println);
    }
}
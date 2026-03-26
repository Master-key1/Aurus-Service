package com.auruspay.service;

import com.jcraft.jsch.*;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReader {

    public static void main(String[] args) {

        String jumpHost = "uat42.auruspay.com";
        String jumpUser = "vchavan";
        String jumpPass = "Bh@nDup$3_2k26!";

        String targetHost = "192.168.50.155";
        String targetUser = "vchavan";
        String targetPass = "Ch!nchP0kl!_2k26!";

        String txnId = "295260761944220105";

        int sshPort = 22;

        Session jumpSession = null;
        Session targetSession = null;

        // 🔥 Output file path
        String outputFile = "C:\\Users\\nkharose\\Documents\\AI Testing task\\txn_log_output.txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {

            JSch jsch = new JSch();

            // 1. Connect Jump Host
            jumpSession = jsch.getSession(jumpUser, jumpHost, sshPort);
            jumpSession.setPassword(jumpPass);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            jumpSession.setConfig(config);
            jumpSession.connect();

            writer.println("Connected to Jump Host");

            // 2. Port Forwarding
            int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, sshPort);

            // 3. Connect Target
            targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
            targetSession.setPassword(targetPass);
            targetSession.setConfig(config);
            targetSession.connect();

            writer.println("Connected to Target Host");

            // STEP 1: Find txn log
            String grepTxnCommand =
                    "grep -a -m 1 '" + txnId + "' /opt/auruspay_switch/log/auruspay/auruspay.log";

            String txnOutput = executeCommand(targetSession, grepTxnCommand);

            writer.println("\n===== RAW TXN LOG =====");
            writer.println(txnOutput);

            // STEP 2: Extract UUID
            String uuid = extractUUID(txnOutput, txnId);

            if (uuid == null) {
                writer.println("❌ UUID not found for txnId: " + txnId);
                return;
            }

            writer.println("✅ Extracted UUID: " + uuid);

            // STEP 3: Fetch logs using UUID
            String uuidCommand =
                    "grep -a '" + uuid + "' /opt/auruspay_switch/log/auruspay/auruspay.log | head -50";

            String uuidOutput = executeCommand(targetSession, uuidCommand);

            writer.println("\n===== UUID BASED LOGS =====");
            writer.println(uuidOutput);

            writer.println("\n====================================\n");

            System.out.println("✅ Logs written to file: " + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (targetSession != null && targetSession.isConnected()) {
                targetSession.disconnect();
            }
            if (jumpSession != null && jumpSession.isConnected()) {
                jumpSession.disconnect();
            }
        }
    }

    // ✅ Command Executor
    private static String executeCommand(Session session, String command) throws Exception {

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];

        while (true) {
            while (in.available() > 0) {
                int i = in.read(buffer);
                if (i < 0) break;
                output.append(new String(buffer, 0, i));
            }

            if (channel.isClosed()) break;

            Thread.sleep(100);
        }

        channel.disconnect();
        return output.toString();
    }

    // ✅ UUID Extractor
    private static String extractUUID(String output, String txnId) {

        if (output == null || output.isEmpty()) return null;

        String[] lines = output.split("\n");

        Pattern uuidPattern = Pattern.compile("[a-f0-9\\-]{36}");

        for (String line : lines) {
            if (line.contains(txnId)) {
                Matcher matcher = uuidPattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
        }

        return null;
    }
}
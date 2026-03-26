package com.auruspay.service;

import com.jcraft.jsch.*;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedSshJump {

    public static void main(String[] args) {

        String jumpHost = "uat42.auruspay.com";
        String jumpUser = "vchavan";
        String jumpPass = "Bh@nDup$3_2k26!";

        String targetHost = "192.168.50.155";
        String targetUser = "vchavan";
        String targetPass = "Ch!nchP0kl!_2k26!";

        String txnId = "295260853050904205"; // INPUT TRANSACTION ID

        int sshPort = 22;

        Session jumpSession = null;
        Session targetSession = null;

        try {
            JSch jsch = new JSch();

            // 1. Connect to Jump Host
            jumpSession = jsch.getSession(jumpUser, jumpHost, sshPort);
            jumpSession.setPassword(jumpPass);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            jumpSession.setConfig(config);

            jumpSession.connect();
            System.out.println("Connected to Jump Host");

            // 2. Port Forwarding
            int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, sshPort);

            // 3. Connect to Target
            targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
            targetSession.setPassword(targetPass);
            targetSession.setConfig(config);
            targetSession.connect();

            System.out.println("Connected to Target Host");

            // 4. Execute Command
            String command = "grep --text '" + txnId + "' /opt/auruspay_switch/log/auruspay/auruspay.log";
            System.out.println("Command : "+ command);

            ChannelExec channel = (ChannelExec) targetSession.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            StringBuilder output = new StringBuilder();

            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) break;
                    String chunk = new String(buffer, 0, i);
                    output.append(chunk);
                }

                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(100);
            }

            channel.disconnect();

            // 5. Extract UUID using regex
            // UUID pattern
            Pattern pattern = Pattern.compile("([a-f0-9\\-]{36})");
            Matcher matcher = pattern.matcher(output.toString());

            String foundUUID = null;

            while (matcher.find()) {
                String uuid = matcher.group(1);

                // Ensure line contains txnId
                if (output.toString().contains(txnId)) {
                    foundUUID = uuid;
                    break;
                }
            }
            {
            	 // 4. Execute Command
                 command = "grep --text '" + foundUUID + "' /opt/auruspay_switch/log/auruspay/auruspay.log";
                 System.out.println("Command : "+ command);
                 channel = (ChannelExec) targetSession.openChannel("exec");
                channel.setCommand(command);

                 in = channel.getInputStream();
                channel.connect();

                 output = new StringBuilder();

                 buffer = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(buffer, 0, 1024);
                        if (i < 0) break;
                        String chunk = new String(buffer, 0, i);
                        output.append(chunk);
                    }

                    if (channel.isClosed()) {
                        break;
                    }
                    Thread.sleep(100);
                }

                channel.disconnect();
            }

            if (foundUUID != null) {
                System.out.println("Extracted UUID: " + foundUUID);
            } else {
                System.out.println("UUID not found for txnId: " + txnId);
            }
            
            
            

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
}
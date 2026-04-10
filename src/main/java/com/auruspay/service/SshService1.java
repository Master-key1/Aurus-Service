package com.auruspay.service;

import com.jcraft.jsch.*;

import java.io.InputStream;
import java.util.Properties;

public class SshService1 {

    private static final String JUMP_HOST = "uat42.auruspay.com";
    private static final String JUMP_USER = "vchavan";
    private static final String JUMP_PASS = "Bh@nDup$3_2k26!";

    private static final String TARGET_HOST = "192.168.50.155";
    private static final String TARGET_USER = "vchavan";
    private static final String TARGET_PASS = "Ch!nchP0kl!_2k26!";

    private static final String LOG_PATH = "/opt/auruspay_switch/log/auruspay/";

    private Session jumpSession;
    private Session targetSession;

    // ================= CONNECT =================
    public void connect() throws Exception {

        JSch jsch = new JSch();

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        // 🔹 Jump Host
        jumpSession = jsch.getSession(JUMP_USER, JUMP_HOST, 22);
        jumpSession.setPassword(JUMP_PASS);
        jumpSession.setConfig(config);
        jumpSession.connect(10000);

        // 🔹 Port Forward
        int forwardedPort = jumpSession.setPortForwardingL(0, TARGET_HOST, 22);

        // 🔹 Target Host
        targetSession = jsch.getSession(TARGET_USER, "127.0.0.1", forwardedPort);
        targetSession.setPassword(TARGET_PASS);
        targetSession.setConfig(config);
        targetSession.connect(10000);
    }

    // ================= DISCONNECT =================
    public void disconnect() {

        if (targetSession != null && targetSession.isConnected()) {
            targetSession.disconnect();
        }

        if (jumpSession != null && jumpSession.isConnected()) {
            jumpSession.disconnect();
        }
    }

    // ================= FETCH TXN =================
    public String fetchTxnLogs(String txnId) throws Exception {

        String command = "zgrep --text '" + txnId + "' " + LOG_PATH + "auruspay.log-2026-03-26-09*";
        return execute(command);
    }

    // ================= FETCH UUID =================
    public String fetchUUIDLogs(String uuid) throws Exception {

        String command = "zgrep --text '" + uuid + "' " + LOG_PATH + "auruspay.log-2026-03-26-09*";
        return execute(command);
    }

    // ================= EXECUTE COMMAND =================
    private String execute(String command) throws Exception {

        if (targetSession == null || !targetSession.isConnected()) {
            throw new IllegalStateException("SSH not connected");
        }

        ChannelExec channel = (ChannelExec) targetSession.openChannel("exec");
        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        channel.connect(5000);

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
}
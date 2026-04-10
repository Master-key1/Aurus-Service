package com.auruspay.service;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class SCP_Service {

    private static final Logger log = LoggerFactory.getLogger(SCP_Service.class);

    private static final int SESSION_TIMEOUT = 30000;
    private static final int CHANNEL_TIMEOUT = 30000;

    /**
     * Create SSH Session
     */
    private Session createSession(String host, int port, String username, String password) throws JSchException {

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);

        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no"); // ⚠️ Change to "yes" in production
        config.put("PreferredAuthentications", "password");

        session.setConfig(config);
        session.connect(SESSION_TIMEOUT);

        log.info("SSH Session connected to {}:{}", host, port);

        return session;
    }

    /**
     * Execute command using existing session
     */
    private String executeCommand(Session session, String command) throws Exception {

        StringBuilder output = new StringBuilder();
        ChannelExec channel = null;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream inputStream = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();

            channel.connect(CHANNEL_TIMEOUT);

            byte[] buffer = new byte[1024];
            int read;

            while (true) {

                while (inputStream.available() > 0) {
                    read = inputStream.read(buffer);
                    if (read < 0) break;
                    output.append(new String(buffer, 0, read));
                }

                while (errorStream != null && errorStream.available() > 0) {
                    read = errorStream.read(buffer);
                    if (read < 0) break;
                    output.append(new String(buffer, 0, read));
                }

                if (channel.isClosed()) {
                    break;
                }

                Thread.sleep(100);
            }

            log.info("Command executed: {}", command);

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }

        return output.toString();
    }

    /**
     * Execute single command
     */
    public String executeSingleCommand(String host, int port, String username, String password, String command) {

        Session session = null;

        try {
            session = createSession(host, port, username, password);
            return executeCommand(session, command);

        } catch (Exception e) {
            log.error("Error executing command: {}", command, e);
            throw new RuntimeException("SSH execution failed", e);

        } finally {
            disconnect(session);
        }
    }

    /**
     * Execute multiple commands in one session
     */
    public String executeMultipleCommands(String host, int port, String username, String password, String[] commands) {

        StringBuilder finalOutput = new StringBuilder();
        Session session = null;

        try {
            session = createSession(host, port, username, password);

            for (String cmd : commands) {
                finalOutput.append(">>> ").append(cmd).append("\n");
                finalOutput.append(executeCommand(session, cmd));
                finalOutput.append("\n");
            }

        } catch (Exception e) {
            log.error("Error executing multiple commands", e);
            throw new RuntimeException("SSH multi-command execution failed", e);

        } finally {
            disconnect(session);
        }

        return finalOutput.toString();
    }

    /**
     * Disconnect session
     */
    private void disconnect(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("SSH Session disconnected");
        }
    }
}
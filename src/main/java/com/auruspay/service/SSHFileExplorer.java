package com.auruspay.service;
import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.Vector;

public class SSHFileExplorer {

    static class SSHConfig {
        String host;
        int port;
        String username;
        String password;

        SSHConfig(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }
    }

    private static Session createSession(SSHConfig config) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(config.username, config.host, config.port);

        session.setPassword(config.password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        System.out.println("✅ Connected to server");
        return session;
    }

    // 🔹 Download file
    private static void downloadFile(ChannelSftp sftp, String remoteFile) throws Exception {

        String downloadDir = System.getProperty("user.home") + "/Downloads";
        new File(downloadDir).mkdirs();

        String fileName = remoteFile.substring(remoteFile.lastIndexOf("/") + 1);
        String localFile = downloadDir + "/" + fileName;

        System.out.println("⬇️ Downloading: " + remoteFile);

        FileOutputStream fos = new FileOutputStream(localFile);
        sftp.get(remoteFile, fos);

        fos.close();

        System.out.println("✅ Downloaded to: " + localFile);
    }

    public static void main(String[] args) {

        SSHConfig config = new SSHConfig(
                "uatpos42.auruspay.com",
                22,
                "kevalin",
                "K3v@l!n_2k26!"
        );

        Session session = null;
        ChannelSftp sftp = null;
        Scanner scanner = new Scanner(System.in);

        try {
            session = createSession(config);

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            String currentPath = "/home";

            while (true) {

                System.out.println("\n📂 Current Path: " + currentPath);

                Vector<ChannelSftp.LsEntry> list = sftp.ls(currentPath);

                int index = 1;

                for (ChannelSftp.LsEntry entry : list) {

                    String name = entry.getFilename();

                    // skip . and ..
                    if (name.equals(".") || name.equals("..")) continue;

                    boolean isDir = entry.getAttrs().isDir();

                    System.out.println(index + ". " + (isDir ? "[DIR] " : "[FILE] ") + name);

                    index++;
                }

                System.out.println("\n👉 Enter choice (0 = back, -1 = exit): ");
                int choice = scanner.nextInt();

                if (choice == -1) break;

                if (choice == 0) {
                    if (!currentPath.equals("/")) {
                        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                        if (currentPath.isEmpty()) currentPath = "/";
                    }
                    continue;
                }

                // 🔥 Get selected entry again
                int count = 1;
                ChannelSftp.LsEntry selectedEntry = null;

                for (ChannelSftp.LsEntry entry : list) {
                    String name = entry.getFilename();
                    if (name.equals(".") || name.equals("..")) continue;

                    if (count == choice) {
                        selectedEntry = entry;
                        break;
                    }
                    count++;
                }

                if (selectedEntry == null) {
                    System.out.println("❌ Invalid selection");
                    continue;
                }

                String selectedName = selectedEntry.getFilename();
                String selectedPath = currentPath + "/" + selectedName;

                if (selectedEntry.getAttrs().isDir()) {
                    // 🔹 Go inside folder
                    currentPath = selectedPath;
                } else {
                    // 🔹 Download file
                    downloadFile(sftp, selectedPath);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sftp != null) sftp.disconnect();
            if (session != null) session.disconnect();
            System.out.println("🔌 Disconnected");
        }
    }
}
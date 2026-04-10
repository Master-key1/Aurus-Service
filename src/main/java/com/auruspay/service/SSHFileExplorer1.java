package com.auruspay.service;
import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.Vector;

public class SSHFileExplorer1 {

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

    // =========================
    // CREATE SESSION
    // =========================
    private static Session createSession(SSHConfig config) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(config.username, config.host, config.port);

        session.setPassword(config.password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30000);

        System.out.println("✅ Connected to server");
        return session;
    }

    // =========================
    // DOWNLOAD FILE
    // =========================
    private static void downloadFile(ChannelSftp sftp, String remoteFile) {

        try {
            String downloadDir = System.getProperty("user.home") + "/Downloads";
            new File(downloadDir).mkdirs();

            String fileName = remoteFile.substring(remoteFile.lastIndexOf("/") + 1);
            String localFile = downloadDir + "/" + fileName;

            System.out.println("⬇️ Downloading: " + remoteFile);

            FileOutputStream fos = new FileOutputStream(localFile);
            sftp.get(remoteFile, fos);

            fos.close();

            System.out.println("✅ Downloaded to: " + localFile);

        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
                System.out.println("❌ Permission denied to download file: " + remoteFile);
            } else {
                System.out.println("❌ Download error: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("❌ Unexpected error during download: " + e.getMessage());
        }
    }

    // =========================
    // MAIN METHOD
    // =========================
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

                Vector<ChannelSftp.LsEntry> list;

                // =========================
                // HANDLE PERMISSION ERROR
                // =========================
                try {
                    list = sftp.ls(currentPath);
                } catch (SftpException e) {

                    if (e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
                        System.out.println("❌ Permission denied: " + currentPath);
                    } else {
                        System.out.println("❌ Error accessing: " + currentPath + " -> " + e.getMessage());
                    }

                    // Go back
                    if (!currentPath.equals("/")) {
                        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                        if (currentPath.isEmpty()) currentPath = "/";
                    }

                    continue; // 🔥 continue program
                }

                int index = 1;

                // =========================
                // DISPLAY FILES
                // =========================
                for (ChannelSftp.LsEntry entry : list) {

                    String name = entry.getFilename();

                    if (name.equals(".") || name.equals("..")) continue;

                    boolean isDir = entry.getAttrs().isDir();

                    System.out.println(index + ". " + (isDir ? "[DIR] " : "[FILE] ") + name);

                    index++;
                }

                System.out.println("\n👉 Enter choice (0 = back, -1 = exit): ");

                int choice;
                try {
                    choice = scanner.nextInt();
                } catch (Exception e) {
                    System.out.println("❌ Invalid input");
                    scanner.nextLine();
                    continue;
                }

                if (choice == -1) break;

                if (choice == 0) {
                    if (!currentPath.equals("/")) {
                        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                        if (currentPath.isEmpty()) currentPath = "/";
                    }
                    continue;
                }

                // =========================
                // FIND SELECTED ITEM
                // =========================
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

                // =========================
                // DIRECTORY OR FILE
                // =========================
                if (selectedEntry.getAttrs().isDir()) {
                    currentPath = selectedPath; // go inside folder
                } else {
                    downloadFile(sftp, selectedPath); // download file
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Connection error: " + e.getMessage());
        } finally {
            if (sftp != null) sftp.disconnect();
            if (session != null) session.disconnect();
            System.out.println("🔌 Disconnected");
        }
    }
}
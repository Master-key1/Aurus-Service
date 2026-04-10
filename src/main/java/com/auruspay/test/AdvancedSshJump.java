package com.auruspay.test;

import com.auruspay.decryptor.AurusDecryptor;
import com.auruspay.filewriter.ExcelWriter;
import com.auruspay.filewriter.TxtWriter;
import com.jcraft.jsch.*;

import java.io.InputStream;
import java.util.*;
import java.util.regex.*;

public class AdvancedSshJump {

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

	private static List<String> list=  new ArrayList<>();

	public static void main(String[] args) {

		String jumpHost = "uat42.auruspay.com";
		String jumpUser = "vchavan";
		String jumpPass = "Bh@nDup$3_2k26!";

		String targetHost = "192.168.50.69";
		String targetUser = "vchavan";
		String targetPass = "Ch!nchP0kl!_2k26!";

		String txnId ="197260891187600651";// "295260853503884501";

		Session jumpSession = null;
		Session targetSession = null;

		try {
			JSch jsch = new JSch();

			// Jump Host
			jumpSession = jsch.getSession(jumpUser, jumpHost, 22);
			jumpSession.setPassword(jumpPass);

			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");

			jumpSession.setConfig(config);
			jumpSession.connect();

			int forwardedPort = jumpSession.setPortForwardingL(0, targetHost, 22);

			// Target Host
			targetSession = jsch.getSession(targetUser, "127.0.0.1", forwardedPort);
			targetSession.setPassword(targetPass);
			targetSession.setConfig(config);
			targetSession.connect();
			
			if(targetSession.isConnected()) {
				System.out.println("Host is connected....!");
			}

			// ===== STEP 1: TXN SEARCH =====
			String txnCmd = "grep --text '" + txnId + "' /opt/auruspay_switch/log/auruspay/auruspay.log";//-2026-03-26-09*";//-2026-03-26-09*
			System.out.println("Command :"+txnCmd);
			String txnOutput = executeCommand(targetSession, txnCmd);
			System.out.println(txnOutput);
		
			// ===== STEP 2: UUID =====
			Matcher matcher = Pattern.compile("([a-f0-9\\-]{36})").matcher(txnOutput);

			String uuid = null;
			if (matcher.find())
				uuid = matcher.group();

			if (uuid == null) {
				System.out.println("UUID not found!");
				return;
			}

			System.out.println("\nUUID: " + uuid);

			// ===== STEP 3: UUID LOG =====
			String uuidCmd = "grep --text '" + uuid + "' /opt/auruspay_switch/log/auruspay/auruspay.log";//-2026-03-26-09*";
			System.out.println("Command :"+uuidCmd);
			String uuidOutput = executeCommand(targetSession, uuidCmd);

			System.out.println("\n===== UUID RAW LOG =====");
			System.out.println(uuidOutput);

			// ===== STEP 4: PARSE =====
			Map<String, Object> map = parseLog(uuidOutput, uuid,txnId);
			
			
			
			/*
			 System.out.println(txnId);
			for (Map.Entry<String, Object> entry : map.entrySet()) {

			    String key = entry.getKey();
			    Object value = entry.getValue();

			    System.out.println(key + " : " + value);
			}
			 */
		//	Map<String, Object> map = parseLog(uuidOutput, uuid);

			String path = "C:\\Users\\nkharose\\Documents\\AI Testing task\\"+txnId;
			ExcelWriter writer = new ExcelWriter(path+".xlsx");
			
			// 👉 BEST FORMAT (Recommended)
			writer.addMapAsSingleRow(map);

			// OR
			// writer.createHeader(new String[]{"Field", "Value"});
			// writer.addMapData(map);

			writer.save();
			
			// 👉 Create writer
			TxtWriter txtWriter = new TxtWriter(path+".txt");
			// 👉 Option 1 (Readable)

			// Write + append logs
			txtWriter.writeMapWithLogs(map, uuidOutput);
			// 👉 Option 2 (Compact)
			// writer.writeMapSingleLine(map);
			

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (targetSession != null)
				targetSession.disconnect();
			if (jumpSession != null)
				jumpSession.disconnect();
		}
	}

	// ================= SSH EXEC =================
	private static String executeCommand(Session session, String command) throws Exception {

		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		//command ="hostname";
		channel.setCommand(command);

		InputStream in = channel.getInputStream();
		channel.connect();

		StringBuilder output = new StringBuilder();
		byte[] buffer = new byte[1024];

		while (true) {
			while (in.available() > 0) {
				int i = in.read(buffer);
				if (i < 0)
					break;
				output.append(new String(buffer, 0, i));
			}

			if (channel.isClosed())
				break;
			Thread.sleep(100);
		}

		channel.disconnect();
		return output.toString();
	}

	// ================= PARSE + PRINT =================
	private static Map<String,Object> parseLog(String logs, String uuid, String txnId) {

		boolean encReq = false, req = false, resp = false, encResp = false;
		String val = null;
		System.out.println("\n========== PARSED OUTPUT ==========");
		Map<String, Object> map = new HashMap<>();
		map.put("TXNID", txnId);
		map.put("UUID", uuid);
		
		for (String line : logs.split("\n")) {

			boolean isTarget = line.contains(uuid);

			// ===== ENCRYPTED REQUEST =====
			if (line.contains("AURUSPAY ENCRYPTED REQUEST") && isTarget) {
				val = "AURUSPAY ENCRYPTED REQUEST :";
				String encrypt = line.substring(line.indexOf(val) + val.length());
				String decrypt = AurusDecryptor.decryptor(encrypt);
				map.put("AurusReqEncrypt", encrypt);
				map.put("AurusReqDecrypt", decrypt);
				
				//System.out.println(val + encrypt);
				//System.out.println(val+decrypt);
				

			}

			// ===== PROCESSOR REQUEST =====
			if (line.contains("[STPL-GRAY-STREAM]- REQUEST :") && isTarget) {
				val = "[STPL-GRAY-STREAM]- REQUEST :";
				String encrypt = line.substring(line.indexOf(val) + val.length());
				String decrypt = AurusDecryptor.decryptor(encrypt);
				map.put("ProcReqEncrypt", encrypt);
				map.put("ProcReqDecrypt", decrypt);
				
				//System.out.println(val + encrypt);
				//System.out.println(val+decrypt);
			}

			

			// ===== FINAL RESPONSE =====
			if (line.contains("[STPL-GRAY-STREAM]-FINAL RESPONSE :") && isTarget) {
				val = "[STPL-GRAY-STREAM]-FINAL RESPONSE :";
				String encrypt = line.substring(line.indexOf(val) + val.length());
				String decrypt = AurusDecryptor.decryptor(encrypt);
				map.put("ProcResEncrypt", encrypt);
				map.put("ProcResDecrypt", decrypt);
				
			//	System.out.println(val + encrypt);
			//	System.out.println(val+decrypt);
			}

			
			// ===== ENCRYPTED RESPONSE =====
			if (line.contains("AURUSPAY ENCRYPTED RESPONSE") && isTarget) {
				val = "AURUSPAY ENCRYPTED RESPONSE :";
				String encrypt = line.substring(line.indexOf(val) + val.length());
				String decrypt = AurusDecryptor.decryptor(encrypt);
				map.put("AurusResEncrypt", encrypt);
				map.put("AurusResDecrypt", decrypt);
				
			//	System.out.println(val + encrypt);
				//System.out.println(val+decrypt);
			}

			// ===== ERROR =====
			if (line.matches(".*(ERROR|Exception|Timeout|Declined|Failed).*")) {
				list.add(line);
				//System.out.println("\n⚠ ISSUE: " + line);
			}
			//map.put("Issue", list);
		}
		return map;
	}

	
}
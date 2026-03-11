package com.auruspay.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class LogFinderService2 {

	private static final Logger logger = Logger.getLogger(LogFinderService2.class.getName());

	private static final String USERNAME = "kevalin";

	private static final String LOG_PATH = "/opt/auruspay_switch/log/auruspay/auruspay.log";

	private final Map<String, String> nodeMap = new HashMap<>();

	private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F\\-]{36}");

	public LogFinderService2() {

		nodeMap.put("91", "192.168.50.152");
		nodeMap.put("92", "192.168.50.153");
		nodeMap.put("93", "192.168.50.172");
		nodeMap.put("94", "192.168.50.72");
		nodeMap.put("95", "192.168.50.155");
		nodeMap.put("96", "192.168.50.196");
		nodeMap.put("97", "192.168.50.69");

	}

//Step-1
	public String resolveNodeIP(String txnId) {

		String nodeKey = txnId.substring(1, 3);

		logger.info("Resolving node for key: " + nodeKey);

		return nodeMap.get(nodeKey);
	}

	private String buildCommand(String searchValue, String ip, boolean withinOneHour, String formattedHour) {

		if (!withinOneHour) {

			String archivedLog = LOG_PATH + "-" + formattedHour + "*";

			return String.format("ssh %s@%s \"zgrep --text -C5 '%s' %s\"", USERNAME, ip, searchValue, archivedLog);

		} else {

			return String.format("ssh %s@%s \"grep --text -C5 '%s' %s\"", USERNAME, ip, searchValue, LOG_PATH);
		}
	}

	// step-2
	public String findUUID(String txnId, String ip, boolean withinOneHour, String formattedHour) throws Exception {

		
		String cmd = buildCommand(txnId, ip, withinOneHour, formattedHour);

		logger.info("Executing command: " + cmd);

		ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);

		pb.redirectErrorStream(true);

		Process process = pb.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

			String line;

			while ((line = reader.readLine()) != null) {

				if (line.contains(txnId)) {

					Matcher matcher = UUID_PATTERN.matcher(line);

					if (matcher.find()) {

						String uuid = matcher.group();

						logger.info("UUID Found: " + uuid);

						return uuid;
					}
				}
			}
		}

		return null;
	}

	// step-3
	public StringBuilder searchFullTransaction(String uuid, String ip, boolean withinOneHour, String formattedHour)
			throws Exception {

		logger.info("Starting full transaction search for UUID: " + uuid + " from IP: " + ip);

		String cmd = buildCommand(uuid, ip, withinOneHour, formattedHour);
		logger.info("Generated command: " + cmd);

		StringBuilder data = new StringBuilder();

		ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
		pb.redirectErrorStream(true);

		logger.info("Executing command...");

		Process process = pb.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

			String line;
			int count = 0 ;
			while ((line = reader.readLine()) != null) {
				data.append(line).append("\n");
			
				++count;
			}
			logger.info("Log Line count: " + count);

		} catch (Exception e) {
			logger.severe("Error while reading process output: " + e.getMessage());
			throw e;
		}

		int exitCode = process.waitFor();
		logger.info("Command execution completed with exit code: " + exitCode);

		if (data.length() == 0) {

			logger.warning("No logs found for UUID: " + uuid);

			}

		logger.info("Transaction log search completed for UUID: " + uuid);

		return data;
	}
}
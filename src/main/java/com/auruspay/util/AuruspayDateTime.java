package com.auruspay.util;

import java.text.ParseException; 
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;

/*
 * 
 * This Method used to parse Date-Time of transaction and its node id from transaction Id. 
 * 
 *  
 * */
public class AuruspayDateTime {

	private static String time1;
	private static String date1;
	
	public static void main(String[] args) {
		processTxn("191261060383667729");
	}
	
	public static  String processTxn(String txnid) {
		
		
		if (txnid.length() != 18) {
			return "Invalid txn..!";
		} else {
			String[] txnIdList = txnid.split("\n");
			for (String str : txnIdList) {
				String txnNodeId = str.substring(1, 3);// Node Id
				String yy = str.substring(3, 5); // Year
				String date = str.substring(5, 8); // Day of the year
				yy = "20" + yy;
				int dayOfYear = Integer.parseInt(date);
				Year y = Year.of(Integer.parseInt(yy));
				LocalDate ld = y.atDay(dayOfYear);

				String timestamp = str.substring(8, 16);

				long millis = Long.parseLong(timestamp);
				String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),TimeUnit.MILLISECONDS.toMinutes(millis)- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
						TimeUnit.MILLISECONDS.toSeconds(millis)- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

				String dateTime = "";
				String inputDate = ld + " " + hms;
				try {
					System.out.println(inputDate);
					Date date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(inputDate);
					dateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aa").format(date2);
					System.out.println("\n---------------------------------------------------------------- \n");
					System.out.println("TRANSACTION ID	:: "+str);
					System.out.println("DATE&TIME	:: "+dateTime);
					System.out.println("APP SERVER	:: "+AuruspayDateTime.NodeId.get(txnNodeId));
					System.out.println("APP NODE ID	:: "+AuruspayDateTime.ServerId.get(txnNodeId));
					System.out.println("\n---------------------------------------------------------------- ");
					//time1 = dateTime.substring(11, 13);
					//date1 = dateTime.substring(0, 10);
					time1 = inputDate.substring(11, 13);
					date1 = inputDate.substring(0, 10);
				} catch (ParseException e) {
					e.printStackTrace();
				}

			}
			String Command = "grep --color --text \"" + txnid + "\" /opt/auruspay_switch/log/auruspay/auruspay.log";
			System.out.println("GREP - COMMAND	:: \n\n"+ Command);
			System.out.println("\n---------------------------------------------------------------- ");
			String Command2 = "zgrep --color --text \"" + txnid + "\" /opt/auruspay_switch/log/auruspay/auruspay.log-"+ date1 + "-" + time1 + ".zip";
			System.out.println("ZGREP - COMMAND	:: \n\n"+ Command2);
			System.out.println("\n---------------------------------------------------------------- ");
			String Command3 = "zgrep --color --text \"" + txnid + "\" auruspay.log-"+ date1 + "-" + time1 + ".zip";
            System.out.println("BACKUP ZGREP - COMMAND :: \n\n"+ Command3);
            System.out.println("\n---------------------------------------------------------------- ");
		}
		return txnid;

		
	}

	public static final Map<String, String> NodeId = new HashMap<String, String>() {
		{
			put("01", "MS11 App1");
			put("02", "MS11 App2");
			put("23", "MS11 App3");
			put("24", "MS11 App4");
			put("71", "MS11 App5");
			put("72", "MS11 App6");
			put("03", "MS41 App1");
			put("04", "MS41 App2");
			put("43", "MS41 App3");
			put("44", "MS41 App4");
			put("73", "MS41 App5");
			put("74", "MS41 App6");
			put("05", "MS17 App1");
			put("06", "MS17 App2");
			put("25", "MS17 App3");
			put("26", "MS17 App4");
			put("83", "MS17 App5");
			put("84", "MS17 App6");
			put("07", "MS57 App1");
			put("08", "MS57 App2");
			put("41", "MS57 App3");
			put("42", "MS57 App4");
			put("85", "MS57 App5");
			put("86", "MS57 App6");
			put("09", "MS01 App1");
			put("10", "MS01 App2");
			put("27", "MS01 App3");
			put("28", "MS01 App4");
			put("11", "MS02 App1");
			put("12", "MS02 App2");
			put("45", "MS02 App3");
			put("46", "MS02 App4");
			put("13", "MS13 APP1");
			put("14", "MS13 APP2");
			put("63", "MS13 APP3");
			put("64", "MS13 APP4");
			put("15", "MS43 APP1");
			put("16", "MS43 APP2");
			put("65", "MS43 APP3");
			put("66", "MS43 APP4");
			put("17", "IVR44 App1");
			put("18", "IVR44 App2");
			put("19", "IVR14 App1");
			put("20", "IVR14 App2");
			put("29", "MS19 App1");
			put("30", "MS19 App2");
			put("31", "MS19 App3");
			put("32", "MS19 App4");
			put("33", "MS59 App1");
			put("34", "MS59 App2");
			put("35", "MS59 App3");
			put("36", "MS59 App4");
			put("37", "MS21 App01");
			put("38", "MS21 App02");
			put("39", "MS61 App01");
			put("40", "MS61 App02");
			put("47", "ES01 APP01");
			put("48", "ES01 APP02");
			put("59", "ES01 APP03");
			put("60", "ES01 APP04");
			put("49", "ES41 APP01");
			put("50", "ES41 APP02");
			put("61", "ES41 APP03");
			put("62", "ES41 APP04");
			put("21", "MK DMSuite");
			put("22", "VW DMSuite");
			put("51", "MS03 APP1");
			put("52", "MS03 APP2");
			put("53", "MS03 APP3");
			put("54", "MS03 APP4");
			put("55", "MS04 APP1");
			put("56", "MS04 APP2");
			put("57", "MS04 APP3");
			put("58", "MS04 APP4");
			put("67", "MS12 APP1");
			put("68", "MS12 APP2");
			put("69", "MS12 APP3");
			put("70", "MS12 APP4");
			put("71", "MS52 APP1");
			put("72", "MS52 APP2");
			put("73", "MS52 APP3");
			put("74", "MS52 APP4");
			put("75", "MS14 APP1");
			put("76", "MS14 APP2");
			put("77", "MS14 APP3");
			put("78", "MS14 APP4");
			put("79", "MS54 APP1");
			put("80", "MS54 APP2");
			put("81", "MS54 APP3");
			put("82", "MS54 APP4");
			put("83", "MS15 APP1");
			put("84", "MS15 APP2");
			put("85", "MS15 APP3");
			put("86", "MS15 APP4");
			put("87", "MS55 APP1");
			put("88", "MS55 APP2");
			put("89", "MS55 APP3");
			put("90", "MS55 APP4");
			put("91", "UAT42 APP1");
			put("92", "UAT42 APP2");
			put("93", "UAT42 APP3");
			put("94", "UAT42 APP4");
			put("95", "STG42 APP1");
			put("96", "PFX42 APP1");
			put("97", "PFX42 APP2");
			put("98", "UAT51 APP1");
			put("99", "UAT51 APP2");
			put("100", "MS16 APP1");
			put("101", "MS16 APP2");
			put("102", "MS16 APP3");
			put("103", "MS16 APP4");
			put("104", "MS56 APP1");
			put("105", "MS56 APP2");
			put("106", "MS56 APP3");
			put("107", "MS56 APP4");
			put("108", "IVR15 App1");
			put("109", "IVR15 App2");
			put("110", "IVR55 App1");
			put("111", "IVR55 App2");
			put("112", "ES02 APP01");
			put("113", "ES02 APP02");
			put("114", "ES42 APP01");
			put("115", "ES42 APP02");

		}
	};
	

	public static final Map<String, String> ServerId = new HashMap<String, String>() {
		{
			put("01", "172.XX.131.171");
			put("02", "172.XX.132.181");
			put("23", "172.XX.131.170");
			put("24", "172.XX.132.180");
			put("71", "172.XX.131.154");
            put("72", "172.XX.132.150");
			put("03", "172.XX.20.152");
			put("04", "172.XX.21.52");
			put("43", "172.XX.20.52");
			put("44", "172.XX.21.152");
			put("73", "172.XX.20.102");
            put("74", "172.XX.21.220");
			put("05", "172.XX.131.196");
			put("06", "172.XX.132.196");
			put("25", "172.XX.131.238");
			put("26", "172.XX.132.238");
			put("07", "172.XX.20.196");
			put("08", "172.XX.21.196");
			put("41", "172.XX.20.38");
			put("42", "172.XX.21.38");
			put("09", "172.XX.131.200");
			put("10", "172.XX.132.200");
			put("27", "172.XX.131.204");
			put("28", "172.XX.132.204");
			put("11", "172.XX.20.151");
			put("12", "172.XX.21.51");
			put("45", "172.XX.20.51");
			put("46", "172.XX.21.151");
			put("13", "172.XX.131.190");
			put("14", "172.XX.132.190");
			put("63", "172.XX.131.218");
			put("64", "172.XX.132.218");
			put("15", "172.XX.10.27");
			put("16", "172.XX.20.27");
			put("65", "172.XX.10.30");
			put("66", "172.XX.20.30");
			put("17", "172.XX.20.32");
			put("18", "172.XX.21.32");
			put("19", "172.XX.131.132");
			put("20", "172.XX.132.132");
			put("29", "172.XX.131.182");
			put("30", "172.XX.132.182");
			put("31", "172.XX.131.161");
			put("32", "172.XX.132.161");
			put("33", "172.XX.20.182");
			put("34", "172.XX.21.182");
			put("35", "172.XX.20.161");
			put("36", "172.XX.21.161");
			put("37", "172.XX.131.162");
			put("38", "172.XX.132.162");
			put("39", "172.XX.20.170");
			put("40", "172.XX.21.170");
			put("47", "172.XX.131.191");
			put("48", "172.XX.132.191");
			put("59", "172.XX.131.240");
			put("60", "172.XX.132.234");
			put("49", "172.XX.20.186");
			put("50", "172.XX.21.86");
			put("61", "172.XX.20.85");
			put("62", "172.XX.21.85");
			put("21", "172.XX.131.223");
			put("22", "172.XX.131.153");
			put("51", "172.XX.131.193");
			put("52", "172.XX.132.199");
			put("53", "172.XX.131.195");
			put("54", "172.XX.132.170");
			put("55", "172.XX.20.231");
			put("56", "172.XX.21.231");
			put("57", "172.XX.20.73");
			put("58", "172.XX.21.73");
			put("67", "172.XX.231.14");
			put("68", "172.XX.232.14");
			put("69", "172.XX.231.32");
			put("70", "172.XX.232.32");
			put("71", "172.XX.120.14");
			put("72", "172.XX.121.14");
			put("73", "172.XX.120.32");
			put("74", "172.XX.121.32");
			put("75", "172.XX.231.17");
			put("76", "172.XX.232.17");
			put("77", "172.XX.231.18");
			put("78", "172.XX.232.18");
			put("79", "172.XX.120.17");
			put("80", "172.XX.121.17");
			put("81", "172.XX.120.18");
			put("82", "172.XX.121.18");
			put("83", "172.XX.231.76");
			put("84", "172.XX.232.76");
			put("85", "172.XX.231.77");
			put("86", "172.XX.232.77");
			put("87", "172.XX.120.76");
			put("88", "172.XX.121.76");
			put("89", "172.XX.120.77");
			put("90", "172.XX.121.77");
			put("91", "192.168.50.152");
			put("92", "192.168.50.153");
			put("93", "192.168.50.172");
			put("94", "192.168.50.72");
			put("95", "192.168.50.155");
			put("96", "192.168.50.169");
			put("97", "192.168.50.69");
			put("98", "192.168.106.11");
			put("99", "192.168.107.11");
			put("100", "MS16 APP1");
			put("101", "MS16 APP2");
			put("102", "MS16 APP3");
			put("103", "MS16 APP4");
			put("104", "MS56 APP1");
			put("105", "MS56 APP2");
			put("106", "MS56 APP3");
			put("107", "MS56 APP4");
			put("108", "IVR15 App1");
			put("109", "IVR15 App2");
			put("110", "IVR55 App1");
			put("111", "IVR55 App2");
			put("112", "ES02 APP01");
			put("113", "ES02 APP02");
			put("114", "ES42 APP01");
			put("115", "ES42 APP02");

		}
	};

}
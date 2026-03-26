package com.auruspay.util;

import java.io.File;

import com.auruspay.filewriter.ExcelWriter;

public class ModelWriter {
    public static void main(String[] args) {

    	try {
    	    String fileName = "tasks.xlsx";
    	     String path = "C:\\Users\\nkharose\\Pictures\\Excel\\";
    	    File file = new File(path+fileName);

    	    ExcelWriter writer = new ExcelWriter(file.getAbsolutePath());

    	    // Header
    	    writer.createHeader(new String[]{
    	            "ID", "Name", "Role", "Salary"
    	    });

    	    // Add entries
    	    writer.addRow(new Object[]{1, "Nitesh", "Java Developer", 60000});
    	    writer.addRow(new Object[]{2, "Rahul", "Tester", 40000});
    	    writer.addRow(new Object[]{3, "Priya", "Manager", 90000});

    	    // Save file
    	    writer.save();

    	    // ✅ Print full path
    	    System.out.println("Excel file created at: " + file.getAbsolutePath());

    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
    }
}
package com.auruspay.controller;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.auruspay.AurusServiceApplication;
import com.auruspay.model.LogService;
import com.auruspay.service.Driver;
import com.auruspay.service.Helper;

@RestController
@RequestMapping("/aurus")
public class AurusServiceController {

    private final AurusServiceApplication aurusServiceApplication;
	

	@Autowired
	private Environment env  ;
	
	@Autowired
	private Helper helper ;
	@Autowired
	private Driver driver;
	
	
	   private static final Logger logger = Logger.getLogger(AurusServiceController.class.getName());
	   private static final int ResponseEntity = 0;

    AurusServiceController(AurusServiceApplication aurusServiceApplication) {
        this.aurusServiceApplication = aurusServiceApplication;
    }

	    @GetMapping("/log/{txnID}/{uniqueID}/{auruspayDate}")
	    public ResponseEntity<String> getLogDetails(
	            @PathVariable String txnID,
	            @PathVariable String uniqueID,
	            @PathVariable String auruspayDate) {
	    	
	        logger.info("Received request for log search");
	        logger.info("txnID: " + txnID + ", uniqueID: " + uniqueID + ", auruspayDate: " + auruspayDate);

	        try {

	            logger.info("Invoking log search service");

	       //     StringBuilder logBuffer = main.invokeLogDetails(txnID, uniqueID, auruspayDate);

	       //     logger.info("Log search completed successfully.\n"+logBuffer);

	      //      return new ResponseEntity<>(logBuffer.toString(), HttpStatus.OK);

	        } catch (Exception e) {

	            logger.severe("Error while fetching logs: " + e.getMessage());

	            return new ResponseEntity<>("Error fetching logs", HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	        return null;
	    }
	
	@GetMapping("/logger")
	//public ResponseEntity<LogServiceDetails> getLogDetails(@RequestBody LogServiceDetails details ){
	public ResponseEntity<String> getLogDetails(){
		
		/*
		LogServiceDetails details = null;
		String txnID = details.getTxnID();
		String uniqueID = details.getUniqueID();
		String auruspayDate = details.getAuruspayDate();
		*/
		
		String details = "logger : (port : "; 
		String port = env.getProperty("server.port");
		details+=port +")";
		return new ResponseEntity<String>(details ,HttpStatus.OK);
		
	}
	
	@GetMapping("/logs")
	public StringBuffer getLog(@RequestParam("txnid") String txnID) {
	//    return helper.main(txnID);  // should return Map
	    return driver.main(txnID);   
		
	}
	
	  @PostMapping("/txnlog")
	    public String gettxnLog( @RequestBody LogService logService ){
	          
		  System.out.println("TXN ID :"+logService.getTxnID());
		  System.out.println("Date :"+logService.getAuruspayDate());
		  System.out.println("UUID "+logService.getUniqueID());
		  
		  return helper.main(logService.getTxnID()).toString();
		  
	  }
	
	
	
    @GetMapping("/test")
    public ResponseEntity<String> test() {
    	 try {

             logger.info("Invoking log search service");

             String txnID = null;
			 String uniqueID= null;
			 String auruspayDate= null;
			// StringBuilder logBuffer = main.invokeLogDetails(txnID, uniqueID, auruspayDate);

          //   logger.info("Log search completed successfully.\n"+logBuffer);

            // return new ResponseEntity<>(logBuffer.toString(), HttpStatus.OK);

         
    	String port = env.getProperty("server.port");
    	
        String  msg = "\"Aurus Service Running ......... with ( Port : \"+port+\" )\"";
        
		return new ResponseEntity<String>(msg,HttpStatus.OK);
    	 } catch (Exception e) {

             logger.severe("Error while fetching logs: " + e.getMessage());

             return new ResponseEntity<>("Error fetching logs"+e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
         }
    }
	

}

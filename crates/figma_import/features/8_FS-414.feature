@FS-452 @REQ_FS-414
Feature: SMR-014 Allow OEMs to designate Static UI Elements
	#The Vehicle Software UI toolkit shall provide a way for OEMs to designate static UI elements as Safety Critical and therefore to be monitored by the Safety Monitor.

	#Input: activate/deactivate *every* OEM static component request on the CAN bus sequentially and invert pixel update  framebuffer
	#Output:  safe state signal triggered within TBD ms
	@TEST_FS-489 @TESTSET_FS-458 @smr_testcase_import1
	Scenario: Static Pixel update wrong (presence)
		Given BUS.<signal> = <signal_status_1>
		When BUS.<signal> = <signal_status_2>
		And pixelbuffer.<telltale> is inverted
		Then safe state signal is set within t_tol_1 ms
		
		Examples:  
		|	signal			      |	telltale	  	    |	signal_status_1 | signal_status_2 |
		|	lateral_control 	|	lateral_control	  |	ON	          	| OFF             |
		|	lateral_control 	|	lateral_control	  |	OFF         		| ON              |
		|	cruise_control  	|	cruise_control	  |	ON	          	| OFF             |
		|	cruise_control	  |	cruise_control	  |	OFF         		| ON              |
		|	tpms_warning	    |	tpms_warning    	|	ON	          	| OFF             |
		|	tpms_warning	    |	tpms_warning  	  |	OFF         		| ON              |
		|	stability_failure	|	stability_failure	|	ON	          	| OFF             |
		|	stability_failure	|	stability_failure	|	OFF         		| ON              |
		|	sidelights		    |	sidelights		    |	ON	          	| OFF             |
		|	sidelights		    |	sidelights		    |	OFF         		| ON              |
		|	lowbeam		        |	lowbeam		        |	ON	          	| OFF             |
		|	lowbeam	        	|	lowbeam		        |	OFF         		| ON              |
		|	highbeam		      |	highbeam		      | ON	          	| OFF             |
		|	highbeam		      |	highbeam		      |	OFF         		| ON              |
		|	driver_airbag		  |	driver_airbag	   	|	ON	          	| OFF             |
		|	driver_airbag		  |	driver_airbag		  |	OFF         		| ON              |
		|	seatbelt		      |	seatbelt		      |	ON	          	| OFF             |
		|	seatbelt		      |	seatbelt		      |	OFF         		| ON              |
		|	parking_brake	    |	parking_brake	    |	ON	          	| OFF             |
		|	parking_brake	    |	parking_brake	    |	OFF         		| ON              |
		|	abs			          |	abs		          	|	ON	          	| OFF             |
		|	abs		          	|	abs			          |	OFF         		| ON              |
		|	brake_failure	  	|	brake_failure		  |	ON	          	| OFF             |
		|	brake_failure	  	|	brake_failure		  |	OFF         		| ON              |
		|	speed_limit	    	|	speed_limit		    |	ON	          	| OFF             |
		|	speed_limit	    	|	speed_limit		    |	OFF         		| ON              |
		|
	#Input: activate/deactivate *one* OEM static component request on the CAN bus and invert pixel update  framebuffer
	#Output:  safe state signal triggered within TBD ms
	@TEST_FS-488 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Static Pixel update wrong (presence) - Seatbelt Telltale
		Given BUS.seatbelt = OFF
		When BUS.seatbelt = ON
		And pixelbuffer.seatbelt is inverted
		Then safe state signal is set within t_tol_1 ms
		  
		Given BUS.seatbelt = ON
		When BUS.seatbelt = OFF
		And pixelbuffer.seatbelt is inverted
		Then safe state signal is set within t_tol_1 ms

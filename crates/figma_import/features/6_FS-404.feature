@FS-452 @REQ_FS-404
Feature: SMR-004 Verify Static UI Elements Onscreen
	#The Safety Monitor shall verify that OEM designated static UI elements are displayed correctly onscreen when the the associated message bus signals are active.

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
	#Input: activate/deactivate *every* OEM static component request on the CAN bus sequentially
	#Output: no safe state signal triggered
	@TEST_FS-485 @TESTSET_FS-458 @smr_testcase_import1
	Scenario Outline: Verify static UI OEM elements displayed correctly
		Given BUS.<signal> = OFF
		When BUS.<signal> = ON
		Then <telltale> is <status>
		And no errors
		
		Examples:
		|	signal			      |	telltale	  	    |	status	  	    |
		|	lateral_control 	|	lateral_control	  |	displayed	    	|
		|	lateral_control 	|	lateral_control	  |	not displayed		|
		|	cruise_control  	|	cruise_control	  |	displayed	    	|
		|	cruise_control	  |	cruise_control	  |	not displayed		|
		|	tpms_warning	    |	tpms_warning    	|	displayed	    	|
		|	tpms_warning	    |	tpms_warning  	  |	not displayed		|
		|	stability_failure	|	stability_failure	|	displayed		    |
		|	stability_failure	|	stability_failure	|	not displayed		|
		|	sidelights		    |	sidelights		    |	displayed		    |
		|	sidelights		    |	sidelights		    |	not displayed		|
		|	lowbeam		        |	lowbeam		        |	displayed		    |
		|	lowbeam	        	|	lowbeam		        |	not displayed		|
		|	highbeam		      |	highbeam		      |	displayed	    	|
		|	highbeam		      |	highbeam		      |	not displayed		|
		|	driver_airbag		  |	driver_airbag	   	|	displayed		    |
		|	driver_airbag		  |	driver_airbag		  |	not displayed		|
		|	seatbelt		      |	seatbelt		      |	displayed		    |
		|	seatbelt		      |	seatbelt		      |	not displayed		|
		|	parking_brake	    |	parking_brake	    |	displayed		    |
		|	parking_brake	    |	parking_brake	    |	not displayed		|
		|	abs			          |	abs		          	|	displayed	    	|
		|	abs		          	|	abs			          |	not displayed		|
		|	brake_failure	  	|	brake_failure		  |	displayed		    |
		|	brake_failure	  	|	brake_failure		  |	not displayed		|
		|	speed_limit	    	|	speed_limit		    |	displayed		    |
		|	speed_limit	    	|	speed_limit		    |	not displayed		|
	#Input: activate/deactivate *one* (seatbelt telltale) OEM static component request on the CAN bus
	#Output: no safe state signal triggered
	@TEST_FS-484 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Verify static UI OEM elements displayed correctly - Seatbelt Telltale
		Given BUS.seatbelt = OFF
		When BUS.seatbelt = ON
		Then seatbelt is displayed
		And no errors
		  
		Given BUS.seatbelt = ON
		When BUS.seatbelt = OFF
		Then seatbelt is not displayed
		And no errors

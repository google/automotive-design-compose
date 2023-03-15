@FS-452 @REQ_FS-407
Feature: SMR-007 Evaluate Pixel Buffer
	#The Safety Monitor shall evaluate the correctness of the UI state by directly inspecting on-screen pixel information via a shared memory buffer or other implementation.

	#Input: activate/deactivate *every* OEM static component request on the CAN bus and disable pixel buffer function call
	#Output:  safe state signal triggered within TBD ms
	@TEST_FS-495 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario Outline: Pixel buffer not called
		Given BUS.<signal> = OFF
		When BUS.<signal> = ON
		And pixelbuffer is not called
		Then safe state signal is set within t_tol_1 ms
		  
		Examples:
		|	signal			      |
		|	lateral_control 	|
		|	lateral_control 	|
		|	cruise_control  	|
		|	cruise_control	  |
		|	tpms_warning	    |
		|	tpms_warning	    |
		|	stability_failure	|
		|	stability_failure	|
		|	sidelights		    |
		|	sidelights		    |
		|	lowbeam		        |
		|	lowbeam	        	|
		|	highbeam		      |
		|	highbeam		      |
		|	driver_airbag		  |
		|	driver_airbag		  |
		|	seatbelt		      |
		|	seatbelt		      |
		|	parking_brake	    |
		|	parking_brake	    |
		|	abs			          |
		|	abs		          	|
		|	brake_failure	  	|
		|	speed_limit	    	|
		|	speed_limit	    	|
	#Input: activate/deactivate *one* OEM static component request on the CAN bus and modify pixel buffer output
	#Output:  safe state signal triggered within TBD ms
	@TEST_FS-493 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Pixel buffer output wrong - seatbelt telltale displayed wrong color
		Given BUS.seatbelt = OFF
		When BUS.seatbelt = ON
		And pixelbuffer.seatbelt is green
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

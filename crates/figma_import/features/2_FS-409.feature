@FS-452 @REQ_FS-409
Feature: SMR-009 Recieve Timestamps From UI
	#The Safety Monitor shall check timestamp information via a shared memory buffer or some other mechanism from the current UI rendering process, to ensure the UI is rendering in a timely and regular fashion.

	#Input: activate/deactivate one OEM static component request on the CAN bus and remove timestamp information
	#Output:  safe state signal triggered within TBD ms
	@TEST_FS-500 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Timestamps from UI missing - Seatbelt Telltale
		Given BUS.seatbelt = OFF
		When BUS.seatbelt = ON
		And pixelbuffer.timestamp is zero
		Then safe state signal is set within t_tol_1 ms
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

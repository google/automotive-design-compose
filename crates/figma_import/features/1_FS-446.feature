@FS-452 @REQ_FS-446
Feature: SMR-038 There shall be a UI Heartbeat Message that is monitored by Safety Monitor 
	#The vehicle.software UI implementation shall provide a heartbeat message during specific periods where it is not providing the Safety Monitor with pixel buffers with a frequency sufficient to satisfy the system watchdog.

	#Input: system in idle state (e.g. no active rendering) and heartbeat message missing
	#Output: watchdog triggered after TBD ms
	@TEST_FS-516 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: UI heartbeat missing - Seatbelt Telltale
		Given BUS.seatbelt = OFF
		And pixelbuffer.heartbeat is zero
		Then safe state signal is set within t_tol_1 ms
	#Input: system in idle state (e.g. no active rendering) and heartbeat message frozen
	#Output: watchdog triggered after TBD ms
	@TEST_FS-515 @TESTSET_FS-458 @smr_testcase_import1
	Scenario: UI heartbeat frozen
		Given BUS.seatbelt = OFF
		When pixelbuffer.heartbeat is constant
		Then safe state signal is set within t_tol_1 ms
	#Input: system in idle state (e.g. no active rendering) and heartbeat message delayed
	#Output: watchdog triggered when delay is bigger than TBD
	@TEST_FS-514 @TESTSET_FS-458 @smr_testcase_import1
	Scenario: UI heartbeat too late
		Given BUS.seatbelt = OFF
		And pixelbuffer.heartbeat is late
		Then safe state signal is set within t_tol_1 ms
	#Input: system in idle state (e.g. no active rendering)
	#Output: safety monitor state and logic updating watchdog every TBD ms
	@TEST_FS-513 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Tickle Watchdog after UI heartbeat
		Given state = runtime
		And heartbeat is sent
		Then watchdog is tickled
		And no errors

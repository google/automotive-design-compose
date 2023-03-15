@FS-452 @REQ_FS-408
Feature: SMR-008 Update Watchdog
	#The Safety Monitor shall update an OEM or operating system specific watchdog regularly to allow the system to detect when it has entered into a frozen or hung state.

	#Input: system in active state and Safety monitor state and logic function not called
	#Output:  watchdog triggered
	@TEST_FS-498 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Safety monitor state and logic not called - Seatbelt Telltale
		Given BUS.seatbelt = OFF
		When BUS.seatbelt = ON
		And Safety monitor state and logic is not called
		Then safe state signal is set within t_tol_1 ms
	#Input: system in active state
	#Output: safety monitor state and logic updating watchdog every TBD ms
	@TEST_FS-497 @TESTSET_FS-458 @smr_testcase_import1_p1
	Scenario: Tickle Watchdog after Pixel Update
		Given state = runtime
		And BUS.seatbelt = OFF
		When BUS.seatbelt = ON
		And EvaluateSafeState is completed
		Then watchdog is tickled
		And no errors

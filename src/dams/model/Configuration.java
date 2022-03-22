package dams.model;

import java.util.Map;

public class Configuration {

	/**
	 * Host names
	 */
	public final static String HOSTNAME = "localhost";

	/**
	 * Port numbers
	 */
	public final static int SERVER_PORT_NUMBER = 7777;
	public final static int MTL_LISTENER_PORT_NUMBER = 8888;
	public final static int QUE_LISTENER_PORT_NUMBER = 8889;
	public final static int SHE_LISTENER_PORT_NUMBER = 8890;

	/**
	 * Admin details Of Montreal Server
	 */
	public final static String MTL_ADMIN_ID = "MTLA0000";

	/**
	 * Admin details Of Quebec Server
	 */
	public final static String QUE_ADMIN_ID = "QUEA0000";

	/**
	 * Admin details Of Sherbrooke Server
	 */
	public final static String SHE_ADMIN_ID = "QUEA0000";

	/**
	 * Path
	 */
	public final static String LOG_FILE_PATH = "logs/servers/";
	public final static String ADMIN_LOG_FILE_PATH = "logs/admin/";
	public final static String PATIENT_LOG_FILE_PATH = "logs/patient/";

	/**
	 * HashMap for type of appointment
	 */
	public final static Map<String, AppointmentType> MAP_OF_APPOINTMENT_TYPE = Map.of("PHYSICIAN",
			AppointmentType.PHYSICIAN, "DENTAL", AppointmentType.DENTAL, "SURGEON", AppointmentType.SURGEON);

}

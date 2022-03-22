package dams.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dams.model.AppointmentDetails;
import dams.model.AppointmentType;
import dams.model.Configuration;

public class SHEHospitalServer extends UnicastRemoteObject implements HospitalServerInterface {
	private static final long serialVersionUID = 1L;

	private Map<AppointmentType, HashMap<String, AppointmentDetails>> database = new HashMap<AppointmentType, HashMap<String, AppointmentDetails>>();
	private final static String LOG_FILE_NAME = "SHEServer_log_file.log";

	public SHEHospitalServer() throws RemoteException {
		super();
		addTestData();
	}

	private String sharebrookeUDPClient(int portNumber, String message) {
		// TODO Auto-generated method stub
		byte[] buffer = new byte[1024];
		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket();
			buffer = message.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
					InetAddress.getByName(Configuration.HOSTNAME), portNumber);
			socket.send(packet);

			byte[] receiveBuffer = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
			socket.receive(receivePacket);
			socket.close();
			String receivedResponse = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

			System.out.println(receivedResponse);
			return receivedResponse;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	private void addTestData() {
		// TODO Auto-generated method stub
		String patient3 = "SHEP5565";
		String patient4 = "SHEP2475";
		String patient5 = "MTLP2345";

		HashMap<String, AppointmentDetails> input1 = new HashMap<String, AppointmentDetails>();

		AppointmentDetails details2 = new AppointmentDetails(AppointmentType.PHYSICIAN, "SHEE080222", 5);
		details2.addPatient(patient3);
		details2.addPatient(patient4);
		details2.addPatient(patient5);
		input1.put("SHEE080222", details2);

		HashMap<String, AppointmentDetails> input2 = new HashMap<String, AppointmentDetails>();

		AppointmentDetails details6 = new AppointmentDetails(AppointmentType.DENTAL, "SHEA050222", 5);
		details6.addPatient(patient3);
		details6.addPatient(patient4);
		input2.put("SHEA050222", details6);

		HashMap<String, AppointmentDetails> input3 = new HashMap<String, AppointmentDetails>();

		AppointmentDetails details8 = new AppointmentDetails(AppointmentType.SURGEON, "SHEE070222", 3);
		details8.addPatient(patient3);
		details8.addPatient(patient4);
		details8.addPatient(patient5);
		input3.put("SHEE070222", details8);

		database.put(AppointmentType.PHYSICIAN, input1);
		database.put(AppointmentType.DENTAL, input2);
		database.put(AppointmentType.SURGEON, input3);

	}

	@Override
	public String addAppointment(String appointmentID, AppointmentType appointmentType, int capacity)
			throws RemoteException {
		// TODO Auto-generated method stub
		String response = "";

		String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		List<String> requestParameters = new ArrayList<String>();
		requestParameters.add(appointmentID);
		requestParameters.add(appointmentType.toString());
		requestParameters.add(capacity + "");

		boolean completed = true;

		if (!appointmentID.substring(0, 3).equals("SHE")) {
			completed = false;
			response = "Failed: Cannot book appointment Id - " + appointmentID + " by Sherbrooke server";
		} else if (!database.containsKey(appointmentType)) {
			HashMap<String, AppointmentDetails> data = new HashMap<String, AppointmentDetails>();
			AppointmentDetails details = new AppointmentDetails(appointmentType, appointmentID, capacity);
			data.put(appointmentID, details);
			database.put(appointmentType, data);

			response = "Success: Appointment Added";
		} else {
			HashMap<String, AppointmentDetails> data = database.get(appointmentType);

			if (!data.containsKey(appointmentID)) {
				AppointmentDetails details = new AppointmentDetails(appointmentType, appointmentID, capacity);
				data.put(appointmentID, details);
				database.put(appointmentType, data);

				response = "Success: Appointment Added";
			} else {
				completed = false;
				response = "Failed: Already present Appintment id and Appointment type";
			}
		}

		System.out.println();
		System.out.println("Add appointment");
		System.out.println(response);
		addRecordIntoLogFile(requestDate, "Add appointment", requestParameters, completed, response);
		return response;
	}

	@Override
	public String removeAppointment(String appointmentID, AppointmentType appointmentType) throws RemoteException {
		// TODO Auto-generated method stub
		String response = "";

		String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		List<String> requestParameters = new ArrayList<String>();
		requestParameters.add(appointmentID);
		requestParameters.add(appointmentType.toString());

		boolean completed = false;

		if (database.containsKey(appointmentType)) {
			HashMap<String, AppointmentDetails> data = database.get(appointmentType);
			if (data.containsKey(appointmentID)) {
				AppointmentDetails details = data.get(appointmentID);
				if (details.getAvailableSpace() == details.getCapacity()) {
					data.remove(appointmentID);
					response = "Success: Appointment is removed with appointmentId: " + appointmentID;
				} else {
					List<String> patientIds = details.getPatientListIds();
					boolean removed = bookNextAvailableAppointment(patientIds, appointmentID, appointmentType);
					data.remove(appointmentID);

					if (removed) {
						response = "Success: Patient is transferred to available appointment and removed the appointmentId: "
								+ appointmentID;
					} else {
						response = "Success: Not able to find available appointment and removed the appointmentId: "
								+ appointmentID;
					}
				}
				completed = true;
			} else {
				response = "Failed: No appointment is available with appointmentId: " + appointmentID;
			}
		} else {
			response = "Failed: No appointment is available with appointmentType: " + appointmentType;
		}

		System.out.println();
		System.out.println("Remove appointment");
		System.out.println(response);
		addRecordIntoLogFile(requestDate, "Remove appointment", requestParameters, completed, response);
		return response;
	}

	private boolean bookNextAvailableAppointment(List<String> patientIds, String appointmentID,
			AppointmentType appointmentType) {
		// TODO Auto-generated method stub
		boolean success = false;

		if (database.containsKey(appointmentType)) {
			HashMap<String, AppointmentDetails> data = database.get(appointmentType);
			List<String> allAppointmentIds = new ArrayList<String>(data.keySet());

			Map<String, Integer> shiftPriority = new HashMap<String, Integer>();
			shiftPriority.put("A", 2);
			shiftPriority.put("M", 1);
			shiftPriority.put("E", 3);

			Comparator<String> idCompare = new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					// TODO Auto-generated method stub
					String shift1 = o1.substring(3, 4);
					String shift2 = o2.substring(3, 4);

					String day1 = o1.substring(4, 6);
					String day2 = o2.substring(4, 6);

					String month1 = o1.substring(6, 8);
					String month2 = o2.substring(6, 8);

					if (month1.compareTo(month2) == 0) {
						if (day1.compareTo(day2) == 0) {
							return shift1.compareTo(shift2);
						}
						return day1.compareTo(day2);
					}
					return shift1.compareTo(shift2);
				}
			};

			Collections.sort(allAppointmentIds, idCompare);

			int i = 0;
			for (String id : allAppointmentIds) {
				if (idCompare.compare(id, appointmentID) > 0) {
					AppointmentDetails details = data.get(id);
					while (i < patientIds.size() && details.getAvailableSpace() > 0) {
						details.addPatient(patientIds.get(i));
						i = i + 1;
					}
				}
			}

			if (i >= patientIds.size()) {
				success = true;
			}

		}

		return success;
	}

	@Override
	public String listAppointmentAvailability(AppointmentType appointmentType) throws RemoteException {
		// TODO Auto-generated method stub
		StringBuilder response = new StringBuilder();
		response.append("List appointment " + appointmentType.toString() + ": [");
		boolean completed = false;
		String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		List<String> requestParameters = new ArrayList<String>();
		requestParameters.add(appointmentType.toString());

		response.append(sherbrookeListAppointmenOfType(appointmentType));

		String message = "listAppointment:" + appointmentType.toString();

		Runnable montrealThread = () -> {
			String montrealResponse = sharebrookeUDPClient(Configuration.MTL_LISTENER_PORT_NUMBER, message);
			response.append(montrealResponse);
			addRecordIntoLogFile(requestDate, "Received UDP response from Montreal server", requestParameters,
					completed, montrealResponse);
		};

		Runnable quebecThread = () -> {
			String quebecResponse = sharebrookeUDPClient(Configuration.QUE_LISTENER_PORT_NUMBER, message);
			response.append(quebecResponse);
			addRecordIntoLogFile(requestDate, "Received UDP response from quebec server", requestParameters, completed,
					quebecResponse);
		};

		Thread montrealThreadObj = new Thread(montrealThread);
		montrealThreadObj.start();
		String msg = "Sent UDP request to Montreal server:" + appointmentType + " list appointment availability";
		addRecordIntoLogFile(requestDate, msg, requestParameters, completed, "");

		Thread quebecThreadObj = new Thread(quebecThread);
		quebecThreadObj.start();

		msg = "Sent UDP request to Quebec server:" + appointmentType + " list appointment availability";
		addRecordIntoLogFile(requestDate, msg, requestParameters, completed, "");

		while (montrealThreadObj.isAlive() || quebecThreadObj.isAlive())
			;
		response.append("]");

		addRecordIntoLogFile(requestDate, "List Appointment Availability of " + appointmentType, requestParameters,
				completed, response.toString());

		System.out.println();
		System.out.println("List appointment");
		System.out.println(response.toString());
		return response.toString();
	}

	public String sherbrookeListAppointmenOfType(AppointmentType appointmentType) {
		StringBuilder response = new StringBuilder();
		if (database.containsKey(appointmentType)) {
			HashMap<String, AppointmentDetails> data = database.get(appointmentType);
			data.forEach((key, value) -> response.append(key + "  " + value.getAvailableSpace() + ", "));
		}

		return response.toString();
	}

	@Override
	public String bookAppointment(String patientID, String appointmentID, AppointmentType appointmentType)
			throws RemoteException {
		// TODO Auto-generated method stub
		StringBuilder response = new StringBuilder();

		String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		List<String> requestParameters = new ArrayList<String>();
		requestParameters.add(patientID);
		requestParameters.add(appointmentID);
		requestParameters.add(appointmentType.toString());

		boolean completed = false;

		String appointmentSchedule = getAppointmentSchedule(patientID);
		String validationResult = validationOfAppointmentId(appointmentID, appointmentSchedule, appointmentType);

		if (validationResult.contains("Failed")) {
			addRecordIntoLogFile(requestDate, "Book appointment", requestParameters, completed, validationResult);
			return validationResult;
		}

		String message = "bookAppointment:" + patientID + "," + appointmentID + "," + appointmentType.toString();

		if (appointmentID.startsWith("SHE")) {
			response.append(sherbrookeBookAppointment(patientID, appointmentID, appointmentType));
		} else if (appointmentID.startsWith("QUE")) {

			Runnable quebecThread = () -> {
				String quebecResponse = sharebrookeUDPClient(Configuration.QUE_LISTENER_PORT_NUMBER, message);
				response.append(quebecResponse);
				addRecordIntoLogFile(requestDate, "Received UDP response from quebec server", requestParameters,
						completed, quebecResponse);
			};

			Thread quebecThreadObj = new Thread(quebecThread);
			quebecThreadObj.start();

			String msg = "Sent UDP request to Quebec server:" + appointmentID + " book appointment";
			addRecordIntoLogFile(requestDate, msg, requestParameters, completed, "");

			while (quebecThreadObj.isAlive())
				;

		} else if (appointmentID.startsWith("MTL")) {

			Runnable montrealThread = () -> {
				String montrealResponse = sharebrookeUDPClient(Configuration.MTL_LISTENER_PORT_NUMBER, message);
				response.append(montrealResponse);
				addRecordIntoLogFile(requestDate, "Received UDP response from Montreal server", requestParameters,
						completed, montrealResponse);
			};

			Thread sharebrookeThreadObj = new Thread(montrealThread);
			sharebrookeThreadObj.start();
			String msg = "Sent UDP request to Montreal server:" + appointmentType + " book appointment";
			addRecordIntoLogFile(requestDate, msg, requestParameters, completed, "");
			while (sharebrookeThreadObj.isAlive())
				;
		}

		System.out.println();
		System.out.println("Book appointment");
		System.out.println(response.toString());
		addRecordIntoLogFile(requestDate, "Book appointment", requestParameters, completed, response.toString());
		return response.toString();
	}

	public String sherbrookeBookAppointment(String patientID, String appointmentID, AppointmentType appointmentType) {
		StringBuilder response = new StringBuilder();

		if (database.containsKey(appointmentType)) {
			HashMap<String, AppointmentDetails> data = database.get(appointmentType);
			if (data.containsKey(appointmentID)) {
				AppointmentDetails details = data.get(appointmentID);
				if (!details.getPatientListIds().contains(patientID)) {
					if (details.addPatient(patientID)) {
						response.append("Success: Appointment successfully booked");
					} else {
						response.append("Failed: No Slot available for selected slot");
					}

				} else {
					response.append("Failed: Patient has already booked appointment with [" + appointmentType + ", "
							+ appointmentID + "]");
				}

			} else {
				response.append("Failed: No appointment available for selected slot");
			}
		} else {
			response.append("Failed: No appointment available for selected appointment type");
		}

		return response.toString();
	}

	private String validationOfAppointmentId(String appointmentID, String appointmentSchedule,
			AppointmentType appointmentType) {
		// TODO Auto-generated method stub

		HashMap<Integer, Integer> weekCount = new HashMap<Integer, Integer>();

		String schedules = appointmentSchedule.substring(34, appointmentSchedule.length() - 1);

		if (!schedules.equals("")) {
			String allIds[] = schedules.split(",");

			ArrayList<ArrayList<String>> idRecords = new ArrayList<ArrayList<String>>();

			for (String id : allIds) {
				ArrayList<String> tempList = new ArrayList<String>();
				tempList.add(id.split(":")[0]);
				tempList.add(id.split(":")[1]);

				idRecords.add(tempList);
			}

			for (ArrayList<String> record : idRecords) {
				if (record.get(0).equals(appointmentType.toString())
						&& record.get(1).substring(4).equals(appointmentID.substring(4))) {
					return "Failed: Patient has already booked appointment in same day with"
							+ appointmentType.toString();
				} else if (!record.get(1).startsWith("SHE")) {

					try {
						String input = record.get(1).substring(4);
						String format = "ddMMyy";

						SimpleDateFormat df = new SimpleDateFormat(format);
						Date date = df.parse(input);
						Calendar cal = Calendar.getInstance();
						cal.setTime(date);
						int week = cal.get(Calendar.WEEK_OF_YEAR);

						if (weekCount.containsKey(week)) {
							weekCount.put(week, weekCount.get(week) + 1);
						} else {
							weekCount.put(week, 1);
						}
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			System.out.println(weekCount.toString());

			try {
				String format = "ddMMyy";

				SimpleDateFormat df = new SimpleDateFormat(format);
				Date date = df.parse(appointmentID.substring(4));
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				int appointmentWeekNumber = cal.get(Calendar.WEEK_OF_YEAR);

				if (weekCount.containsKey(appointmentWeekNumber) && weekCount.get(appointmentWeekNumber) > 2) {
					return "Failed: Patient has already booked 3 appointment other than SherBrooke Server";
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return "Success: Patient can book the appointment";

	}

	@Override
	public String getAppointmentSchedule(String patientID) throws RemoteException {
		// TODO Auto-generated method stub
		StringBuilder response = new StringBuilder();
		response.append("Appointment Schedule of " + patientID + " [");

		boolean completed = false;
		String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		List<String> requestParameters = new ArrayList<String>();
		requestParameters.add(patientID);

		response.append(sharebrookeAppointmentScheduleOfPatientId(patientID));

		String message = "getSchedule:" + patientID;

		Runnable montrealThread = () -> {
			String montrealResponse = sharebrookeUDPClient(Configuration.MTL_LISTENER_PORT_NUMBER, message);
			response.append(montrealResponse);
			addRecordIntoLogFile(requestDate, "Received UDP response from Motreal server", requestParameters, completed,
					montrealResponse);
		};

		Runnable quebecThread = () -> {
			String quebecResponse = sharebrookeUDPClient(Configuration.QUE_LISTENER_PORT_NUMBER, message);
			response.append(quebecResponse);
			addRecordIntoLogFile(requestDate, "Received UDP response from quebec server", requestParameters, completed,
					quebecResponse);
		};

		Thread montrealThreadObj = new Thread(montrealThread);
		montrealThreadObj.start();
		String msg = "Sent UDP request to Montreal server:" + patientID + " getAppointmentSchedule";
		addRecordIntoLogFile(requestDate, msg, requestParameters, completed, response.toString());

		Thread quebecThreadObj = new Thread(quebecThread);
		quebecThreadObj.start();
		msg = "Sent UDP request to Quebec server:" + patientID + " getAppointmentSchedule";
		addRecordIntoLogFile(requestDate, msg, requestParameters, completed, response.toString());

		while (quebecThreadObj.isAlive() || montrealThreadObj.isAlive())
			;

		response.append("]");

		System.out.println();
		System.out.println("Get appointment Schedule");
		System.out.println(response.toString());
		addRecordIntoLogFile(requestDate, "Get appointment Schedule", requestParameters, completed,
				response.toString());

		return response.toString();
	}

	public String sharebrookeAppointmentScheduleOfPatientId(String patientID) {
		StringBuilder response = new StringBuilder();

		for (Map.Entry<AppointmentType, HashMap<String, AppointmentDetails>> entry : database.entrySet()) {
			for (Map.Entry<String, AppointmentDetails> record : entry.getValue().entrySet()) {
				if (record.getValue().getPatientListIds().contains(patientID)) {
					response.append(entry.getKey() + ":" + record.getKey() + ",");
				}
			}
		}

		return response.toString();
	}

	@Override
	public String cancelAppointment(String patientID, String appointmentID) throws RemoteException {
		// TODO Auto-generated method stub
		String response = "Failed: No record found of [" + patientID + ", " + appointmentID + "]";

		String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		List<String> requestParameters = new ArrayList<String>();
		requestParameters.add(appointmentID);
		requestParameters.add(patientID);

		boolean completed = false;

		for (Map.Entry<AppointmentType, HashMap<String, AppointmentDetails>> entry : database.entrySet()) {
			for (Map.Entry<String, AppointmentDetails> record : entry.getValue().entrySet()) {
				if (record.getKey().equals(appointmentID)
						&& record.getValue().getPatientListIds().contains(patientID)) {
					record.getValue().removePatientId(patientID);
					response = "Success: Cancelled appointment with " + appointmentID;
					completed = true;
				}
			}
		}

		System.out.println();
		System.out.println("Cancel appointment");
		System.out.println(response);
		addRecordIntoLogFile(requestDate, "Cancel appointment", requestParameters, completed, response);
		return response;
	}

	public void addRecordIntoLogFile(String requestDate, String requestType, List<String> requestParameter,
			boolean completed, String response) {
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter pw = null;
		try {

			File f = new File(Configuration.LOG_FILE_PATH + SHEHospitalServer.LOG_FILE_NAME);
			if (!f.exists()) {
				new File(Configuration.LOG_FILE_PATH).mkdirs();
				f.createNewFile();
			}

			fw = new FileWriter(Configuration.LOG_FILE_PATH + SHEHospitalServer.LOG_FILE_NAME, true);
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(requestDate + "|" + requestType + "|" + requestParameter.toString() + "|" + response + "|"
					+ completed + "|");
			pw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				pw.close();
				bw.close();
				fw.close();
			} catch (IOException io) {
			}
		}

	}
}

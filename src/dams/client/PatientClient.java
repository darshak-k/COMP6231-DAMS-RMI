package dams.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import dams.model.AppointmentType;
import dams.model.Configuration;
import dams.server.HospitalServerInterface;

public class PatientClient {
	private static HospitalServerInterface hospitalServer = null;
	private static Registry registry = null;
	private static String patientId = "";

	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("Usage: <PatientClient> <patientId>");
			return;
		}

		patientId = args[0];

		if (!verifyPatientDetails(patientId)) {
			System.out.println("Not Valid Patient ID: <MTL/QUE/SHE><P><XXXX>");
			return;
		}

		String serverName = patientId.substring(0, 3);
		System.out.println("Logged in User, " + patientId + "!");

		// find the remote object and cast it to an interface object
		runPatientOption(serverName);
	}

	private static void runPatientOption(String serverName) {
		// TODO Auto-generated method stub
		String registryURL = "rmi://" + Configuration.HOSTNAME + ":" + Configuration.SERVER_PORT_NUMBER + "/"
				+ serverName + "Server";

		try {
			registry = LocateRegistry.getRegistry(Configuration.SERVER_PORT_NUMBER);
			hospitalServer = (HospitalServerInterface) registry.lookup(registryURL);

			runPatientAllOptions();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void runPatientAllOptions() {
		// TODO Auto-generated method stub
		Scanner scanner = new Scanner(System.in);
		String logFileName = patientId + ".log";
		boolean exit = false;
		try {
			while (!exit) {
				System.out.println("===========================================");
				System.out.println("                   MENU                    ");
				System.out.println("===========================================");
				System.out.println("1. Book Appointment");
				System.out.println("2. Get Appointment SChedule");
				System.out.println("3. Cancel Appointment");
				System.out.println("4. Exit");
				System.out.println("");

				System.out.print("Enter your choice > ");
				int choice = scanner.nextInt();

				String requestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
						.format(Calendar.getInstance().getTime());

				if (choice == 1) {
					AppointmentType appointmentType = getAppointmentType();
					String appointmentId = getAppointmentId();

					String serverResponse = hospitalServer.bookAppointment(patientId, appointmentId, appointmentType);

					List<String> requestParameters = new ArrayList<String>();
					requestParameters.add(patientId);
					requestParameters.add(appointmentId);
					requestParameters.add(appointmentType.toString());

					boolean completed = serverResponse.contains("Success");

					addRecordIntoLogFile(logFileName, requestDate, "Book appointment by " + patientId,
							requestParameters, completed, serverResponse);

					System.out.println();
					System.out.println(serverResponse);
				} else if (choice == 2) {
					String serverResponse = hospitalServer.getAppointmentSchedule(patientId);

					List<String> requestParameters = new ArrayList<String>();
					requestParameters.add(patientId);

					boolean completed = serverResponse.contains("Success");

					addRecordIntoLogFile(logFileName, requestDate, "Get Appointment Schedule", requestParameters,
							completed, serverResponse);

					System.out.println();
					System.out.println(serverResponse);
				} else if (choice == 3) {
					String appointmentId = getAppointmentId();
					List<String> requestParameters = new ArrayList<String>();
					requestParameters.add(patientId);
					requestParameters.add(appointmentId);

					String serverResponse = hospitalServer.cancelAppointment(patientId, appointmentId);
					boolean completed = serverResponse.contains("Success");

					addRecordIntoLogFile(logFileName, requestDate, "Cancel Appointment Schedule", requestParameters,
							completed, serverResponse);

					System.out.println();
					System.out.println(serverResponse);
				} else if (choice == 4) {
					exit = true;
				} else {
					System.out.println("Not valid option. Try again.");
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static void addRecordIntoLogFile(String fileName, String requestDate, String requestType,
			List<String> requestParameter, boolean completed, String response) {
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter pw = null;
		try {

			File f = new File(Configuration.PATIENT_LOG_FILE_PATH + fileName);
			if (!f.exists()) {
				new File(Configuration.PATIENT_LOG_FILE_PATH).mkdirs();
				f.createNewFile();
			}
			

			fw = new FileWriter(Configuration.PATIENT_LOG_FILE_PATH + fileName, true);
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(requestDate + " | " + requestType + " | " + requestParameter.toString() + " | " + response
					+ " | " + completed);
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

	private static int getCapacity() {
		// TODO Auto-generated method stub
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter Capacity > ");
		int capacity = scanner.nextInt();

		return capacity;
	}

	private static String getAppointmentId() {
		// TODO Auto-generated method stub
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter appointmentID [(MTL|SHE|QUE)(A|E|M)(MMDDYY)] > ");
		String appointmentId = scanner.next();
		
		while (!appointmentId.matches("(MTL|SHE|QUE)(A|E|M)(0[1-9]|1[0-9]|2[0-9]|3[01])(0[1-9]|11|12)(0[1-9]|1[0-9]|2[0-2])")) {
			System.out.print("Invalid ID Try again. Enter appointmentID [(MTL|SHE|QUE)(A|E|M)(MMDDYY)] > ");
			appointmentId = scanner.next();
		}


		return appointmentId;
	}

	private static AppointmentType getAppointmentType() {
		// TODO Auto-generated method stub
		Scanner scanner = new Scanner(System.in);
		boolean exit = false;
		System.out.println("===========================================");
		System.out.println("            Appointment Type               ");
		System.out.println("===========================================");
		System.out.println("1. Physician");
		System.out.println("2. Surgeon");
		System.out.println("3. Dental");
		System.out.println("4. Exit");
		System.out.println("");

		System.out.print("Enter your choice > ");
		while (!exit) {

			int choice = scanner.nextInt();

			if (choice == 1) {
				return AppointmentType.PHYSICIAN;
			} else if (choice == 2) {
				return AppointmentType.SURGEON;
			} else if (choice == 3) {
				return AppointmentType.DENTAL;
			} else {
				System.out.println("Try again. Enter your choice > ");
			}
		}

		return null;
	}

	private static boolean verifyPatientDetails(String patientID) {
		// TODO Auto-generated method stub
		if (patientID.startsWith("MTLP") && patientID.length() == 8) {
			return true;
		}

		if (patientID.startsWith("QUEP") && patientID.length() == 8) {
			return true;
		}

		if (patientID.startsWith("SHEP") && patientID.length() == 8) {
			return true;
		}

		return false;
	}
}

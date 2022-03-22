package dams.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import dams.model.AppointmentType;

public interface HospitalServerInterface extends Remote {

	public String addAppointment(String appointmentID, AppointmentType appointmentType, int capacity) throws RemoteException;

	public String removeAppointment(String appointmentID, AppointmentType appointmentType) throws RemoteException;

	public String listAppointmentAvailability(AppointmentType appointmentType) throws RemoteException;

	public String bookAppointment(String patientID, String appointmentID, AppointmentType appointmentType)
			throws RemoteException;

	public String getAppointmentSchedule(String patientID) throws RemoteException;

	public String cancelAppointment(String patientID, String appointmentID) throws RemoteException;
}

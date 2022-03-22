package dams.model;

import java.util.ArrayList;
import java.util.List;

public class AppointmentDetails {
	private AppointmentType appointmentType;
	private String appointmentId;
	private int capacity;
	private List<String> patientListIds;

	public AppointmentDetails(AppointmentType appointmentType, String appointmentId, int capacity) {
		this.appointmentType = appointmentType;
		this.appointmentId = appointmentId;
		this.capacity = capacity;
		this.patientListIds = new ArrayList<String>();
	}

	public boolean addPatient(String patientDetails) {
		if (!patientListIds.contains(patientDetails)) {
			if (getAvailableSpace() > 0) {
				patientListIds.add(patientDetails);
				return true;
			}
		}

		return false;
	}

	public int getAvailableSpace() {
		return capacity - patientListIds.size();
	}

	public AppointmentType getAppointmentType() {
		return appointmentType;
	}

	public void setAppointmentType(AppointmentType appointmentType) {
		this.appointmentType = appointmentType;
	}

	public String getAppointmentId() {
		return appointmentId;
	}

	public void setAppointmentId(String appointmentId) {
		this.appointmentId = appointmentId;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public List<String> getPatientListIds() {
		return patientListIds;
	}

	public void setPatientListIds(List<String> patientListIds) {
		this.patientListIds = patientListIds;
	}

	public boolean removePatientId(String patientListId) {
		if (patientListIds.contains(patientListId)) {
			patientListIds.remove(patientListId);
			return true;
		}

		return false;
	}

}

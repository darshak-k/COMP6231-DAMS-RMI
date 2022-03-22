package dams.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import dams.model.AppointmentType;
import dams.model.Configuration;

public class SherbrookeServer {

	private static String sherbrookeRegistryURL = "rmi://" + Configuration.HOSTNAME + ":"
			+ Configuration.SERVER_PORT_NUMBER + "/SHEServer";
	private static Registry registry = null;
	private static SHEHospitalServer serverObj = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			registry = getRegistry(Configuration.SERVER_PORT_NUMBER);
			serverObj = new SHEHospitalServer();
			registry.bind(sherbrookeRegistryURL, serverObj);

			System.out.println("Sherbrooke server is running..");

			sherbrookeUDPServer();

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void sherbrookeUDPServer() {
		// TODO Auto-generated method stub
		boolean running = true;
		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket(Configuration.SHE_LISTENER_PORT_NUMBER);

			while (running) {
				byte[] buffer = new byte[1024];
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				String received = new String(packet.getData(), 0, packet.getLength()).trim();
				System.out.println(received);
				String parameters = received.split(":")[1];
				String response = "";
				if (received.startsWith("listAppointment")) {
					AppointmentType appointmentType = Configuration.MAP_OF_APPOINTMENT_TYPE.get(parameters);
					response = serverObj.sherbrookeListAppointmenOfType(appointmentType);
				} else if (received.startsWith("getSchedule")) {
					response = serverObj.sharebrookeAppointmentScheduleOfPatientId(parameters);
				} else if (received.startsWith("bookAppointment")) {
					String[] otherParameters = parameters.split(",");
					AppointmentType appointmentType = Configuration.MAP_OF_APPOINTMENT_TYPE.get(otherParameters[2]);

					response = serverObj.sherbrookeBookAppointment(otherParameters[0], otherParameters[1],
							appointmentType);
				}
				
				buffer = new byte[1024];
				buffer = response.getBytes();
				packet = new DatagramPacket(buffer, buffer.length, address, port);
				socket.send(packet);
			}
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Registry getRegistry(int portNumber) throws RemoteException {
		try {
			Registry registry = LocateRegistry.getRegistry(portNumber);
			registry.list();
			return registry;

		} catch (RemoteException e) {
			Registry registry = LocateRegistry.createRegistry(portNumber);
			return registry;
		}
	}

}

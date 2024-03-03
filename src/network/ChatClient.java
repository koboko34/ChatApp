package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class ChatClient {

	private static Socket socket;
	
	private static Scanner in;
	private static Scanner serverIn;
	private static PrintWriter serverOut;
		
	private static String name;
	
	private static HashSet<String> userNames;
	
	private static ArrayList<String> pingResponders;
	
	public static void main(String[] args) throws IOException {
		in = new Scanner(System.in);
		
		System.out.println("Enter in an IP address to connect to:");
		String address = in.nextLine();
		
		// ask for port number to attempt to connect to
		// checks for valid port between 0 and 65535
		int port = -1;
		while (port == -1) {
			System.out.println("Enter in a port to connect to:");
			String portString = in.nextLine();
			try {
				port = Integer.parseInt(portString);
				if (port < 0 || port > 65535) {
					System.out.println("Port must be between 0 and 65535!");
					System.out.println("For best results, please use ports 49152 to 65535! (private ports)");
					port = -1;
					continue;
				}
			} catch (Exception e) {
				System.out.println("Port not valid! Please input numbers only!");
			}
		}
		
		// attempts to establish a connection to the server
		// prepares input and output streams for communication with server
		try {
			socket = new Socket(address, port);
			serverIn = new Scanner(socket.getInputStream());
			serverOut = new PrintWriter(socket.getOutputStream(), true);
			
			System.out.println("Connected!\n");
			
			// register name, server validates to ensure name is unique
			while (true) {
				System.out.println("Please enter in your name: ");
				name = in.nextLine();
				serverOut.println(name);
				
				// gives server time to respond whether the name is unique or not
				while (!serverIn.hasNextLine())
				{
					// if server hasn't responded yet, sleep this thread for
					// 50ms and try again
					Thread.sleep(50);
				}
				
				String response = serverIn.nextLine();
				if (response.equals("NAME_ACCEPTED")) {
					break;
				}
				else {
					System.out.println("Name taken!");
				}
			}
			
			// launches a thread with an instance of Printer for handling incoming messages
			Thread printerThread = new Thread(new Printer(), "printerThread");
			printerThread.start();
			
		} catch (NumberFormatException | IOException | InterruptedException e) {
			e.printStackTrace();
			return;			
		}
		
		// allow user to write messages and run commands
		while (!socket.isClosed()) {
			String s = in.nextLine();
			serverOut.println(s);
			if (s.equals("!quit")) {
				break;
			}
		}
		
		// closing any open connections and streams
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
		if (in != null) {
			in.close();
		}
		if (serverOut != null) {
			serverOut.close();
		}
		if (serverIn != null) {
			serverIn.close();
		}
	}
	
	// This class is responsible for handling incoming messages from the server.
	// Instances of this class will run on its own thread.
	// This instance will also respond to any pings sent by the server.
	private static class Printer implements Runnable {
		
		// stores active users
		// used by coordinator
		private void storeUserNames() {
			userNames = new HashSet<>();
			while (serverIn.hasNextLine()) {
				String s = serverIn.nextLine();
				if (s.equals("NAMES_END")) {
					return;
				}
				userNames.add(s);
			}
		}
		
		@Override
		public void run() {
			serverOut.println("READY");
			
			while (!socket.isClosed()) {
				while (serverIn.hasNextLine()) {
					String message = serverIn.nextLine();
					if (message.equals("QUIT_SUCCESS")) {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					}
					else if (message.equals("NAMES_BEGIN")) {
						storeUserNames();
					}
					else if (message.equals("PING")) {
						serverOut.println("PING");
					}
					else if (message.equals("NEW_COORDINATOR")) {
						Thread pingerThread = new Thread(new Pinger(), "pingerThread");
						pingerThread.start();
					}
					else {
						System.out.println(message);						
					}
				}
				
				// put thread to sleep to save processing power
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// this class is instantiated only on the coordinator
	// responsible for maintaining state of active members by sending pings to all clients
	// clients who fail to respond are considered as disconnected and are removed from active users list
	private static class Pinger implements Runnable {

		@Override
		public void run() {			
			while (!socket.isClosed()) {
				// clear list
				pingResponders = new ArrayList<>();
				
				// send pings to all clients to check if still connected
				serverOut.println("PING_START");
				for (String userName : userNames) {
					serverOut.println(userName);
				}
				serverOut.println("PING_END");
				
				// repeat after 60 seconds
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

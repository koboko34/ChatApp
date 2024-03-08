package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;

public class ChatClient {

	static Socket socket;
	
	static Scanner in;
	public static Scanner serverIn;
	static PrintWriter serverOut;
		
	static String name;
	
	public static HashSet<String> userNames;
		
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
			if (s.isEmpty()) {
				continue;
			}
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
}

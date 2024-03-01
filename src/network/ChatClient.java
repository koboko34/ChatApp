package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ChatClient {

	private static Socket socket;
	
	private static Scanner in;
	private static Scanner serverIn;
	private static PrintWriter serverOut;
		
	private static String name;
	
	public static void main(String[] args) throws IOException {
		in = new Scanner(System.in);
		
		System.out.println("Enter in an IP address:");
		String address = in.nextLine();
		
		// ask for port number to attempt to connect to
		// checks for valid port between 0 and 65535
		int port = -1;
		while (port == -1) {
			System.out.println("Enter in a port:");
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
			Thread printerThread = new Thread(new Printer());
			printerThread.start();
			
		} catch (NumberFormatException | IOException | InterruptedException e) {
			e.printStackTrace();
			return;			
		}
		
		// allow user to write messages and run commands
		while (socket.isConnected()) {
			serverOut.println(in.nextLine());
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
	private static class Printer implements Runnable {
		
		@Override
		public void run() {			
			while (true) {
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
					System.out.println(message);
				}
				
				// put thread to sleep to save processing power
				// messages are polled for 20 times per second
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

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
		
		int port = -1;
		while (port == -1) {
			System.out.println("Enter in a port:");
			String portString = in.nextLine();
			try {
				port = Integer.parseInt(portString);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
		// establish connection and start Printer
		try {
			socket = new Socket(address, port);
			serverIn = new Scanner(socket.getInputStream());
			serverOut = new PrintWriter(socket.getOutputStream(), true);
			
			// register name
			while (true) {
				System.out.println("Please enter in your name: ");
				name = in.nextLine();
				serverOut.println(name);
				
				while (!serverIn.hasNextLine())
				{
					Thread.sleep(5);
				}
				String response = serverIn.nextLine();
				if (response.equals("NAME_ACCEPTED")) {
					break;
				}
				else {
					System.out.println("Name taken!");
				}
			}
			
			Thread printerThread = new Thread(new Printer());
			printerThread.start();
			
		} catch (NumberFormatException | IOException | InterruptedException e) {
			e.printStackTrace();
			return;			
		}
		
		while (socket.isConnected()) {
			serverOut.println(in.nextLine());
		}
		
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
	
	private static class Printer implements Runnable {
		
		@Override
		public void run() {			
			while (true) {
				while (serverIn.hasNextLine()) {
					String message = serverIn.nextLine();
					if (message.equals("QUIT_SUCCESS")) {
						return;
					}
					System.out.println(message);
				}
			}
		}
	}
}

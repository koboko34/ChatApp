package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ChatClient {

	private static Socket socket;
	private static Socket coordinatorSocket = null;
	
	private static Scanner in;
	private static PrintWriter out;
	
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
		
		try {
			socket = new Socket(address, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			out.println("This is a test!\nI want to see if the newline char works.");
		} catch (NumberFormatException | UnknownHostException | IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}

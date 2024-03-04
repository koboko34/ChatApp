package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerContext {
	public static HashMap<Socket, String> activeUsers = new HashMap<>();
	public static Socket coordinatorSocket = null;
	
	private static ServerContext instance = null;
	
	private ServerContext() {
		
	}
	
	public static ServerContext get() {
		if (instance == null) {
			instance = new ServerContext();
		}
		return instance;
	}
	
	public static void init() {
		if (instance == null) {
			get();
		}
		
		Scanner in = new Scanner(System.in);
		
		// ask for port number for the server to listen on
		// checks for valid port between 0 and 65535
		int port = -1;
		while (port == -1) {
			System.out.println("Enter in a port for the server to listen on:");
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
		
		// closing system input stream
		in.close();
		
		// create thread pool which Worker instances will use to handle communication between server and users
		ExecutorService pool = Executors.newFixedThreadPool(500, new NamedThreadFactory("serverWorker"));
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(port);
			System.out.println("Server is running...");
			while (true) {
				pool.execute(new ServerWorker(listener.accept()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (listener != null && !listener.isClosed()) {
				try {
					closeAllSockets();
					listener.close();
					System.out.println("Closing server...");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// closes all sockets which established a connected with the server
	private static void closeAllSockets() {
		for (Socket socket : activeUsers.keySet()) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

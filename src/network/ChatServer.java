package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
	static Scanner in;
	static PrintWriter out;
	
	public static void main(String[] args) {
		ExecutorService pool = Executors.newFixedThreadPool(500);
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(40404);
			System.out.println("Server is running...");
			while (true) {
				pool.execute(new Worker(listener.accept()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (listener != null && !listener.isClosed()) {
				try {
					listener.close();
					System.out.println("Closing server...");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	static HashSet<Socket> activeUsers = new HashSet<>();
	static Socket coordinatorSocket = null;
	
	private static class Worker implements Runnable {
		private Socket socket;
		
		public Worker(Socket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run() {
			System.out.println("Something connected...");
			if (coordinatorSocket == null) {
				coordinatorSocket = socket;
			}
			if (!activeUsers.contains(socket)) {
				activeUsers.add(socket);
			}
			
			try {
				in = new Scanner(socket.getInputStream());
				while (in.hasNextLine()) {
					System.out.println(in.nextLine());					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// handle whatever the user wants
		}
		
	}
}

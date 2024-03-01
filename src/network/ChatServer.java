package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class ChatServer {
	
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
	
	static HashMap<Socket, String> activeUsers = new HashMap<>();
	static Socket coordinatorSocket = null;
	
	static String coordinatorMessage = "Welcome! The current coordinator is " +
			activeUsers.get(coordinatorSocket);
	static String userCountMessage = "Number of users in chat: " + activeUsers.size();
	
	private static class Worker implements Runnable {
		private Socket socket;
		private Scanner serverIn;
		private PrintWriter serverOut;
		
		public Worker(Socket socket) {
			this.socket = socket;
			try {
				this.serverIn = new Scanner(this.socket.getInputStream());
				this.serverOut = new PrintWriter(this.socket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void assignRandomCoordinator() {
			Set<Socket> set = activeUsers.keySet();
			set.remove(coordinatorSocket);
			
			if (set.size() == 0) {
				coordinatorSocket = null;
			}
			else {
				int randomIndex = ThreadLocalRandom.current().nextInt(0, set.size() + 1);
				int i = 0;
				for (Socket user : set) {
					if (i == randomIndex) {
						coordinatorSocket = user;
						return;
					}
					i++;
				}
			}
		}
		
		private void validateUsers() {
			boolean coordinatorChanged = false;
			
			for (Socket user : activeUsers.keySet()) {
				if (user.isClosed()) {
					if (user.equals(coordinatorSocket)) {
						coordinatorChanged = true;
						assignRandomCoordinator();						
					}
					activeUsers.remove(user);
				}
			}
			
			if (coordinatorChanged && coordinatorSocket != null)
			{
				broadcast("Coordinator changed. The new coordinator is " + activeUsers.get(coordinatorSocket));
			}
		}
		
		private void broadcast(String message) {
			for (Socket user : activeUsers.keySet()) {
				try {
					PrintWriter out = new PrintWriter(user.getOutputStream(), true);
					out.println(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void run() {
			System.out.println("Something came through...");
			
			// validate that all users are still connected
			validateUsers();
			
			if (!serverIn.hasNextLine())
			{
				return;
			}
			
			// check if in activeUsers
			// if not, try add with name from stream
			// if name taken, respond with NAME_TAKEN
			// if successful, respond with NAME_ACCEPTED, add socket and name to activeUsers
			if (!activeUsers.containsKey(socket)) {
				String name = serverIn.nextLine();
				if (activeUsers.containsValue(name))
				{
					serverOut.println("NAME_TAKEN");
					return;
				}
				serverOut.println("NAME_ACCEPTED");
				
				broadcast(activeUsers.get(socket) + " has joined the chat!");
				activeUsers.put(socket, name);
				
				if (coordinatorSocket == null) {
					coordinatorSocket = socket;
				}
				
				serverOut.println("Welcome to the chat!");
				serverOut.println(userCountMessage);
				serverOut.println(coordinatorMessage);
				return;
			}
			
			// handle request from the user
			if (serverIn.hasNextLine()) {
				String message = serverIn.nextLine();
				
				if (message.equals("!coordinator")) {
					serverOut.println(coordinatorMessage);
				}
				else if (message.equals("!online")) {
					serverOut.println(userCountMessage);
				}
				else if (message.equals("!quit")) {
					serverOut.println("QUIT_SUCCESS");
					
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					validateUsers();
				}
				else {
					broadcast(message);
				}
			}			
		}
	}
}

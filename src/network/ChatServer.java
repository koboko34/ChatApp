package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class ChatServer {
	
	static HashMap<Socket, String> activeUsers = new HashMap<>();
	static Socket coordinatorSocket = null;
	
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		
		// ask for port number for the server to listen on
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
		
		// create thread pool which Worker instances will use to handle communication between server and users
		ExecutorService pool = Executors.newFixedThreadPool(500);
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(port);
			System.out.println("Server is running...");
			while (true) {
				pool.execute(new Worker(listener.accept()));
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
		
		// closing system input stream
		in.close();
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
	
	
	

	
	
	
	// Worker class responsible for handling events between the server and one user.
	// Each worker instance is on its own thread and handles only one user for the thread's lifetime.
	private static class Worker implements Runnable {
		private Socket socket;
		private Scanner serverIn;
		private PrintWriter serverOut;
		
		private MessageMode messageMode = MessageMode.BROADCAST;
		private Socket privateRecipient = null;
		
		// prepares streams for communication between server and the user this worker is responsible for
		public Worker(Socket socket) {
			this.socket = socket;
			try {
				this.serverIn = new Scanner(this.socket.getInputStream());
				this.serverOut = new PrintWriter(this.socket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// assigns a new random coordinator from the connected users
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
		
		// validates that all users in activeUsers are still connected
		// in the event that the coordinator is found to be disconnected,
		// a new coordinator is selected at random
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
		
		// broadcast a message to all active users except self
		private void broadcast(String message) {
			PrintWriter out = null;
			for (Socket user : activeUsers.keySet()) {
				if (user == socket) {
					continue;
				}
				try {
					out = new PrintWriter(user.getOutputStream(), true);
					out.println(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// sends a private message to current privateRecipient
		// throws exception if privateRecipient is null
		private void privateMessage(String message) {
			try {
				if (privateRecipient == null) {
					throw new Exception("privateRecipient is null!");
				}
				PrintWriter out = new PrintWriter(privateRecipient.getOutputStream(), true);
				out.println(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void printCommands() {
			serverOut.println("\n");
			serverOut.println("======= CUSTOM COMMANDS =======");
			serverOut.println("!commands / !help -> View this message again");
			serverOut.println("!coordinator -> Views the current coordinator");
			serverOut.println("!online -> Views details of current users");
			serverOut.println("!quit -> Quits the chat");
			serverOut.println("!broadcast -> Switch to broadcast mode");
			serverOut.println("!private [USERNAME] -> Switch to private message mode to specified user");
			serverOut.println("\n");
		}
		
		// prints a formatted string containing the name of the current session coordinator
		private void printCoordinatorMessage() {
			serverOut.println("The current coordinator is: " + activeUsers.get(coordinatorSocket));
		}
		
		// prints a formatted string containing the number of active users
		private void printUserCountMessage() {
			serverOut.println("Number of users in chat: " + activeUsers.size());
		}
		
		// prints a formatted string containing the number of active users
		private void printMessageMode() {
			if (messageMode == MessageMode.BROADCAST) {
				serverOut.println("Switched to broadcast mode!");
			}
			else {
				serverOut.println("=== Switched to private message mode! ===");
				serverOut.println("=== Current private recipient: " + activeUsers.get(privateRecipient) + " ===");
			}
		}
		
		// prints a formatted string containing the details of all active users
		private void printUserDetailsMessage() {
			String s = "";
			//============================FINISH THIS LATER=============================
		}
		
		// returns a formatted timestamp string used at the start of user messages
		private static String getCurrentTimestamp() {
			LocalTime currentTime = LocalTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
			return "[" + currentTime.format(formatter) + "] ";
		}
		
		@Override
		public void run() {			
			// validate that all users are still connected
			validateUsers();
			
			
			// check if socket is already in activeUsers
			// if not, try add with name from stream
			// if name taken, respond with NAME_TAKEN
			while (!activeUsers.containsKey(socket)) {
				while (!serverIn.hasNextLine()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// check if the name input by the user is unique
				// returns NAME_TAKEN if name is already used
				String name = serverIn.nextLine();
				if (activeUsers.containsValue(name))
				{
					serverOut.println("NAME_TAKEN");
					continue;
				}

				// if successful, respond with NAME_ACCEPTED, add socket and name to activeUsers map
				serverOut.println("NAME_ACCEPTED");
				broadcast(name + " has joined the chat!");
				activeUsers.put(socket, name);
				
				if (coordinatorSocket == null) {
					coordinatorSocket = socket;
				}
				
				// messages to the user upon joining the chat
				// also informs users of custom commands
				printCommands();
				printMessageMode();				
				serverOut.println("\nWelcome to the chat!");
				printUserCountMessage();
				printCoordinatorMessage();
			}
			
			// handle requests from the user
			while (socket.isConnected()) {				
				if (serverIn.hasNextLine()) {
					String message = serverIn.nextLine();
					
					if (message.charAt(0) == '!') {
						if (message.equals("!coordinator")) {
							printCoordinatorMessage();
						}
						else if (message.equals("!online")) {
							printUserDetailsMessage();
						}
						else if (message.equals("!commands") || message.equals("!help")) {
							printCommands();
						}
						else if (message.equals("!broadcast")) {
							messageMode = MessageMode.BROADCAST;
							printMessageMode();
						}
						else if (message.length() > 9 && message.substring(0, 8).equals("!private ")) {
							// get substring from 9th char to remove the command part of message
							String name = message.substring(9);
							
							// find name in map and set to privateRecipient
							Socket targetSocket = null;
							for (Map.Entry<Socket, String> entry : activeUsers.entrySet()) {
								if (entry.getValue() == name) {
									targetSocket = entry.getKey();
									break;
								}
							}
							
							// if name not in map, do not change messageMode
							if (targetSocket == null) {
								serverOut.println("Username not found in active users!");
								continue;
							}
							
							privateRecipient = targetSocket;
							messageMode = MessageMode.PRIVATE;
							printMessageMode();
						}
						else if (message.equals("!quit")) {
							serverOut.println("QUIT_SUCCESS");
							
							// close socket, input and output streams
							try {
								socket.close();
								serverIn.close();
								serverOut.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							validateUsers();
						}
						else {
							serverOut.println("Invalid command!");
						}
					}
					else {
						switch (messageMode) {
						case BROADCAST:
							message = getCurrentTimestamp() + activeUsers.get(socket) + ": " + message;
							broadcast(message);
							break;
						case PRIVATE:
							message = getCurrentTimestamp() + activeUsers.get(socket) + " (PRIVATE): " + message;
							privateMessage(message);
							break;
						default:
							break;
						}
					}
				}			
			}
		}
	}
}

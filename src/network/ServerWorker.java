package network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

// Worker class responsible for handling events between the server and one user.
// Each worker instance is on its own thread and handles only one user for the thread's lifetime.
class ServerWorker implements Runnable {
	public Socket socket;
	public Scanner serverIn;
	public PrintWriter serverOut;
	public String userName;
	
	public MessageMode messageMode;
	public Socket privateRecipient;
	
	// used as the main interface for reading messages
	// unlike a normal Scanner, messages on this queue can be read without being removed from the queue
	public Queue<String> messageQueue;
	public static HashMap<String, Boolean> pingResponders;
	
	// prepares streams for communication between server and the user this worker is responsible for
	public ServerWorker(Socket socket) {
		this.socket = socket;
		try {
			this.serverIn = new Scanner(this.socket.getInputStream());
			this.serverOut = new PrintWriter(this.socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		messageMode = MessageMode.BROADCAST;
		messageQueue = new LinkedList<>();
		pingResponders = new HashMap<>();
	}
	
	// returns whether messageQueue has a message to be read
	private boolean hasNextLine() {
		return messageQueue.size() > 0;
	}
	
	// returns next line without removing it from the queue
	private String peekNextLine() {
		return messageQueue.peek();
	}
	
	// returns next line and removes it from the queue
	public String nextLine() {
		return messageQueue.poll();
	}
	
	// closes a socket and removes user from activeUsers
	private void closeSocket(Socket socket) {
		String name = ServerContext.activeUsers.remove(socket);

		if (name != null) {
			broadcast(name + " has left the chat!");
		}
		
		if (socket.equals(ServerContext.coordinatorSocket)) {
			assignRandomCoordinator();
			if (ServerContext.coordinatorSocket != null) {
				broadcast("Coordinator changed. The new coordinator is " +
						ServerContext.activeUsers.get(ServerContext.coordinatorSocket));					
			}
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		validateUsers();
	}
	
	// assigns a new random coordinator from the connected users
	private void assignRandomCoordinator() {
		Set<Socket> set = ServerContext.activeUsers.keySet();
		set.remove(ServerContext.coordinatorSocket);
		
		if (set.size() == 0) {
			ServerContext.coordinatorSocket = null;
		}
		else {
			int randomIndex = ThreadLocalRandom.current().nextInt(0, set.size());
			int i = 0;
			for (Socket user : set) {
				if (i == randomIndex) {
					setCoordinator(user);
					return;
				}
				i++;
			}
		}
	}
	
	// checks who is still connected by sending a ping and listening for a returning ping
	public void handlePings() {
		PrintWriter out = null;
		pingResponders.clear();
		
		while (!hasNextLine()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (messageQueue.contains("PING_END")) {
				break;
			}
			
			
		}
		
		while (hasNextLine()) {
			String userName = nextLine();
			if (userName.equals("PING_END")) {
				validateUsers();
				return;
			}
			
			Socket user = null;
			for (Map.Entry<Socket, String> entry : ServerContext.activeUsers.entrySet()) {
				if (entry.getValue().equals(userName)) {
					user = entry.getKey();
					break;
				}
			}
			
			if (user == null) {
				continue;
			}
			
			pingResponders.put(userName, false);
			
			try {
				out = new PrintWriter(user.getOutputStream(), true);
				out.println("PING");
			} catch (IOException e) {
				e.printStackTrace();
				closeSocket(user);
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (pingResponders.get(userName) == true) {
				continue;
			}
			
			closeSocket(user);
		}
	}
	
	// validates that all users in activeUsers are still connected
	// in the event that the coordinator is found to be disconnected,
	// a new coordinator is selected at random
	public synchronized void validateUsers() {
		boolean coordinatorChanged = false;
		ArrayList<Socket> usersToRemove = new ArrayList<>();
		
		for (Socket user : ServerContext.activeUsers.keySet()) {
			if (user.isClosed()) {
				usersToRemove.add(user);
			}
		}
		
		for (Socket user : usersToRemove) {
			String name = ServerContext.activeUsers.get(user);
			if (user.equals(ServerContext.coordinatorSocket)) {
				coordinatorChanged = true;
				ServerContext.activeUsers.remove(user);
				assignRandomCoordinator();						
			}
			else {
				ServerContext.activeUsers.remove(user);					
			}
			broadcast(name + "has left the chat!");
		}
		
		pushUsersToCoordinator();
		if (coordinatorChanged && ServerContext.coordinatorSocket != null)
		{
			broadcast("Coordinator changed. The new coordinator is " + 
					ServerContext.activeUsers.get(ServerContext.coordinatorSocket));
		}
	}
	
	private void pushUsersToCoordinator() {
		if (ServerContext.coordinatorSocket == null) {
			return;
		}
		
		PrintWriter out = null;
		try {
			out = new PrintWriter(ServerContext.coordinatorSocket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
			closeSocket(ServerContext.coordinatorSocket);
		}
		
		out.println("NAMES_BEGIN");
		for (String name : ServerContext.activeUsers.values()) {
			out.println(name);
		}
		out.println("NAMES_END");
	}
	
	// broadcast a message to all active users except self
	public void broadcast(String message) {
		PrintWriter out = null;
		for (Socket user : ServerContext.activeUsers.keySet()) {
			if (socket.isClosed()) {
				continue;
			}
			try {
				out = new PrintWriter(user.getOutputStream(), true);
				out.println(message);
			} catch (IOException e) {
				e.printStackTrace();
				closeSocket(user);
			}
		}
	}
	
	// sends a private message to current privateRecipient
	// throws exception if privateRecipient is null
	public void privateMessage(String message) {
		try {
			if (privateRecipient == null) {
				throw new Exception("privateRecipient is null!");
			}
			PrintWriter out = new PrintWriter(privateRecipient.getOutputStream(), true);
			out.println(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
		serverOut.println(message);
	}
	
	// prints a message to the user and displays all the commands available for them to use in the chat
	public void printCommands() {
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
	public void printCoordinatorMessage() {
		serverOut.println("The current coordinator is: " +
				ServerContext.activeUsers.get(ServerContext.coordinatorSocket) +
				". IP: " + ServerContext.coordinatorSocket.getInetAddress().getHostAddress() +
				"  PORT: " + ServerContext.coordinatorSocket.getLocalPort());
	}
	
	// prints a formatted string containing the number of active users
	public void printUserCountMessage() {
		serverOut.println("Number of users in chat: " + ServerContext.activeUsers.size());
	}
	
	// prints a formatted string containing the number of active users
	public void printMessageMode() {
		if (messageMode == MessageMode.BROADCAST) {
			serverOut.println("=== Switched to broadcast mode! ===");
		}
		else {
			serverOut.println("=== Switched to private message mode! ===");
			serverOut.println("=== Current private recipient: " +
					ServerContext.activeUsers.get(privateRecipient) + " ===");
		}
	}
	
	// prints a formatted string containing the details of all active users
	// print format:
	//   IP               |  Port   |  Name
	//   127.127.127.127  |  65656  |  James
	//   87.126.10.2      |  6563   |  Rachel
	//   17.17.1.107      |  5820   |  Steve
	// etc...
	public void printUserDetailsMessage() {
		String s = "  IP               |  Port   |  Name";
		serverOut.println(s);
		
		for (Map.Entry<Socket, String> entry : ServerContext.activeUsers.entrySet()) {
			String ip = entry.getKey().getLocalAddress().getHostAddress();
			String port = String.valueOf(entry.getKey().getLocalPort());
			String name = entry.getValue();
			
			if (entry.getKey().equals(ServerContext.coordinatorSocket)) {
				name += " (coordinator)";
			}
			
			s = "  " + ip;
			for (int i = ip.length(); i < 15; i++) {
				s += " ";
			}
			s += "  |  " + port;
			for (int i = port.length(); i < 5; i++) {
				s += " ";
			}
			s += "  |  " + name;
			
			serverOut.println(s);
		}
	}
	
	// returns a formatted timestamp string used at the start of user messages
	public static String getCurrentTimestamp() {
		LocalTime currentTime = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		return "[" + currentTime.format(formatter) + "] ";
	}
	
	private void setCoordinator(Socket newCoordinator) {
		ServerContext.coordinatorSocket = newCoordinator;
		pushUsersToCoordinator();

		try {
			PrintWriter out = new PrintWriter(newCoordinator.getOutputStream(), true);
			out.println("NEW_COORDINATOR");
		} catch (IOException e) {
			e.printStackTrace();
			closeSocket(newCoordinator);
		}
	}
	
	@Override
	public void run() {			
		// validate that all users in activeUsers are still connected
		validateUsers();
		
		// creates a thread for StreamReader and gives a unique name with id matching the
		// id of current server worker thread
		Thread streamReader = new Thread(new StreamReader(this), "streamReader-" +
				Thread.currentThread().getName().charAt(Thread.currentThread().getName().length() - 1));
		streamReader.start();
		
		// check if socket is already in activeUsers
		// if not, try add with name from stream
		// if name taken, respond with NAME_TAKEN
		while (!ServerContext.activeUsers.containsKey(socket)) {
			// starts reading input stream and pushing those messages to messageQueue				
			while (!hasNextLine()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			// check if the name input by the user is unique
			// returns NAME_TAKEN if name is already used
			String name = nextLine();
			if (ServerContext.activeUsers.containsValue(name))
			{
				serverOut.println("NAME_TAKEN");
				continue;
			}

			// if successful, respond with NAME_ACCEPTED, add socket and name to activeUsers map
			broadcast(name + " has joined the chat!");
			ServerContext.activeUsers.put(socket, name);
			serverOut.println("NAME_ACCEPTED");
			userName = name;
		}
		
		// wait for client to respond to confirm they are ready for input
		while (!hasNextLine()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// listen for READY message
		try {
			if (!nextLine().equals("READY")) {
				throw new Exception(Thread.currentThread().getName() + " failed to find READY message from client!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}
		
		// sets this client as new coordinator if one doesn't yet exist
		// and pushes list of active users to coordinator
		if (ServerContext.coordinatorSocket == null) {
			setCoordinator(socket);
		}
		else {
			pushUsersToCoordinator();
		}
		
		// messages to the user upon joining the chat
		// also informs users of custom commands
		printCommands();
		printMessageMode();				
		serverOut.println("\nWelcome to the chat!");
		printUserCountMessage();
		printCoordinatorMessage();
		
		RequestHandler requestHandler = new RequestHandler(this);
		
		// handle requests from the user
		while (!socket.isClosed()) {				
			if (hasNextLine()) {
				requestHandler.handleRequest();
			}
		}
		
		validateUsers();
		
		try {
			streamReader.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return;
	}
}

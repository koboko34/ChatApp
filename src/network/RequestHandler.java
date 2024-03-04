package network;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

class RequestHandler {
	ServerWorker serverWorker;
	
	public RequestHandler(ServerWorker serverWorker) {
		this.serverWorker = serverWorker;
	}
	
	// checks if there is a request in messageQueue and if so, handles the request accordingly
	public void handleRequest() {
		if (serverWorker.messageQueue.isEmpty()) {
			return;
		}
		
		String message = serverWorker.nextLine();
		
		if (message.charAt(0) == '!') {
			handleCommand(message);
		}
		else if (message.equals("PING_START")) {
			serverWorker.handlePings();
		}
		else {
			handleMessage(message);
		}
	}
	
	public void handleCommand(String message) {
		if (message.equals("!coordinator")) {
			serverWorker.printCoordinatorMessage();
		}
		else if (message.equals("!online")) {
			serverWorker.printUserDetailsMessage();
		}
		else if (message.equals("!commands") || message.equals("!help")) {
			serverWorker.printCommands();
		}
		else if (message.equals("!broadcast")) {
			serverWorker.messageMode = MessageMode.BROADCAST;
			serverWorker.printMessageMode();
		}
		else if (message.length() > 9 && message.substring(0, 9).equals("!private ")) {
			// get substring from 9th char to remove the command part of message
			String name = message.substring(9);
			
			// find name in map and set to privateRecipient
			Socket targetSocket = null;
			for (Map.Entry<Socket, String> entry : ServerContext.activeUsers.entrySet()) {
				if (entry.getValue().equals(name)) {
					targetSocket = entry.getKey();
					break;
				}
			}
			
			// if name not in map, do not change messageMode
			if (targetSocket == null) {
				serverWorker.serverOut.println("Username not found in active users!");
				return;
			}
			
			serverWorker.privateRecipient = targetSocket;
			serverWorker.messageMode = MessageMode.PRIVATE;
			serverWorker.printMessageMode();
		}
		else if (message.equals("!quit")) {
			serverWorker.serverOut.println("QUIT_SUCCESS");
			
			// close socket, input and output streams
			try {
				serverWorker.socket.close();
				serverWorker.serverIn.close();
				serverWorker.serverOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			serverWorker.validateUsers();
			return;
		}
		else {
			serverWorker.serverOut.println("Invalid command!");
		}
	}
	
	public void handleMessage(String message) {
		switch (serverWorker.messageMode) {
		case BROADCAST:
			message = ServerWorker.getCurrentTimestamp() + ServerContext.activeUsers.get(serverWorker.socket) + ": " + message;
			serverWorker.broadcast(message);
			break;
		case PRIVATE:
			message = ServerWorker.getCurrentTimestamp() + ServerContext.activeUsers.get(serverWorker.socket) + " (PRIVATE): " + message;
			serverWorker.privateMessage(message);
			break;
		default:
			break;
		}
	}
}

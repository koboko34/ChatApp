package network;

import java.io.IOException;
import java.util.HashSet;

// This class is responsible for handling incoming messages from the server.
// Instances of this class will run on its own thread.
// This instance will also respond to any pings sent by the server.
public class Printer implements Runnable {
	
	Thread pingerThread;
	
	// stores active users, used by coordinator
	public void storeUserNames() {
		ChatClient.userNames = new HashSet<>();
		while (ChatClient.serverIn.hasNextLine()) {
			String s = ChatClient.serverIn.nextLine();
			if (s.equals("NAMES_END")) {
				return;
			}
			ChatClient.userNames.add(s);
		}
	}
	
	@Override
	public void run() {
		ChatClient.serverOut.println("READY");
		
		while (!ChatClient.socket.isClosed()) {
			while (ChatClient.serverIn.hasNextLine()) {
				String message = ChatClient.serverIn.nextLine();
				if (message.equals("QUIT_SUCCESS")) {
					try {
						ChatClient.socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
				else if (message.equals("NAMES_BEGIN")) {
					storeUserNames();
				}
				else if (message.equals("PING")) {
					ChatClient.serverOut.println("PING");
				}
				else if (message.equals("NEW_COORDINATOR")) {
					pingerThread = new Thread(new Pinger(), "pingerThread");
					pingerThread.start();
				}
				else {
					System.out.println(message);						
				}
			}
			
			// put thread to sleep to save processing power
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (pingerThread.isAlive()) {
			pingerThread.interrupt();			
		}
	}
}

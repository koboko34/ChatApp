package network;

// This class is instantiated only on the coordinator
// Responsible for maintaining state of active members by sending pings to all clients
// Clients who fail to respond are considered as disconnected and are removed from active users list
class Pinger implements Runnable {
	
	public Pinger() {

	}
	
	@Override
	public void run() {			
		while (!ChatClient.socket.isClosed()) {				
			// send pings to all clients to check if still connected
			ChatClient.serverOut.println("PING_START");
			for (String userName : ChatClient.userNames) {
				ChatClient.serverOut.println(userName);
			}
			ChatClient.serverOut.println("PING_END");
			
			// repeat after 60 seconds
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

package network;

//this class runs on its own thread and constantly reads the input stream
// any messages are passed through to messageQueue
public class StreamReader implements Runnable {
	ServerWorker serverWorker;
	
	public StreamReader(ServerWorker serverWorker) {
		this.serverWorker = serverWorker;
	}
	
	// reads messages from input stream and pushes it to messageQueue
	private void readStream() {
		while (!serverWorker.socket.isClosed() && serverWorker.serverIn.hasNextLine()) {
			String s = serverWorker.serverIn.nextLine();
			if (s.equals("PING")) {
				ServerWorker.pingResponders.put(serverWorker.userName, true);
			}
			else {						
				serverWorker.messageQueue.add(s); 
			}
		}
	}
	
	@Override
	public void run() {
		readStream();
	}
}

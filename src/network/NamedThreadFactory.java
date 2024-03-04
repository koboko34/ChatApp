package network;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

// thread factory which allows for custom naming of threads for readability
class NamedThreadFactory implements ThreadFactory {
	private String threadName;
	private static int i = 0;
	
	public NamedThreadFactory(String name) {
		threadName = name;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, threadName + "-" + i);
		i++;
		return t;
	}
}
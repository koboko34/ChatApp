package network.test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Scanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import network.ChatClient;
import network.Printer;

// This test ensures that the Printer class is correctly storing the active users when
// communicating with the server.
class PrinterTest {

	static InputStream inputStream;
	static HashSet<String> namesSent;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		namesSent = new HashSet<>();
		namesSent.add("Steve");
		namesSent.add("Bob");
		namesSent.add("James");
		
		String s = "";
		for (String name : namesSent) {

			s += name + "\n";
		}
		s += "NAMES_END\n";
		
		inputStream = new ByteArrayInputStream(s.getBytes());
	}
	
	@BeforeEach
	void setUpBeforeTest() throws Exception {
		ChatClient.serverIn = new Scanner(inputStream);
	}

	@AfterEach
	void tearDown() throws Exception {
		ChatClient.userNames.clear();
	}

	// Normal test, sending 3 names and expecting the same 3 names to be stored.
	// Expected result: correct
	@Test
	void testStoreUserNames1() {		
		Printer printer = new Printer();
		printer.storeUserNames();
		
		boolean correct = false;
		if (ChatClient.userNames.containsAll(namesSent) &&
			namesSent.containsAll(ChatClient.userNames)) {
			correct = true;
		}
		
		assert(correct);
	}
	
	// Sending 3 names and expecting the same 3 names to be stored
	// This test simulates none of the names being stored
	// Expected result: incorrect
	@Test
	void testStoreUserNames2() {		
		Printer printer = new Printer();
		printer.storeUserNames();
		ChatClient.userNames.clear();
		
		boolean correct = false;
		if (ChatClient.userNames.containsAll(namesSent) &&
			namesSent.containsAll(ChatClient.userNames)) {
			correct = true;
		}
		
		assert(!correct);
	}
	
	// Sending 3 names and expecting the same 3 names to be stored
	// This test simulates one of the names being stored multiple times
	// Expected result: incorrect
	@Test
	void testStoreUserNames3() {		
		Printer printer = new Printer();
		printer.storeUserNames();
		
		String nameToCopy = "";
		int userCount = ChatClient.userNames.size();
		for (String name : ChatClient.userNames) {
			nameToCopy = name;
		}
		ChatClient.userNames.clear();
		
		for (int i = 0; i < userCount; i++) {
			ChatClient.userNames.add(nameToCopy);
		}
		
		boolean correct = false;
		if (ChatClient.userNames.containsAll(namesSent) &&
			namesSent.containsAll(ChatClient.userNames)) {
			correct = true;
		}
		
		assert(!correct);
	}
}

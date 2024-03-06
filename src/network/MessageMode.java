package network;

// This enum class is used to keep track of which messaging mode the client is currently in.
// BROADCAST will send broadcast messages to all connected users.
// PRIVATE will send a private message to the user which is currently set to privateRecipient
// in the server worker class.
public enum MessageMode {
	BROADCAST,
	PRIVATE
}

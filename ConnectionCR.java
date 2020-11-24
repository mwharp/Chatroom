/**
 * This acts as a separate thread
 * for each client connection to the ChatServer.
 *
 * @author Max Harper Destin West
 * 11/21/19
 * 
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.time.Instant;
import java.util.Vector;
import java.util.HashMap;

public class ConnectionCR implements Runnable
{	
	public static final int BUFFER_SIZE = 1056;
	private Socket client;
	
	HashMap<String, DataOutputStream> connections = new HashMap<String, DataOutputStream>(); //hashmap w/ key:usernames, value:streams
	Vector<String> messages = new Vector<String>(); //a vector consisting of client messages
    
    public BufferedReader fromClient = null; //read headers and messages from chat client
    public DataOutputStream toClient = null; //write headers back to chat client

	String line = new String();
	String message = new String();
	String privateMessage = new String();
	String fromUserName = new String();
	String toUserName = new String();
	String date = new String();

	// constructor
	public ConnectionCR(Socket client, Vector<String> messages, HashMap<String, DataOutputStream> connections) {
		this.client = client;
		this.messages = messages;
		this.connections = connections;
	}

	public void run() { 
		try {
			// set up the necessary communication channels
			fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			toClient = new DataOutputStream(client.getOutputStream());

			// constantly check for messages from the client
			// handle each message as dictated by the YeetMail protocol
			while((line = fromClient.readLine()) != null) {

				// join request
				if(line.equals("status: 200")){
					date = fromClient.readLine();
					fromUserName = fromClient.readLine();

					// if the username is taken, inform client and close connection
					if(connections.containsKey(fromUserName)){
						toClient.writeBytes("status: 401" + "\r\n" + date + "\r\n\r\n");
						toClient.flush();
						client.close();
					
					// username is unique, inform client, add user and dataoutputstream to connections hashmap
					// inform the rest of the clients
					} else {
						toClient.writeBytes("status: 201"  + "\r\n" + date + "\r\n\r\n");
						toClient.flush();

						//inform the user of who is in the chat
						toClient.writeBytes("Current yeeters are: \r\n");
						for(String name: connections.keySet()){
							toClient.writeBytes("status: 301" + "\r\n" + date + "\r\n" + name + "\r\n" + "is currently yeeting" + "\r\n\r\n");
						}

						connections.put(fromUserName, toClient);
						messages.add("status: 301" + "\r\n" + date + "\r\n" + fromUserName + "\r\n" + "has joined the chat" + "\r\n\r\n");
					}
				}
				// public message
				else if(line.equals("status: 202")){
					date = fromClient.readLine();
					fromUserName = fromClient.readLine();
					message = fromClient.readLine() + "\r\n";
					messages.add(line + "\r\n" + date + "\r\n" + fromUserName + "\r\n" + message + "\r\n\r\n");
				}
				// private message
				else if(line.equals("status: 203")){
					date = fromClient.readLine();
					fromUserName = fromClient.readLine();
					toUserName = fromClient.readLine();
					message = fromClient.readLine();

					// if the recipient exists, send them the private message
					if(connections.containsKey(toUserName)){
						privateMessage = line + "\r\n" + date + "\r\n" + fromUserName + "\r\n" + toUserName + "\r\n" + message + "\r\n\r\n";
						connections.get(toUserName).writeBytes(privateMessage);
					// else, inform the client the recipient doesn't exist
					} else {
						toClient.writeBytes("status: 404"  + "\r\n" + date + "\r\n" + toUserName + "\r\n\r\n");
						toClient.flush();
					}
				}
			}
		}
		catch (java.io.IOException ioe) { System.err.println(ioe);}
	}
}
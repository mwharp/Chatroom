/**
 * A server that implements the YeetMail protocol in order to facilitate a chat room
 *
 * @author Max Harper Destin West
 * 11/19/19
 */

import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.Instant;

public class  Server
{
	public static final int PORT = 7331;

    // construct a thread pool for concurrency	
	private static final Executor exec = Executors.newCachedThreadPool();
	
	public static void main(String[] args) throws IOException {
        ServerSocket server = null; //server socket
		HashMap<String, DataOutputStream> connections = new HashMap<String, DataOutputStream>(); //hashmap w/ key:usernames, value:streams
		Vector<String> messages = new Vector<String>(); //a vector consisting of client messages
		
	
		try {
			
			// establish the socket
			server = new ServerSocket(PORT);
			
			while(true) {
				// now listen for connections and service the connection in a separate thread.
				Runnable task = new ConnectionCR(server.accept(), messages, connections);
				exec.execute(task);

				// initialize a broadcast thread for delivering messages to all clients
				Thread BroadcastThread = new Thread(new BroadcastThread(connections, messages));
				BroadcastThread.start();
			}
		}
		catch (IOException ioe) { System.err.println(ioe); }

		// close the socket
		finally {
			if (server != null)
				server.close();
		}
	}
}

// a class that is used to broadcast each clients messages to all other clients
class BroadcastThread implements Runnable
{
	String message = new String();
	String exitName = new String();
	String userName = new String();
	Instant time = Instant.now();

	HashMap<String, DataOutputStream> connections = new HashMap<String, DataOutputStream>(); //hashmap w/ key:usernames, value:streams
	Vector<String> messages = new Vector<String>(); //a vector consisting of client messages

	//constructor
	public BroadcastThread(HashMap<String, DataOutputStream> connections , Vector<String> messages) {
		this.connections = connections;
		this.messages = messages;
	}

    public void run() {

        while (true) {
            // sleep for 1/10th of a second, no need to hurry with human clients
			try { Thread.sleep(100); } catch (InterruptedException ignore) { }
				
				//if there is a message, pop it off the vector
				while(!messages.isEmpty()){
					message = messages.remove(0);

					//write the message to every user, and flush
					try {
						for(String name: connections.keySet()){
							userName = name;
							connections.get(name).writeBytes(message);
							connections.get(name).flush();						
						}
					}
					// catch if an output stream is unavaiable to write to
					// inform the clients, and remove the user name from the connections hash map
					catch (IOException ioe) {
						System.err.println(ioe);
						messages.add("status: 301" + "\r\n" + "date: " + time + "\r\n" + userName + "\r\n" + "is all done yeeting." + "\r\n\r\n");
						connections.remove(userName);
					}
				}									
        }
    }
} 
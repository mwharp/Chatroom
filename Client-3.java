/**
 * A Chat Client that follows the YeetMail protocol.
 * 
 * @author Max Harper Destin West
 * 11/19/19
 */

import java.net.*;
import java.io.*;
import java.time.Instant;

public class Client{

    public static final int PORT = 7331;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
			System.err.println("Usage: java ChatClient <server> <username>");
            System.exit(0);
        }

        Socket socket = null;

        BufferedReader fromServer = null;	// the reader from the network
		DataOutputStream toServer = null;		// the writer to the network
		BufferedReader localBin = null;		// the reader from the keyboard 

        String join = new String();
		String username = new String();
		String toUserName = new String();
		String message = new String();
		String privateMessage = new String();

        
        try {
			// create a socket, IP:PORT
            socket = new Socket(args[0], PORT);
            // set up the necessary communication channels
			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			localBin = new BufferedReader(new InputStreamReader(System.in));
			toServer = new DataOutputStream(socket.getOutputStream());
            
            username = args[1];
            Instant time = Instant.now();

			// construct the join header
			// if username is not already in use on the server side the server will complete the clients connection
			join = "status: 200" + "\r\n" + "date: " + time + "\r\n" + username + "\r\n\r\n";

            // write the header to the users chat server and flush the bytes
			toServer.writeBytes(join);
			toServer.flush();

			// create a new thread to read the chat messages being sent from the server
			Thread ReaderThread = new Thread(new ReaderThread(socket));
			ReaderThread.start();
			
			// read input from the keyboard
			while((message = localBin.readLine()) != null){
				// private message logic
				if (message.startsWith("/pm")){
					// parse our the recipients name and the message from the users input
					String[] pm = message.split("[\\s+]", 3);

					toUserName = pm[1];
					privateMessage = pm[2];
					
					// construct the private message header, send to the server
					toServer.writeBytes("status: 203" + "\r\n" 
					+ "date: " + time + "\r\n" 
					+ username + "\r\n" 
					+ toUserName + "\r\n"
					+ privateMessage + "\r\n\r\n");
				}
				// construct the public message header, send to the server
				else {
					toServer.writeBytes("status: 202" + "\r\n" 
					+ "date: " + time + "\r\n" 
					+ username + "\r\n" 
					+ message + "\r\n\r\n");
				}
			}
		}
		catch (IOException ioe) {
			System.err.println(ioe);
		}
		// close the sockets
        finally {
			if (fromServer != null)
				fromServer.close();
			if (localBin != null)
				localBin.close();
			if (toServer != null)
				toServer.close();
			if (socket != null)
				socket.close();
		}
    }
}

// a class that creates a seperate thread that reads messages from the server
class ReaderThread implements Runnable{

	Socket server;
	BufferedReader fromServer;

	String header = new String();
	String date = new String();
	String fromUserName = new String();
	String toUserName = new String();
	String message = new String();

	// constructor
	public ReaderThread(Socket server) {
		this.server = server;
	}

	public void run() {
		try {
			// set up the necessary communication channels
			fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));

			// constantly check for messages from the server
			// handle each message as dictated by the YeetMail protocol
			while (true) {
				header = fromServer.readLine();

				// join the chat
				if(header.equals("status: 201")){
					System.out.println("You've successfully joined the chat! Yeet!");
				}
				// unsuccessful join, user name already taken
				else if (header.equals("status: 401")){
					System.out.println("You're user name is taken, try again.");
				}
				// public message
				else if (header.equals("status: 202")){
					date = fromServer.readLine();
					fromUserName = fromServer.readLine();
					message = fromServer.readLine() + "\r\n";
					System.out.println("[" + fromUserName + "] " + message);
				}
				// private message
				else if (header.equals("status: 203")){
					date = fromServer.readLine();
					fromUserName = fromServer.readLine();
					toUserName = fromServer.readLine();
					message = fromServer.readLine() + "\r\n";
					System.out.println("[Private Message from " + fromUserName + "] " + message);
				}
				// message from the server, e.g. a new user has joined the chat
				else if (header.equals("status: 301")){
					date = fromServer.readLine();
					fromUserName = fromServer.readLine();
					message = fromServer.readLine();
					System.out.println("[" + fromUserName + " " + message + "]");
				}
				// private message error, recipient does not exist
				else if (header.equals("status: 404")){
					date = fromServer.readLine();
					toUserName = fromServer.readLine();
					System.out.println("[" + toUserName + " is not in this chat.]");
				}
			}
		}
		catch (IOException ioe) { System.out.println(ioe); 
		}
		// close the socket
		// finally {
		// 	if (fromServer != null)
		// 		fromServer.close();
		// }

	}

}
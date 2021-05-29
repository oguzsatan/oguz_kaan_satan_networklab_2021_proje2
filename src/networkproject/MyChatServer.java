package networkproject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyChatServer {

	// Name of clients
	private static Set<String> names = new HashSet<>();

	// Set of print writers
	private static Set<PrintWriter> writers = new HashSet<>();

	// Constructor
	public MyChatServer() {

	}

	/**
	 * Constructor
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("My chat server is running...");
		ExecutorService pool = Executors.newFixedThreadPool(500); // Get thread
																	// pool
		try (ServerSocket listener = new ServerSocket(59001)) { // Create
																// listener
			while (true) {
				pool.execute(new Handler(listener.accept()));
			}
		}
	}

	/**
	 * The client handler task.
	 */
	private static class Handler implements Runnable {
		private String name;
		private Socket socket;
		private Scanner in;
		private PrintWriter out;

		/**
		 * Constructor
		 * 
		 * @param socket
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * Returns names of the clients
		 * 
		 * @param exceptName
		 * @return String
		 */
		public String getNames(String exceptName) {
			String str = "";

			for (String name : names) {
				if (name != null && !name.equals(exceptName)) {
					str += name + "-";
				}
			}
			return str;
		}

		/**
		 * Thread client service
		 */
		public void run() {
			try {
				in = new Scanner(socket.getInputStream()); // create scanner
															// object
				out = new PrintWriter(socket.getOutputStream(), true); // Create
																		// writer
																		// object

				// Keeps client name
				while (true) {
					out.println("SUBMITNAME");
					name = in.nextLine();
					if (name == null) {
						return;
					}
					synchronized (names) {
						if (!name.isEmpty() && !names.contains(name)) {
							names.add(name);

							String clientNames = "ALLNAMES" + getNames(name);
							out.println(clientNames);
						break;
						}
					}
				}

				// Name accept operations
				out.println("NAMEACCEPTED " + name);
				for (PrintWriter writer : writers) {
					writer.println("MESSAGE " + name + " has joined");
				}
				writers.add(out);

				// Accept messages from client and broadcast them.
				while (true) {
					String input = in.nextLine();
					if (input.toLowerCase().startsWith("/quit")) {
						return;
					} else if (input.startsWith("ALLNAMES")) {
						String clientNames = "ALLNAMES" + getNames("");
						out.println(clientNames);
					}
					for (PrintWriter writer : writers) {
						writer.println("MESSAGE " + name + ": " + input);
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			} finally {
				if (out != null) {
					writers.remove(out);
				}
				if (name != null) {
					System.out.println(name + " is leaving"); // Client is
																// leaving
					names.remove(name);
					for (PrintWriter writer : writers) {
						writer.println("MESSAGE " + name + " has left");
					}
				}
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}

}

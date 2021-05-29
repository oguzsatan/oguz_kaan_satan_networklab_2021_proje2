package networkproject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * My chat client
 * 
 * @author 
 *
 */
public class MyChatClient {

	private String serverAddress;  // Server address
	private Scanner in;  // Scanner
	private PrintWriter out;
	private Socket socket = null;
	private String clientName;  // Client name
	private JFrame frame = new JFrame("My Socket Chat App");
	private JTextField textField = new JTextField(30);  // Message enter field
	private JTextArea messageArea = new JTextArea(30, 60);  // Message display area

	private JButton buttonRefresh;  // chat clients combo refresh

	final JComboBox<String> nameCombo;  // Keeps chat clients

	/**
	 * 
	 * Constructor
	 * 
	 * @param serverAddress
	 */
	public MyChatClient(String serverAddress) {
		this.serverAddress = serverAddress;

		// Layout frame componenets
		textField.setEditable(false);
		messageArea.setEditable(false);

		JPanel panelTop = new JPanel();
		panelTop.setLayout(new BorderLayout());

		JScrollPane jScrollPane = new JScrollPane(messageArea);
		jScrollPane.setPreferredSize(new Dimension(700, 550));
		panelTop.add(jScrollPane, BorderLayout.CENTER);
		panelTop.add(textField, BorderLayout.SOUTH);
		frame.getContentPane().add(panelTop, BorderLayout.CENTER);

		frame.pack();
		frame.setLocationRelativeTo(null);  // Center frame

		// Send on enter then clear to prepare for next message
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				String selectedName = "All";  // All clients 

				if (nameCombo.getSelectedItem() != null) {
					selectedName = (String) nameCombo.getSelectedItem();
				}

				if (selectedName.equals("All")) {  // Send message to the all clients
					out.println(textField.getText());
				} else {  // Send private message
					String msg = "pcx-" + selectedName + "-" + clientName + ": " + textField.getText();
                                        System.out.println(msg);
					out.println(msg);
				}

				textField.setText("");  // Clear textfield after send message
			}
		});

		// Other Client Names
		String[] nameValues = new String[] {};

		nameCombo = new JComboBox<String>(nameValues);

		nameCombo.setEditable(true);
		nameCombo.setMaximumSize(new Dimension(40, 30));

		// create bottom panel
		JPanel bottomPanel = new JPanel();

		// create start private chat button
		buttonRefresh = new JButton("Refresh");

		buttonRefresh.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {  // Refresh button event
				out.println("ALLNAMES");
			}
		});

		bottomPanel.add(nameCombo);  // Put name combo to bottom panel
		bottomPanel.add(buttonRefresh); // Put refresh button to bottom panel

		frame.getContentPane().add(BorderLayout.SOUTH, bottomPanel);
	}

	/**
	 * Returns client name
	 * 
	 * @return
	 */
	private String getName() {
		return JOptionPane.showInputDialog(frame, "Enter a screen name:", "Name selection",
				JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Prepare names for name combobox content
	 * 
	 * @param nameValues
	 * @return String[]
	 */
	private String[] prepareNames(String[] nameValues) {
		ArrayList<String> arrList = new ArrayList<>();
		arrList.add("All");  // Default value

		if (nameValues != null && nameValues.length > 0) {
			for (int i = 0; i < nameValues.length; i++) {
				if (nameValues[i] != null && !nameValues[i].equals("") && !nameValues[i].equalsIgnoreCase(clientName)) {
					arrList.add(nameValues[i]);
				}
			}
		}

		String[] str = arrList.toArray(new String[arrList.size()]);

		return str;
	}

	/**
	 * Listens messages
	 * 
	 * @throws IOException
	 */
	private void run() throws IOException {
		try {
			this.socket = new Socket(serverAddress, 59001);  // Create socket object
			this.in = new Scanner(socket.getInputStream());  // Create scanner object
			this.out = new PrintWriter(socket.getOutputStream(), true);  // Create printwriter object

			while (in.hasNextLine()) {
				String line = in.nextLine();
				if (line.startsWith("SUBMITNAME")) {  // Submits name serverdan gelen mesaj(isim)
					String submitName = getName();
					clientName = submitName;
					out.println(submitName);
				} else if (line.startsWith("NAMEACCEPTED")) {  // Name accepted, set frame title and textfield editable status
					this.frame.setTitle("Chat Client - " + line.substring(13));
					textField.setEditable(true);
				} else if (line.startsWith("MESSAGE")) {  // Parse message text and display
					String msg = line.substring(8);
                                        //pcx-ayşe-sevgi: selam
					if (msg != null && msg.length() > 3 && msg.indexOf(": pcx-") > 0) {
						// destination client
						int idp = msg.indexOf(": pcx-");  // Private message?
						int index = +6;
						String tmpLine = msg.substring(index);
						String[] tokens = tmpLine.split("-");

						if (tokens != null && tokens.length == 2 && tokens[0].equals(clientName)) {
							messageArea.append(tmpLine + "\n");
						}

						// source client

						String clName = msg.substring(0, idp); //clNAme=gönderen kısım  Token1=alıcı

						if (tokens.length > 0 && tokens[1] != null && tokens[1].equals(clientName)) {  // Print message to destination client
							messageArea.append(tokens[2] + "\n");
						} else if (clName.equals(clientName)) {  // Print message to source client 
							messageArea.append(tokens[2] + "\n");
						}

					} else if (msg != null && msg.length() > 3 && msg.indexOf(": ALLNAMES") > 0) {
						// do not print message
					} else {
						messageArea.append(msg + "\n");  // Print normal message(not private!)
					}
				} else if (line.startsWith("ALLNAMES")) {
					String[] nameValues = line.substring(8).split("-");  // Get names grom server and split names

					nameValues = prepareNames(nameValues);  // Prepares name array
					DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(nameValues);
					nameCombo.setModel(model); // sets combo model
				}
			}
		} finally {
			frame.setVisible(false);
			frame.dispose();
		}
	}

	public static void main(String[] args) throws Exception {

		String serverName = "localhost";  // Default server name

		if (args.length == 1)  // Server name from command line parameter. Overrides server name default value
		{
			serverName = args[0];
		}

		// Starts chat client
		MyChatClient client = new MyChatClient(serverName);
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}
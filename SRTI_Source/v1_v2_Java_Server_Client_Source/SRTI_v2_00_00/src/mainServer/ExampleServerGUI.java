package mainServer;

/* ExampleServerGUI.java
 * - class that ties to ExampleServer.java logic to show graphic user interface.
 * - uses "Swing" API in Java, requires JDK 1.2 and later. 
 * 		- WARNING: API in Java can vary with each JDK version, requires testing. Otherwise is "100% portable."
 * - handy tutorial: https://www3.ntu.edu.sg/home/ehchua/programming/java/j4a_gui.html#zz-7.
 * 
 * - known issue: GUI will sometimes not update to correctly represent the SRTI. 
 * 					This does not mean the SRTI itself is not active.
 * 					To resolve GUI, closing the GUI and restarting is necessary.
 * */

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.SwingConstants;
import javax.swing.JEditorPane;

public class ExampleServerGUI extends JFrame implements RTISim{
	
	String tag = "ExampleServerGUI";
	
	// a variable to confirm if the gui has loaded (currently has no practical purpose in logic)
	boolean guiLoaded = false;
	// number of simulations connected to the server (referred to in debugging / gui for user)
	int numOfApps = 0;
	// hostname and portnumber needed for simulations to connect to the server (referred to in debugging / gui for user)
	String hostAddress = "[null]";
	String hostPort = "[null]";
	
	// data structures used to hold messages displayed in gui for user
	public class ConnectedSim{
		public String name = "";
		public ArrayList<String> publishTo = new ArrayList<String>();
		public ArrayList<String> subscribeTo = new ArrayList<String>();
	}
	ArrayList<ConnectedSim> listApps_items = new ArrayList<ConnectedSim>();
	ArrayList<String> listAppsPublishTo_items = new ArrayList<String>();
	ArrayList<String> listAppsSubscribeTo_items = new ArrayList<String>();
	public class MessageHistory{
		public String name = "";
		public ArrayList<String> historyTimestamps = new ArrayList<String>();
		public ArrayList<String> historySource = new ArrayList<String>();
		public ArrayList<String> historyContent = new ArrayList<String>();
	}
	ArrayList<MessageHistory> listMessages_items = new ArrayList<MessageHistory>();			
	
	// GUI objects
	JPanel main_panel;
	JTextArea title_area;
	JTextArea numApps_area;
	JList listApps_list;
	JList listAppsPublishTo_list;
	JList listAppsSubscribeTo_list;
	JList listMessages_list;
	JList listMessagesHistory_list;
	JTextArea listMessagesContent_area;
	
	//RTI Objects (needed to access API to properly connect to RTI Server)
	RTILib rtiLib;
	public int messageLimit = -1;
	
	// constructor (when executing .jar file, ExampleServer.java (the RTI Server) will call this automatically. 
	// Else, this can be accessed by a simulation to manually open a GUI without starting a new server.
	public ExampleServerGUI(String hostName, String portNumber) {
		printLine("Starting GUI (not loaded yet)...");
		
		//save hostname and portnumber
		updateHost(hostName, portNumber);
		
		// load GUI, format with JSwing
		setupPanel();

		// connect to RTI Server using same method as any simulation
		rtiLib = new RTILib(this);
		rtiLib.connect(hostName, portNumber);
		rtiLib.setTcpOn(true);
		rtiLib.subscribeToAllPlusHistory();
	}
	
	
	// thread to update UI when change occurs to text shown (is supposed to be more safe to "setListData" rather than re-drawing entire UI)
	Runnable runnableUpdateUI = new Runnable() {
		public void run() {
			listAppsPublishTo_list.setListData(listAppsPublishTo_items.toArray());
			listAppsSubscribeTo_list.setListData(listAppsSubscribeTo_items.toArray());
			
			String[] listMessagesHistory_string;
			if (listMessage_selectedIndex >= 0 && listMessage_selectedIndex < listMessages_items.size()) {
				listMessagesHistory_string = new String[listMessages_items.get(listMessage_selectedIndex).historyTimestamps.size()];
				for (int i = 0; i < listMessagesHistory_string.length; i++) {
					listMessagesHistory_string[i] = listMessages_items.get(listMessage_selectedIndex).historyTimestamps.get(i) 
							+ " <- " + listMessages_items.get(listMessage_selectedIndex).historySource.get(i);
				}
			} else {
				listMessagesHistory_string = new String[0];
			}
			listMessagesHistory_list.setListData(listMessagesHistory_string);
			
			String[] listApps_items_string = new String[listApps_items.size()];
			for (int i = 0; i < listApps_items_string.length; i++) {
				listApps_items_string[i] = listApps_items.get(i).name;
			}
			listApps_list.setListData(listApps_items_string);
			
			String[] listMessages_items_string = new String[listMessages_items.size()];
			for (int i = 0; i < listMessages_items_string.length; i++) {
				listMessages_items_string[i] = listMessages_items.get(i).name;
			}
			listMessages_list.setListData(listMessages_items_string);
		}
	};
	
	
	// click handlers to allow user to click on a item to see it's relevant data in a different panel.
	int listApps_selectedIndex = -1;
	public class ListAppSelectionHandler implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			listApps_selectedIndex = listApps_list.getSelectedIndex();
			
			if (listApps_selectedIndex < 0 || listApps_selectedIndex > listApps_items.size() - 1) {
				printLine("\t\t\t ERROR ... for some reason, list of connected apps is 0 or less?");
				return;
			}
			
			if (listApps_items.get(listApps_selectedIndex).publishTo.size() > 0) {
				listAppsPublishTo_items = listApps_items.get(listApps_selectedIndex).publishTo;
			} else {
				listAppsPublishTo_items.clear();
			}
			listAppsPublishTo_list.setListData(listAppsPublishTo_items.toArray());
			listAppsPublishTo_list.updateUI();
			
			if (listApps_items.get(listApps_selectedIndex).subscribeTo.size() > 0) {
				listAppsSubscribeTo_items = listApps_items.get(listApps_selectedIndex).subscribeTo;
			} else {
				listAppsSubscribeTo_items.clear();
			}
			listAppsSubscribeTo_list.setListData(listAppsSubscribeTo_items.toArray());
			listAppsSubscribeTo_list.updateUI();
			
			SwingUtilities.invokeLater(runnableUpdateUI);
		}
	}
	
	int listMessage_selectedIndex = -1;
	int listMessageHistory_selectedIndex = -1;
	public class ListMessageSelectionHandler implements ListSelectionListener{
		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			
			int selectedIndex = listMessages_list.getSelectedIndex();
			if (selectedIndex != -1) {
				listMessage_selectedIndex = listMessages_list.getSelectedIndex();
				listMessagesContent_area.setText("");
				listMessageHistory_selectedIndex = -1;
			}
			
			if (listMessage_selectedIndex < 0 || listMessage_selectedIndex >= listMessages_items.size()) {
				return;
			}
				
			String[] listMessagesHistory_string = new String[listMessages_items.get(listMessage_selectedIndex).historyTimestamps.size()];
			for (int i = 0; i < listMessagesHistory_string.length; i++) {
				listMessagesHistory_string[i] = listMessages_items.get(listMessage_selectedIndex).historyTimestamps.get(i) 
						+ " <- " + listMessages_items.get(listMessage_selectedIndex).historySource.get(i);
			}
			listMessagesHistory_list.setListData(listMessagesHistory_string);
			listMessagesHistory_list.updateUI();
			
			SwingUtilities.invokeLater(runnableUpdateUI);
		}
	}
	
	public class ListMessageHistorySelectionHandler implements ListSelectionListener{

		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			
			if (listMessage_selectedIndex < 0 || listMessage_selectedIndex >= listMessages_items.size()) {
				return;
			}
		
			int selectedIndex = listMessagesHistory_list.getSelectedIndex();
			if (selectedIndex != -1) {
				listMessageHistory_selectedIndex = selectedIndex;
			}
			
			if (listMessageHistory_selectedIndex < 0 || 
					listMessageHistory_selectedIndex >= listMessages_items.get(listMessage_selectedIndex).historyContent.size())
			{
				return;
			}
			
			listMessagesContent_area.setText(listMessages_items.get(listMessage_selectedIndex).historyContent.get(listMessageHistory_selectedIndex));
		}
		
	}
	
	// below function used to test GUI with random list of simulations.
	/*public void initializeRandomSimList() {
		Random rand = new Random();
		for (int i = 0; i < 6; i++) {
			listApps_items.add(new ConnectedSim());
			listApps_items.get(i).name = "sim_" + String.format("%04d", i);
			int randInt = rand.nextInt(4) + 1;
			for (int j = 0; j < randInt; j++) {
				listApps_items.get(i).publishTo.add("data_" + String.format("%04d", j));
				listApps_items.get(i).subscribeTo.add("data_" + String.format("%04d", j));
			}
		}
	}
	
	public void initializeMessageTypeList() {
		for (int i = 0; i < listApps_items.size(); i++) {
			for (int j = 0; j < listApps_items.get(i).publishTo.size(); j++) {
				MessageHistory newMessage = new MessageHistory();
				String messageName = listApps_items.get(i).publishTo.get(j);
				boolean alreadyListed = false;
				int k = 0;
				for (k = 0; k < listMessages_items.size(); k++) {
					if (listMessages_items.get(k).name.compareTo(messageName) == 0) {
						alreadyListed = true;
						break;
					}
				}
				if (alreadyListed == false) {
					newMessage.name = messageName;
					newMessage.historyTimestamps.add("20171220");
					newMessage.historyTimestamps.add("20171221");
					newMessage.historyContent.add("something here.");
					newMessage.historyContent.add("something there.");
					listMessages_items.add(newMessage);
				} else {
					
				}
			}
			for (int j = 0; j < listApps_items.get(i).subscribeTo.size(); j++) {
				MessageHistory newMessage = new MessageHistory();
				String messageName = listApps_items.get(i).subscribeTo.get(j);
				boolean alreadyListed = false;
				int k = 0;
				for (k = 0; k < listMessages_items.size(); k++) {
					if (listMessages_items.get(k).name.compareTo(messageName) == 0) {
						alreadyListed = true;
						break;
					}
				}
				if (alreadyListed == false) {
					newMessage.name = messageName;
					newMessage.historyTimestamps.add("20171220");
					newMessage.historyTimestamps.add("20171221");
					newMessage.historyContent.add("something here.");
					newMessage.historyContent.add("something there.");
					listMessages_items.add(newMessage);
				} else {
					
				}
			}
		}
	}
	*/
	
	// functions to update a list, keeping track if user had previously selected a sim (else, it would reset and be difficult to read info on any sim)
	public void updateSimList() {
		
		String[] listApps_items_string = new String[listApps_items.size()];
		for (int i = 0; i < listApps_items_string.length; i++) {
			listApps_items_string[i] = listApps_items.get(i).name;
		}
		listApps_list.setListData(listApps_items_string);
		listApps_list.updateUI();

		if (listApps_selectedIndex >= listApps_items.size() || listApps_selectedIndex < 0) {
			listApps_selectedIndex = -1;
			listAppsPublishTo_items.clear();
			listAppsPublishTo_list.setListData(listAppsPublishTo_items.toArray());
			listAppsSubscribeTo_items.clear();
			listAppsSubscribeTo_list.setListData(listAppsSubscribeTo_items.toArray());
			listAppsPublishTo_list.updateUI();
		} else {
			listAppsPublishTo_items = listApps_items.get(listApps_selectedIndex).publishTo;
			listAppsPublishTo_list.setListData(listAppsPublishTo_items.toArray());
			listAppsSubscribeTo_items = listApps_items.get(listApps_selectedIndex).subscribeTo;
			listAppsSubscribeTo_list.setListData(listAppsSubscribeTo_items.toArray());
			listAppsSubscribeTo_list.updateUI();
		}
		
		SwingUtilities.invokeLater(runnableUpdateUI);
	}
	
	public void updateMessageList() {
		String[] listMessages_items_string = new String[listMessages_items.size()];
		for (int i = 0; i < listMessages_items_string.length; i++) {
			listMessages_items_string[i] = listMessages_items.get(i).name;
		}
		
		try {
			/* Why would this return an error? Java Doc says calling this multiple times leads to "undefined behavior,"
			 * and instead we should be updating the "listModel" attached to the list.
			 * 
			 * https://stackoverflow.com/questions/13597903/how-to-clear-a-jlist-in-java
			 *  
			 */
			listMessages_list.setListData(listMessages_items_string);
			listMessages_list.updateUI();
		} catch (Exception e) {
			printLine("error here... why?!??!?!?" + e.getMessage() + " " + e.toString());
		}
		
		
		if (listMessage_selectedIndex >= 0 
				&& listMessage_selectedIndex <= listMessages_items.size()) {

			String[] listMessagesHistory_string = new String[listMessages_items.get(listMessage_selectedIndex).historyTimestamps.size()];
			for (int i = 0; i < listMessagesHistory_string.length; i++) {
				listMessagesHistory_string[i] = listMessages_items.get(listMessage_selectedIndex).historyTimestamps.get(i) 
						+ " <- " + listMessages_items.get(listMessage_selectedIndex).historySource.get(i);
			}
			listMessagesHistory_list.setListData(listMessagesHistory_string);
			listMessagesHistory_list.updateUI();
			
			if (listMessageHistory_selectedIndex >= 0 
					&& listMessageHistory_selectedIndex <= listMessages_items.get(listMessage_selectedIndex).historyContent.size()) {
				listMessagesContent_area.setText(listMessages_items.get(listMessage_selectedIndex).historyContent.get(listMessageHistory_selectedIndex));
			} else {
				listMessagesContent_area.setText("");
			}
		} else {

			listMessagesHistory_list.setListData(new String[0]);
			listMessagesHistory_list.updateUI();
			
			listMessagesContent_area.setText("");
		}
		
		SwingUtilities.invokeLater(runnableUpdateUI);
	}
	
	// initial setting up of GUI panel using Swing api
	public void setupPanel() {
		int width = 800;
		int height = 600;
		
		main_panel = new JPanel (new GridLayout(4,3));
		
		String title_string = "Hello world!!! Version = " + Version.version;
		String subtitle_string = "This is an example GUI to show a RTI ('Really Thankful Interface'), which is similar to an RTI ('Real Time Interface') as described in an HLA system.";

		JTextArea title_area = new JTextArea(6,50);
		title_area.setLineWrap(true);
		title_area.setWrapStyleWord(true);
		title_area.setEditable(false);
		title_area.setTabSize(2);
		title_area.setText(title_string + "\n" + subtitle_string);
		
		String instruct_string = "To connect a simulation to this RTI, please use the following credentials:" + "<br/>"
				+ "&nbsp &nbsp <b>hostname</b> = " + hostAddress + "<br/>"
				+ "&nbsp &nbsp &nbsp &nbsp <i>(or you can use 'localhost' if running simulation on same computer as RTI)</i>" + "<br/>"
				+ "&nbsp &nbsp <b>portnumber</b> = " + hostPort;
		JTextArea instruct_area = new JTextArea(4, 50);
		instruct_area.setLineWrap(true);
		instruct_area.setWrapStyleWord(true);
		instruct_area.setEditable(false);
		instruct_area.setTabSize(2);
		instruct_area.setText(instruct_string);
		
		JTextPane instruct_pane = new JTextPane();
		instruct_pane.setContentType("text/html");
		instruct_pane.setEditable(false);
		instruct_pane.setText(instruct_string);
		instruct_pane.setMaximumSize(new Dimension(10,10));
		
		JPanel buttonPanel = new JPanel();
		JButton exportMessageHistory_button = new JButton("Export Message History");
		exportMessageHistory_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				printLine("<<YOU CLICKED BUTTON to print history>>");
				try {
					FileWriter exportFile = new FileWriter("messagehistory_" + System.currentTimeMillis() + ".txt");
					
					String outputString = "";
					outputString += "Message Name \t Timestamp \t From Sim \t Message Content \t \n";
					for (int i = 0; i < listMessages_items.size(); i++) {
						for (int j = 0; j < listMessages_items.get(i).historyTimestamps.size(); j++) {
							outputString += listMessages_items.get(i).name + "\t"
									+ listMessages_items.get(i).historyTimestamps.get(j) + "\t"
									+ listMessages_items.get(i).historySource.get(j) + "\t"
									+ listMessages_items.get(i).historyContent.get(j) + "\t \n";
						}
					}
					
					exportFile.write(outputString);
					exportFile.flush();
					exportFile.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				
				
			}
			
		});
		//"Export Message History"
		JButton sendStartRequest_button = new JButton("Send Start Request To All");
		sendStartRequest_button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				printLine("<<YOU CLICKED BUTTON to send start request>>");
				rtiLib.publish("RTI_StartSim", "");
			}
		});
		final JButton sendPauseRequest_button = new JButton("Send Pause Request To Server");
		sendPauseRequest_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (sendPauseRequest_button.getText().compareTo("Send Pause Request To Server") == 0) {
					printLine("<<YOU CLICKED BUTTON to send pause request>>");
					sendPauseRequest_button.setText("Send Resume Request To Server");
					rtiLib.publish("RTI_PauseSystem", "");
				} else {
					printLine("<<YOU CLICK BUTTON to send resume request>>");
					sendPauseRequest_button.setText("Send Pause Request To Server");
					rtiLib.publish("RTI_ResumeSystem", "");
				}
				
			}
		});
		buttonPanel.add(exportMessageHistory_button);
		buttonPanel.add(sendStartRequest_button);
		buttonPanel.add(sendPauseRequest_button);
		
		String numApps_string = "Number of applications connected: " + numOfApps;
		numApps_area = new JTextArea(2, 50);
		numApps_area.setLineWrap(true);
		numApps_area.setWrapStyleWord(true);
		numApps_area.setEditable(false);
		numApps_area.setTabSize(2);
		numApps_area.setText(numApps_string);
		numApps_area.setBorder(BorderFactory.createTitledBorder("Data:"));
		
		
		String[] listApps_items_string = new String[listApps_items.size()];
		for (int i = 0; i < listApps_items_string.length; i++) {
			listApps_items_string[i] = listApps_items.get(i).name;
		}
		JScrollPane listApps_scrollpane = new JScrollPane();
		listApps_list = new JList();
		listApps_list.setListData(listApps_items_string);

		listApps_list.addListSelectionListener(new ListAppSelectionHandler());
		listApps_scrollpane.setViewportView(listApps_list);
		listApps_scrollpane.setBorder(BorderFactory.createTitledBorder("List of connected apps:"));
		
		JScrollPane listAppsPublishTo_scrollpane = new JScrollPane();
		listAppsPublishTo_list = new JList();
		listAppsPublishTo_list.setListData(listAppsPublishTo_items.toArray());

		listAppsPublishTo_scrollpane.setViewportView(listAppsPublishTo_list);
		listAppsPublishTo_scrollpane.setBorder(BorderFactory.createTitledBorder("PUBLISH TO (click on connected app to see):"));
		
		JScrollPane listAppsSubscribeTo_scrollpane = new JScrollPane();
		listAppsSubscribeTo_list = new JList();
		listAppsSubscribeTo_list.setListData(listAppsSubscribeTo_items.toArray());

		listAppsSubscribeTo_scrollpane.setViewportView(listAppsSubscribeTo_list);
		listAppsSubscribeTo_scrollpane.setBorder(BorderFactory.createTitledBorder("SUBSCRIBE TO (click on connected app to see):"));
		
		JScrollPane listMessages_scrollpane = new JScrollPane();
		listMessages_list = new JList();
		ArrayList<String> listMessages_items_string = new ArrayList<String>();
		for (int i = 0; i < listMessages_items.size(); i++) {
			listMessages_items_string.add(listMessages_items.get(i).name);
		}
		listMessages_list.setListData(listMessages_items_string.toArray());

		listMessages_list.addListSelectionListener(new ListMessageSelectionHandler());
		listMessages_scrollpane.setViewportView(listMessages_list);
		listMessages_scrollpane.setBorder(BorderFactory.createTitledBorder("List of data messages:"));
		
		JScrollPane listMessagesHistory_scrollpane = new JScrollPane();
		listMessagesHistory_list = new JList();
		listMessagesHistory_list.addListSelectionListener(new ListMessageHistorySelectionHandler());
		listMessagesHistory_scrollpane.setViewportView(listMessagesHistory_list);
		listMessagesHistory_scrollpane.setBorder(BorderFactory.createTitledBorder("Data Message HISTORY:"));
		
		JScrollPane listMessagesContent_scrollpane = new JScrollPane();
		listMessagesContent_area = new JTextArea();
		listMessagesContent_area.setLineWrap(true);
		listMessagesContent_area.setWrapStyleWord(true);
		listMessagesContent_area.setEditable(false);
		listMessagesContent_scrollpane.setViewportView(listMessagesContent_area);
		listMessagesContent_scrollpane.setBorder(BorderFactory.createTitledBorder("Data Message CONTENT:"));
		
		/* 
		 * 1, 2, 3
		 * 4, 5, 6
		 * 7, 8, 9
		 */
		main_panel.add(title_area);						//1
		main_panel.add(instruct_pane);					//2
		main_panel.add(new JPanel(null));				//3
		main_panel.add(numApps_area);					//4
		main_panel.add(buttonPanel);					//5
		main_panel.add(new JPanel(null));				//6
		main_panel.add(listApps_scrollpane);			//7
		main_panel.add(listAppsPublishTo_scrollpane);	//8
		main_panel.add(listAppsSubscribeTo_scrollpane);	//9
		main_panel.add(listMessages_scrollpane);		//10
		main_panel.add(listMessagesHistory_scrollpane);	//11
		main_panel.add(listMessagesContent_scrollpane);	//12
		
		setContentPane(main_panel);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(width,height);
		setVisible(true);
		
		if (guiLoaded == false) {
			printLine("Loaded GUI panel, should be visible now.");
		}
		guiLoaded = true;
	}
	
	public void updateHost(String newAddress, String newPort) {
		hostAddress = newAddress;
		hostPort = newPort;
	}
	
	public void updateNumConnected(int newNumConnected) {
		numOfApps = newNumConnected;
		printLine("\t updating number of connected apps = " + numOfApps);
		
		numApps_area.setText("Number of applications connected: " + numOfApps);
	}
	
	public String addNewLine(String subjectString, int width) {
		String returnString = "";
		
		int lengthCounter = 0;
		for (int i = 0; i < subjectString.length(); i++) {
			returnString += subjectString.charAt(i);
			lengthCounter++;
			if (lengthCounter > width * 0.01f) {
				lengthCounter = 0;
				returnString += '\n';
			}
		}
		
		return returnString;
	}

	
	
	// necessary function to implement "RTISim"
	public String getSimName() {
		String returnString = "";
		returnString = tag;
		return returnString;
	}
	
	//!!!! Below is RTISim-specific logic to handle specific messages from RTI Server 
	@Override
	public void receivedMessage(String messageName, String content, String timestamp, String vTimestamp, String source) {
		
		switch(messageName) {
			case "RTI_UpdateSim":
				//need to parse out each "sim," its "publish" and "subscribe" messages
				//total number of sims and total list of messages can be calculated after.
				//printLine(" Received message of name " + messageName + ", I'll use it!");
				listApps_items.clear();
				
				updateNumConnected(rtiLib.getJsonArray(rtiLib.getJsonObject("subcontent",content)).length);
				for (int i = 0; i < rtiLib.getJsonArray(rtiLib.getJsonObject("subcontent",content)).length; i++) {
					ConnectedSim newSim = new ConnectedSim();
					newSim.name = rtiLib.getStringNoQuotes(rtiLib.getJsonObject("name",rtiLib.getJsonArray(rtiLib.getJsonObject("subcontent",content))[i]));
					newSim.publishTo = new ArrayList<String>();
					String publishToJson = rtiLib.getJsonObject("publishTo", rtiLib.getJsonArray(rtiLib.getJsonObject("subcontent",content))[i]).toString();
					//printLine("PUBLISHTO : " + publishToJson);
					for (int j = 0; j < rtiLib.getJsonArray(publishToJson).length; j++) {
						newSim.publishTo.add(rtiLib.getStringNoQuotes(rtiLib.getJsonArray(publishToJson)[j]));
					}
					newSim.subscribeTo = new ArrayList<String>();

					String subscribeToJson = rtiLib.getJsonObject("subscribeTo", rtiLib.getJsonArray(rtiLib.getJsonObject("subcontent",content))[i]).toString();
					//printLine("SUBSCRIBETO : " + subscribeToJson);
					for (int j = 0; j <  rtiLib.getJsonArray(subscribeToJson).length; j++) {
						newSim.subscribeTo.add(rtiLib.getStringNoQuotes(rtiLib.getJsonArray(subscribeToJson)[j]));
					}
					listApps_items.add(newSim);
				}
				printLine("Now currently this many connected apps: " + listApps_items.size());
				
				updateSimList();
				break;
			case "RTI_UpdateMessage":
				//need to parse out "messageName" and "content," then add to the appropriate list history here.
				//printLine(" Received message of name " + messageName + ", I'll use it!");
				//technically, both "RTI_UpdateMessage" and the original message in the content need to be updated (note: not all sims should subscribe to RTI_UpdateMessage by default)
				String contentMessageName = rtiLib.getJsonObject("name", content);
				boolean alreadyListed = false;
				int existIndex = 0;
				for (int i = 0; i < listMessages_items.size(); i++) {
					if (listMessages_items.get(i).name.compareTo(contentMessageName) == 0) {
						alreadyListed = true;
						existIndex = i;
						break;
					}
				}
				if (alreadyListed == true) {
					listMessages_items.get(existIndex).historyTimestamps.add(rtiLib.getJsonObject("timestamp", content));
					listMessages_items.get(existIndex).historySource.add(rtiLib.getJsonObject("source", content));
					listMessages_items.get(existIndex).historyContent.add(rtiLib.getJsonObject("content", content));
				} else {
					MessageHistory newMessageItem = new MessageHistory();
					newMessageItem.name = contentMessageName;
					newMessageItem.historyTimestamps = new ArrayList<String>();
					newMessageItem.historyTimestamps.add(rtiLib.getJsonObject("timestamp", content));
					newMessageItem.historySource = new ArrayList<String>();
					newMessageItem.historySource.add(rtiLib.getJsonObject("source", content));
					newMessageItem.historyContent = new ArrayList<String>();
					newMessageItem.historyContent.add(rtiLib.getJsonObject("content", content));
					listMessages_items.add(newMessageItem);
				}
				updateMessageList();
				break;
			default:
				//printLine(" Received message of name " + messageName + " but I don't know what to do with it...");
				break;
		}
		
		// in addition to the message received in "RTI_UpdateMessage," (meant to represent all messages) add all other messages that might have been received
		String contentMessageName = messageName;
		boolean alreadyListed = false;
		int existIndex = 0;
		for (int i = 0; i < listMessages_items.size(); i++) {
			if (listMessages_items.get(i).name.compareTo(contentMessageName) == 0) {
				alreadyListed = true;
				existIndex = i;
				break;
			}
		}
		if (alreadyListed == true) {
			if (alreadyReceivedMessage(contentMessageName, content, timestamp, source) == false) {
				listMessages_items.get(existIndex).historyTimestamps.add(timestamp);
				listMessages_items.get(existIndex).historySource.add(source);
				listMessages_items.get(existIndex).historyContent.add(content);
			
				if (messageLimit > 0) {
					while (listMessages_items.size() > messageLimit) {
						listMessages_items.remove(0);
					}
					for (int i = 0; i < listMessages_items.size(); i++) {
						while (listMessages_items.get(i).historySource.size() > messageLimit) {
							listMessages_items.get(i).historySource.remove(0);
							listMessages_items.get(i).historyContent.remove(0);
							listMessages_items.get(i).historyTimestamps.remove(0);
						}
					}
				}
			}
		} else {
			MessageHistory newMessageItem = new MessageHistory();
			newMessageItem.name = contentMessageName;
			newMessageItem.historyTimestamps = new ArrayList<String>();
			newMessageItem.historyTimestamps.add(timestamp);
			newMessageItem.historySource.add(source);
			newMessageItem.historyContent = new ArrayList<String>();
			newMessageItem.historyContent.add(content);
			listMessages_items.add(newMessageItem);
			
			if (messageLimit > 0) {
				while (listMessages_items.size() > messageLimit) {
					listMessages_items.remove(0);
				}
				for (int i = 0; i < listMessages_items.size(); i++) {
					while(listMessages_items.get(i).historySource.size() > messageLimit) {
						listMessages_items.get(i).historySource.remove(0);
						listMessages_items.get(i).historyContent.remove(0);
						listMessages_items.get(i).historyTimestamps.remove(0);
					}
				}
			}
		}
		updateMessageList();
	}
	
	// need to be able to decide what to do if duplicate message is received
	private boolean alreadyReceivedMessage(String name, String content, String timestamp, String source) {

		for(int i = 0; i < listMessages_items.size(); i++) {
			if (listMessages_items.get(i).name.compareTo(name) == 0){
				for (int j = 0; j < listMessages_items.get(i).historyTimestamps.size(); j++) {
					if (listMessages_items.get(i).historyTimestamps.get(j).compareTo(timestamp) == 0) {
						if (listMessages_items.get(i).historySource.get(j).compareTo(source) == 0 &&
								listMessages_items.get(i).historyContent.get(j).compareTo(content) == 0) {
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	
	public void printLine(String line) {
		String formatLine = String.format("%1$32s", "[" + tag + "]" + " --- ") + line;
		Version.printConsole(formatLine);
		Version.printFile(formatLine);
	}
	
	public static void main(String[] args) {
		System.out.println("Example running ExampleServerGUI.java with ui...");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ExampleServerGUI gui = new ExampleServerGUI("35.3.104.242", "4200");
				gui.setupPanel();
			}
		});
		System.out.println("Done.");
	}

	

}

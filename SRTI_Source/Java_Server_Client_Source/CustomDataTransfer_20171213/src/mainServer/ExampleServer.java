package mainServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

public class ExampleServer {

	public static void main(String args[]) {
		ExampleServer server = new ExampleServer();		

		server.loadSettingsFile();
		server.startRTI();

		if (server.guiOn == true) {
			ExampleServerGUI serverGUI = new ExampleServerGUI(server.getHostName(), server.getPortNumber());
			serverGUI.rtiLib.setDebugOutput(true);
		}
	}
	
	
	
	
	
	String tag = "ExampleServer";
	
	private String hostName = "localhost";
	public int portNumber = -1;
	public boolean guiOn = true;
	public boolean tcpOn = false;
	public boolean compressOn = false;
	public boolean retryConnection = false;
	public int oldMessageLimit = -1;
	
	ArrayList<RTIConnectThread> threadList = new ArrayList<RTIConnectThread>();
	ArrayList<String> messageHistoryList = new ArrayList<String>();
	
	public int loadSettingsFile() {
		printLine("Trying to read settings file.");
		try {
			FileReader configStream = new FileReader("settings.txt");
			BufferedReader configBuffer = new BufferedReader(configStream);
			String jsonString = "";
			String line = "";
			while ((line = configBuffer.readLine()) != null) {
				jsonString += line;
			}
			jsonString = stringToJson(jsonString);
			JsonReader reader = Json.createReader(new StringReader(jsonString));
			JsonObject json = reader.readObject();
			reader.close();
			
			portNumber = json.getJsonNumber("portNumber").intValue();
			guiOn = json.getBoolean("gui");
			tcpOn = json.getBoolean("tcp");
			compressOn = json.getBoolean("compress");
			retryConnection = json.getBoolean("retryConnection");
			oldMessageLimit = json.getJsonNumber("oldMessageLimit").intValue();
		} catch (Exception e) {
			e.printStackTrace();
			printLine("Error trying to open settings.txt file, will proceed with default values.");
		}
		return 0;
	}
	
	private String stringToJson(String string) {
		String returnString = "";
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != '\n') {
				returnString += string.charAt(i);
			}
		}
		return returnString;
	}
	
	public int startRTI() {
				
		File currentDirectory = new File(".");
		File[] listOfFiles = currentDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				boolean startsWith = fileName.startsWith("messagehistorylist_");
				boolean endsWith = fileName.endsWith(".txt");
				return startsWith && endsWith;
			}
		});
		for (int i = 0; i < listOfFiles.length; i++) {
			listOfFiles[i].delete();
		}
		
		try {
			//hostName = InetAddress.getLoopbackAddress().toString();		//"localhost/127.0.0.1"
			//hostName = InetAddress.getLocalHost().toString();			//"DESKTOP-8DQHEEH/35.3.63.93"
			//hostName = InetAddress.getLocalHost().getHostName();		//"DESKTOP-8DQHEEH"
			hostName = InetAddress.getLocalHost().getHostAddress();		//"35.3.63.93"
			// if using "ipconfig" command on command prompt, I get IPv4 Address = "35.3.63.93"
		} catch (Exception e) {
			// TODO Auto-generated catch block
			printLine("... error here => " + e);
			return -1;
		}
		

		ServerSocket serverSocket;
		try {
			printLine("Try to create socket on this port: " + portNumber);
			if (portNumber == -1)
				serverSocket = new ServerSocket(0);
			else
				serverSocket = new ServerSocket(portNumber);
			portNumber = serverSocket.getLocalPort();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return -1;
		}
		

		
		printLine("This is an example GUI to show a RTI ('Really Thankful Interface'), which is similar to an RTI ('Real Time Interface') as described in an HLA system.");
		printLine("To connect a simulation to this RTI, please use the following credentials:\n"
				+ "\t hostname = " + hostName + "\n" 
				+ "\t\t (or you can use 'localhost' if running simulation on same computer as RTI)" + "\n" 
				+ "\t portnumber = " + portNumber);
		
		/*if (serverGUI != null) {
			serverGUI.updateHost(hostName, "" + portNumber);
			serverGUI.setupPanel();
		}*/
		
		printLine("Opening socket connection.");
		
		RTIStartThread mainThread = new RTIStartThread(serverSocket);
		mainThread.start();
		
		//printLine("Main thread running, just need to connect some apps now...");
		
		return 0;
	}
	
	public class RTIStartThread extends Thread{
		
		ServerSocket serverSocket;
		Socket rtiSocket;
		
		public RTIStartThread(ServerSocket serverSock) {
			serverSocket = serverSock;
		}
		
		public void run() {
			while (true) {
				try {
					printLine("Waiting for simulations to connect to me...");

					rtiSocket = serverSocket.accept();
					printLine("\t Connected!");
					//RTIConnectThread connectThread = new RTIConnectThread(rtiSocket, this);
					RTIConnectThread connectThread = new RTIConnectThread(rtiSocket, this, tcpOn);
					connectThread.start();
					threadList.add(connectThread);
					
					printLine("\t Added thread to the list! Now sim size is: " + threadList.size());
					
					/*if (serverGUI != null) {
						serverGUI.updateNumConnected(connectThread.getNumberOfThreads());
					}*/
				} catch (Exception e) {
					printLine("... error here => " + e);
					try {
						serverSocket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						printLine("... error here => " + e1);
						//return -1;
					}
					//return -1;
				}
			}
		}
		
		public void handleReceivedMessage(int threadIndex, String message) {
			printLine("received message from index " + threadIndex + ": " + message);
			
			JsonReader reader = Json.createReader(new StringReader(message));
			JsonObject json = reader.readObject();
			
			String name = json.getString("name");
			String content = json.getString("content");
			boolean tcp = Boolean.parseBoolean(json.getString("tcp"));
			
			if (tcp == true) {
				if (name.compareTo("RTI_ReceivedMessage") != 0) {
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {	
							threadList.get(i).update("RTI_ReceivedMessage", message);
						}
					}
				}
			}
			
			String rtiUpdateSimString = "";
			
			switch (name){
				case "RTI_ReceivedMessage":
					// Tell sim it received the message, it can stop waiting for response now.
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {	
							threadList.get(i).setTcpResponse(true, content);
						}
					}
					return;
				case "RTI_InitializeSim":
					printLine("received message, use info to intialize sim name...");
					
					String newSimName = Json.createReader(new StringReader(content)).readObject().getString("simName");
					int numOfDuplicates = 0;
					// WAIT! First check to see if there is already another sim with the same name, if so, then need to change the name
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getSimName().compareTo(newSimName) == 0) {
							numOfDuplicates++;
						}
					}
					
					
					// Update sim name to the appropriate thread
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {	
							threadList.get(i).updateSimName(newSimName);
							threadList.get(i).setNumOfSameName(numOfDuplicates);
						}
					}
					
					// by default, send out update of current thread list to everyone
					rtiUpdateSimString = buildRTIUpdateSim();
					//JsonObject jsonContent = Json.createObjectBuilder();
					for (int i = 0; i < threadList.size(); i++) {
						threadList.get(i).update(rtiUpdateSimString);
					}
					messageHistoryList.add(rtiUpdateSimString);
					
					break;
				case "RTI_PublishTo":
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {
							String newPublishName = Json.createReader(new StringReader(content)).readObject().getString("publishTo");
							threadList.get(i).updatePublishTo(newPublishName);
						}
					}
					rtiUpdateSimString = buildRTIUpdateSim();
					//JsonObject jsonContent = Json.createObjectBuilder();
					for (int i = 0; i < threadList.size(); i++) {
						threadList.get(i).update(rtiUpdateSimString);
					}
					messageHistoryList.add(rtiUpdateSimString);
					break;
				case "RTI_SubscribeTo":
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {
							String newSubscribeName = Json.createReader(new StringReader(content)).readObject().getString("subscribeTo");
							threadList.get(i).updateSubscribeTo(newSubscribeName);
						}
					}
					rtiUpdateSimString = buildRTIUpdateSim();
					//JsonObject jsonContent = Json.createObjectBuilder();
					for (int i = 0; i < threadList.size(); i++) {
						threadList.get(i).update(rtiUpdateSimString);
					}
					messageHistoryList.add(rtiUpdateSimString);
					break;
				case "RTI_SubscribeToAll":
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {
							threadList.get(i).setSubscribeAll();
						}
					}
					rtiUpdateSimString = buildRTIUpdateSim();
					//JsonObject jsonContent = Json.createObjectBuilder();
					for (int i = 0; i < threadList.size(); i++) {
						threadList.get(i).update(rtiUpdateSimString);
					}
					messageHistoryList.add(rtiUpdateSimString);
					break;
				case "RTI_SubscribeToAllPlusHistory":
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {
							threadList.get(i).setSubscribeAll();
							//additionally, must send all past messages to this one
							//start with files, then current memory
							File currentDirectory = new File(".");
							File[] listOfFiles = currentDirectory.listFiles(new FilenameFilter() {
								public boolean accept(File directory, String fileName) {
									boolean startsWith = fileName.startsWith("messagehistorylist_");
									boolean endsWith = fileName.endsWith(".txt");
									return startsWith && endsWith;
								}
							});
							for (int j = 0; j < listOfFiles.length; j++) {
								try {
									BufferedReader fileReader = new BufferedReader(new FileReader(listOfFiles[j].getName()));
									String readLine = "";
									readLine = fileReader.readLine();
									while (readLine != null){
										threadList.get(i).update(readLine);
										readLine = fileReader.readLine();
									}
									fileReader.close();
								} catch (Exception e) {
									e.printStackTrace();
									printLine("Some error when trying to read files to get old messages.");
								}
							}
							for (int j = 0; j < messageHistoryList.size(); j++) {
								threadList.get(i).update(messageHistoryList.get(j));
							}
						}
					}
					rtiUpdateSimString = buildRTIUpdateSim();
					//JsonObject jsonContent = Json.createObjectBuilder();
					for (int i = 0; i < threadList.size(); i++) {
						threadList.get(i).update(rtiUpdateSimString);
					}
					messageHistoryList.add(rtiUpdateSimString);
					break;
				case "RTI_SubscribeToMessagePlusHistory":
					for (int i = 0; i < threadList.size(); i++) {
						if (threadList.get(i).getIndex() == threadIndex) {
							String newSubscribeName = Json.createReader(new StringReader(content)).readObject().getString("subscribeTo");
							threadList.get(i).updateSubscribeTo(newSubscribeName);
							File currentDirectory = new File(".");
							File[] listOfFiles = currentDirectory.listFiles(new FilenameFilter() {
								public boolean accept(File directory, String fileName) {
									boolean startsWith = fileName.startsWith("messagehistorylist_");
									boolean endsWith = fileName.endsWith(".txt");
									return startsWith && endsWith;
								}
							});
							for (int j = 0; j < listOfFiles.length; j++) {
								try {
									BufferedReader fileReader = new BufferedReader(new FileReader(listOfFiles[j].getName()));
									String readLine = "";
									readLine = fileReader.readLine();
									while (readLine != null){										
										String name2 = Json.createReader(new StringReader(readLine)).readObject().getString("name");
										if (name2.compareTo(newSubscribeName) == 0) {
											threadList.get(i).update(readLine);
										}
										readLine = fileReader.readLine();
									}
									fileReader.close();
								} catch (Exception e) {
									e.printStackTrace();
									printLine("Some error when trying to read files to get old messages.");
								}
							}
							for (int j = 0; j < messageHistoryList.size(); j++) {
								String name2 = Json.createReader(new StringReader(messageHistoryList.get(j))).readObject().getString("name");
								if (name2.compareTo(newSubscribeName) == 0) {
									threadList.get(i).update(messageHistoryList.get(j));
								}
								threadList.get(i).update(messageHistoryList.get(j));
							}
						}
					}
					rtiUpdateSimString = buildRTIUpdateSim();
					//JsonObject jsonContent = Json.createObjectBuilder();
					for (int i = 0; i < threadList.size(); i++) {
						threadList.get(i).update(rtiUpdateSimString);
					}
					messageHistoryList.add(rtiUpdateSimString);
					break;
				default:
					printLine("received message, but don't know what to do with it... " + message);
					// if not a special RTI message, then search for sim thread that subscribed to it, and send out to them using "send(,)".
					break;
			}
			
			// HERE, change "source" if there were more than one, before sending the message back out again.
			String source = json.getString("source");
			String timestamp = json.getString("timestamp");
			
			for (int i = 0; i < threadList.size(); i++) {
				if (threadList.get(i).getIndex() == threadIndex) {	
					int numOfDuplicates = threadList.get(i).getNumOfSameName();
					if (numOfDuplicates > 0) {
						source = threadList.get(i).getSimName() + "_" + String.format("%03d", numOfDuplicates);
					}
				}
			}
			
			String newJsonMessage =  Json.createObjectBuilder()
					.add("name", name)
					.add("content", content)
					.add("timestamp", timestamp)
					.add("source", source)
					.build().toString();
			
			messageHistoryList.add(newJsonMessage);
			// what if messageHistoryList is too large? Write to a file to save it for later.
			// example: 1 message of 100 characters = 100 bytes, so 100 messages = 10 KB, 1,000 messages = 100 KB
			if (oldMessageLimit > 0 && messageHistoryList.size() > oldMessageLimit) {
				try {
					FileWriter exportFile = new FileWriter("messageHistoryList_" + System.currentTimeMillis() + ".txt");
					String outputString = "";
					for (int i = 0; i < messageHistoryList.size(); i++) {
						outputString += messageHistoryList.get(i) + "\n";
					}
					exportFile.write(outputString);
					exportFile.flush();
					exportFile.close();
					messageHistoryList.clear();
				} catch (Exception e) {
					e.printStackTrace();
					printLine("Error trying to save older messages to file. Will keep in memory.");
				}
			}
			
			//send message back out to all sims that are subscribed to it
			int subscribedToTotal = 0;
			for (int i = 0; i < threadList.size(); i++) {
				if (threadList.get(i).isSubscribedTo(name)) {
					subscribedToTotal ++;
					threadList.get(i).update(newJsonMessage);
				}
				//threadList.get(i).update("RTI_UpdateMessage", message);
			}
			printLine("There should be " + subscribedToTotal + " subscribed to message " + name);
		}
		
		public void disconnectThread(int threadIndex) {
			for (int i = 0; i < threadList.size(); i++) {
				if (threadList.get(i).getIndex() == threadIndex) {	
					threadList.remove(i);
					break;
				}
			}
			
			// by default, send out update of current thread list to everyone
			String rtiUpdateSimString = buildRTIUpdateSim();
			//JsonObject jsonContent = Json.createObjectBuilder();
			for (int i = 0; i < threadList.size(); i++) {
				threadList.get(i).update(rtiUpdateSimString);
			}
			messageHistoryList.add(rtiUpdateSimString);
		}
	}
	

	
	private String buildRTIUpdateSim() {
		String returnString = "";
		JsonObjectBuilder jsonMessageBuilder = Json.createObjectBuilder();
		JsonArrayBuilder jsonSimBuilder = Json.createArrayBuilder();
		JsonArray jsonSimArray;
		JsonObject jsonMessageObject;
		for (int i = 0; i < threadList.size(); i++) {
			JsonObjectBuilder jsonSimObject = Json.createObjectBuilder();
			JsonArrayBuilder jsonPublishArray = Json.createArrayBuilder();

			for (int j = 0; j < threadList.get(i).getPublishArray().length; j++) {
				jsonPublishArray.add(threadList.get(i).getPublishArray()[j]);
			}
			
			JsonArrayBuilder jsonSubscribeArray = Json.createArrayBuilder();
			if (threadList.get(i).isSubscribedToAll()) {
				jsonSubscribeArray.add("<<ALL>>");
			} else {
				for (int j = 0; j < threadList.get(i).getSubscribeArray().length; j++) {
					jsonSubscribeArray.add(threadList.get(i).getSubscribeArray()[j]);
				}
			}
			
			int numOfDuplicates = threadList.get(i).getNumOfSameName();
			if (numOfDuplicates == 0) {
				jsonSimObject.add("name", threadList.get(i).getSimName());
			} else {
				jsonSimObject.add("name", threadList.get(i).getSimName() + "_" + String.format("%03d", numOfDuplicates));
			}
			
			jsonSimObject.add("publishTo", jsonPublishArray.build());
			jsonSimObject.add("subscribeTo", jsonSubscribeArray.build());
			jsonSimBuilder.add(jsonSimObject.build());
		}
		jsonSimArray = jsonSimBuilder.build();
		
		jsonMessageBuilder.add("content", jsonSimArray.toString());
		jsonMessageBuilder.add("name", "RTI_UpdateSim");
		jsonMessageBuilder.add("source", "RTI");
		jsonMessageBuilder.add("timestamp", "" + System.currentTimeMillis());
		// to support TCP, add variable here to let RTILib know if it needs to confirm received message
		jsonMessageBuilder.add("tcp", tcpOn);
		//
		jsonMessageObject = jsonMessageBuilder.build();
		
		returnString = jsonMessageObject.toString();
		return returnString;
	}
	
	private String buildRTIPublishTo(String publishToName) {
		String returnString = "";
		JsonObjectBuilder jsonPublishBuilder = Json.createObjectBuilder();
		JsonObject jsonPublishObject;

		jsonPublishBuilder.add("publishTo", publishToName);
		jsonPublishObject = jsonPublishBuilder.build();

		returnString = jsonPublishObject.toString();
		return returnString;
	}
	
	private String buildRTISubscribeTo(String subscribeToName) {
		String returnString = "";
		JsonObjectBuilder jsonSubscribeBuilder = Json.createObjectBuilder();
		JsonObject jsonSubscribeObject;

		jsonSubscribeBuilder.add("subscribeTo", subscribeToName);
		jsonSubscribeObject = jsonSubscribeBuilder.build();

		returnString = jsonSubscribeObject.toString();
		return returnString;
	}

	public String getHostName() {
		return hostName;
	}
	
	public String getPortNumber() {
		return "" + portNumber;
	}
	
	public void printLine(String line) {
		System.out.println(String.format("%1$32s", "[" + tag + "]" + " --- ") + line);
	}
}

package com.polytech.mwsoc;

import com.polytech.mwsoc.map.InputDialog;
import com.polytech.mwsoc.map.MapViewer;
import com.soap.ws.client.generated.IRoutingService;
import com.soap.ws.client.generated.RoutingService;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;

import javax.jms.*;
import javax.swing.*;
import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String WSDL_URL = "http://localhost:8000/LetsGoBikingServer/RoutingService?wsdl";
	private static final String SERVICE_NAMESPACE = "http://tempuri.org/";
	private static final String SERVICE_NAME = "RoutingService";
	private static Connection connection;
	private static Session session;
	private static String QUEUE_NAME;
	private static int counter = 0;
	private static IRoutingService port;
	
	public static void main(String[] args) {
		try {
			// Setting up the origin and destination for the user
			String[] inputData = InputDialog.promptOriginDestination();
			String origin;
			String destination;
			if (inputData != null) {
				origin = inputData[0];
				destination = inputData[1];
			} else {
				throw new RuntimeException("No origin and destination provided!");
			}
			
			// Setting the SOAP server URL and the service name
			URL wsdlURL = new URL(WSDL_URL);
			QName serviceName = new QName(SERVICE_NAMESPACE, SERVICE_NAME);
			RoutingService service = new RoutingService(wsdlURL, serviceName);
			port = service.getBasicHttpBindingIRoutingService();
			
			// Getting the queue name where the server will send the itineraries
			QUEUE_NAME = port.getItineraryStepByStep(origin, destination, null);
			
			// Setting up the ActiveMQ consumer
			setupActiveMQConsumer(QUEUE_NAME);
		}
		catch(SOAPFaultException e) {
			String faultString = e.getFault().getFaultString();
			System.err.println("SOAP Fault: " + faultString);
			
			if(e.getFault().hasDetail()) {
				String detailText = e.getFault().getDetail().getTextContent();
				System.err.println("Detail: " + detailText);
			}
		}
		catch(JMSException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void requestUpdate() {
		try {
			System.out.println("Waiting for server response...");
			port.getItineraryUpdate(QUEUE_NAME);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getUserInput(Scanner scanner, String prompt) {
		System.out.println(prompt);
		String r = scanner.nextLine();
		return new String(r.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
	}
	
	private static void setupActiveMQConsumer(String QUEUE_NAME) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
		configureRedeliveryPolicy(connectionFactory);
		hideDebugLogs();
		
		connection = connectionFactory.createConnection();
		session = createSession(connection);
		
		MessageConsumer consumer = createConsumer(session, QUEUE_NAME);
		consumer.setMessageListener(createMessageListener());
	}
	
	private static void hideDebugLogs() {
		org.slf4j.Logger root = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		((ch.qos.logback.classic.Logger) root).setLevel(ch.qos.logback.classic.Level.OFF);
	}
	
	private static void configureRedeliveryPolicy(ActiveMQConnectionFactory connectionFactory) {
		RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(0);
		connectionFactory.setRedeliveryPolicy(redeliveryPolicy);
	}
	
	private static Session createSession(Connection connection) throws JMSException {
		connection.start();
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}
	
	private static MessageConsumer createConsumer(Session session, String queueName) throws JMSException {
		Destination destination = session.createQueue(queueName);
		return session.createConsumer(destination);
	}
	
	private static MessageListener createMessageListener() {
		return message -> {
			if(message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				try {
					if(textMessage.getText().equals("FINISHED")) {
						JOptionPane.showMessageDialog(null, "Itinerary is completed!", "[INFO]", JOptionPane.INFORMATION_MESSAGE);
						System.exit(0);
					}
					
					System.out.println("Received response from server!");
					String[] parts = textMessage.getText().split("\\|");
					MapViewer.updateText(parts[0]);
					List<ArrayList<double[]>> coordinates = parseCoordinates(parts[1]);
					if(counter == 0) {
						MapViewer.showMap(coordinates);
						counter++;
					}
					else {
						MapViewer.coordinates = coordinates;
						MapViewer.updateMap(coordinates);
					}
				}
				catch(JMSException e) {
					throw new RuntimeException(e);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	public static List<ArrayList<double[]>> parseCoordinates(String input) {
		ArrayList<ArrayList<double[]>> coordinates = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\[(\\[.*?\\])\\]");
		Matcher matcher = pattern.matcher(input);
		
		while(matcher.find()) {
			ArrayList<double[]> itineraryCoordinates = new ArrayList<>();
			String listString = matcher.group(1);
			
			String[] pairs = listString.split("\\],\\[");
			
			for(String pair : pairs) {
				String[] values = pair.replaceAll("\\[|\\]", "").split(",");
				double[] coordinatePair = new double[2];
				coordinatePair[1] = Double.parseDouble(values[0]);
				coordinatePair[0] = Double.parseDouble(values[1]);
				
				itineraryCoordinates.add(coordinatePair);
			}
			
			coordinates.add(itineraryCoordinates);
		}
		
		return coordinates;
	}
}
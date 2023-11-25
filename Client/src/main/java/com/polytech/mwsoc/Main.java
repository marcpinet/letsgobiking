package com.polytech.mwsoc;

import com.polytech.mwsoc.map.InputDialog;
import com.polytech.mwsoc.map.MapViewer;
import com.polytech.mwsoc.utils.JsonUtils;
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

public class Main {
	
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String WSDL_URL = "http://localhost:8000/LetsGoBikingServer/RoutingService?wsdl";
	private static final String SERVICE_NAMESPACE = "http://tempuri.org/";
	private static final String SERVICE_NAME = "RoutingService";
	private static String queueName;
	private static int counter = 0;
	private static IRoutingService port;
	private static JFrame loadingFrame;
	
	public static void main(String[] args) {
		try {
			// Setting up the origin and destination for the user
			String[] inputData = InputDialog.promptOriginDestination();
			String origin;
			String destination;
			if(inputData.length == 2) {
				origin = inputData[0];
				destination = inputData[1];
			}
			else {
				throw new RuntimeException("No origin and destination provided!");
			}
			
			showLoadingIndicator();
			
			// Setting the SOAP server URL and the service name
			URL wsdlURL = new URL(WSDL_URL);
			QName serviceName = new QName(SERVICE_NAMESPACE, SERVICE_NAME);
			RoutingService service = new RoutingService(wsdlURL, serviceName);
			port = service.getBasicHttpBindingIRoutingService();
			
			// Getting the queue name where the server will send the itineraries
			queueName = port.getItineraryStepByStep(origin, destination, null);
			
			// Setting up the ActiveMQ consumer
			setupActiveMQConsumer(queueName);
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
			port.getItineraryUpdate(queueName);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void setupActiveMQConsumer(String QUEUE_NAME) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
		configureRedeliveryPolicy(connectionFactory);
		hideDebugLogs();
		
		Connection connection = connectionFactory.createConnection();
		Session session = createSession(connection);
		
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
					List<ArrayList<double[]>> coordinates = JsonUtils.parseCoordinates(parts[1]);
					if(counter == 0) {
						hideLoadingIndicator();
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
	
	private static void showLoadingIndicator() {
		loadingFrame = new JFrame("Loading");
		JLabel loadingLabel = new JLabel("Loading.", SwingConstants.CENTER);
		loadingFrame.add(loadingLabel);
		loadingFrame.setSize(300, 200);
		loadingFrame.setLocationRelativeTo(null);
		loadingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loadingFrame.setVisible(true);
		
		// Create a new thread for the animation
		new Thread(() -> {
			try {
				String[] loadingTexts = {"Loading.", "Loading..", "Loading...", "Almost there!", "Just a bit longer!", "Mettez moi 20 svp", "????", "Loading...", "Loading.."};
				int i = 0;
				while(loadingFrame.isVisible()) {
					loadingLabel.setText(loadingTexts[i % loadingTexts.length]);
					i++;
					Thread.sleep(500);
				}
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	private static void hideLoadingIndicator() {
		loadingFrame.setVisible(false);
		loadingFrame.dispose();
	}
}
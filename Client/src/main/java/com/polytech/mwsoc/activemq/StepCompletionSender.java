package com.polytech.mwsoc.activemq;

import javax.jms.*;

public class StepCompletionSender {
	private MessageProducer producer;
	private Session session;
	
	public StepCompletionSender(Session session, String destinationQueue) throws JMSException {
		this.session = session;
		Destination destination = session.createQueue(destinationQueue);
		this.producer = session.createProducer(destination);
	}
	
	public void sendStepCompletionMessage(String stepInfo) throws JMSException {
		TextMessage message = session.createTextMessage(stepInfo);
		producer.send(message);
	}
}

package com.polytech.mwsoc.map;

import javax.swing.*;
import java.awt.*;

public class TransparentTextPanel extends JPanel {
	private JTextArea textArea;
	private JScrollPane scrollPane;
	
	public TransparentTextPanel() {
		setLayout(new BorderLayout());
		setOpaque(false);
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setOpaque(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		
		scrollPane = new JScrollPane(textArea);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(null);
		
		add(scrollPane, BorderLayout.CENTER);
	}
	
	public void addMessage(String message) {
		textArea.append(message + "\n");
		scrollToBottom();
	}
	
	private void scrollToBottom() {
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
}

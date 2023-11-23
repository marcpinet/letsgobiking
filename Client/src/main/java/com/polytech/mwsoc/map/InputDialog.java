package com.polytech.mwsoc.map;

import javax.swing.*;

public class InputDialog {
	
	public static String[] promptOriginDestination() {
		JTextField originField = new JTextField(10);
		JTextField destinationField = new JTextField(10);
		
		JPanel panel = new JPanel();
		panel.add(new JLabel("Enter origin:"));
		panel.add(originField);
		panel.add(Box.createHorizontalStrut(15)); // a spacer
		panel.add(new JLabel("Enter destination:"));
		panel.add(destinationField);
		
		int result = JOptionPane.showConfirmDialog(null, panel,
				"Please Enter Origin and Destination", JOptionPane.OK_CANCEL_OPTION);
		
		if (result == JOptionPane.OK_OPTION) {
			return new String[]{originField.getText(), destinationField.getText()};
		} else {
			return null; // User canceled, or closed the dialog
		}
	}
}

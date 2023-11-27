package com.polytech.mwsoc.map;

import javax.swing.*;

public class InputDialog {
	
	public static String[] promptOriginDestination() {
		JTextField originField = new JTextField(10);
		JTextField destinationField = new JTextField(10);
		
		// Création du JSpinner pour l'entier
		SpinnerModel model = new SpinnerNumberModel(1, 1, 99, 1);
		JSpinner spinner = new JSpinner(model);
		
		JPanel panel = new JPanel();
		panel.add(new JLabel("Enter origin:"));
		panel.add(originField);
		panel.add(Box.createHorizontalStrut(15)); // a spacer
		panel.add(new JLabel("Enter destination:"));
		panel.add(destinationField);
		panel.add(Box.createHorizontalStrut(15)); // a spacer
		panel.add(new JLabel("Enter minimum bikes:"));
		panel.add(spinner);
		
		int result = JOptionPane.showConfirmDialog(null, panel,
				"Please Enter Origin and Destination", JOptionPane.OK_CANCEL_OPTION);
		
		if(result == JOptionPane.OK_OPTION) {
			String number = spinner.getValue().toString();
			return new String[]{originField.getText(), destinationField.getText(), number};
		}
		else {
			return new String[0];
		}
	}
}


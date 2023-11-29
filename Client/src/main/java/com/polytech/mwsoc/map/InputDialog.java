package com.polytech.mwsoc.map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class InputDialog {
	private static MouseAdapter currentMouseListener;
	
	public static String[] promptOriginDestination() {
		JTextField originField = new JTextField(10);
		JButton originMapButton = new JButton("📍");
		JTextField destinationField = new JTextField(10);
		JButton destinationMapButton = new JButton("📍");
		
		SpinnerModel model = new SpinnerNumberModel(1, 1, 99, 1);
		JSpinner spinner = new JSpinner(model);
		JXMapViewer mapViewer = new JXMapViewer();
		configMap(mapViewer);
		
		originMapButton.addActionListener(e -> selectLocation(mapViewer, originField));
		destinationMapButton.addActionListener(e -> selectLocation(mapViewer, destinationField));
		
		JPanel panel = new JPanel();
		panel.add(new JLabel("Enter origin:"));
		panel.add(originMapButton);
		panel.add(originField);
		panel.add(Box.createHorizontalStrut(15)); // a spacer
		panel.add(new JLabel("Enter destination:"));
		panel.add(destinationMapButton);
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
	
	private static void configMap(JXMapViewer mapViewer) {
		TileFactoryInfo info = new OSMTileFactoryInfo();
		DefaultTileFactory tileFactory = new DefaultTileFactory(info);
		mapViewer.setTileFactory(tileFactory);
		
		mapViewer.setZoom(13);
		mapViewer.setAddressLocation(new GeoPosition(46.81509864599243, 3.09814453125));  // France center
		
		MouseInputListener mia = new PanMouseInputListener(mapViewer);
		ZoomMouseWheelListenerCursor mwl = new ZoomMouseWheelListenerCursor(mapViewer);
		DelegatingMouseAdapter delegator = new DelegatingMouseAdapter();
		delegator.setMouseListener(mia);
		delegator.setMouseMotionListener(mia);
		delegator.setMouseWheelListener(mwl);
		
		final DelegatingMouseAdapterToggle toggle = new DelegatingMouseAdapterToggle(delegator);
		mapViewer.addMouseListener(delegator);
		mapViewer.addMouseMotionListener(delegator);
		mapViewer.addMouseWheelListener(delegator);
		mapViewer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				boolean right = SwingUtilities.isRightMouseButton(evt);
				if(right) {
					toggle.toggle();
				}
			}
		});
	}
	
	private static void selectLocation(JXMapViewer mapViewer, JTextField fieldToUpdate) {
		if(currentMouseListener != null)
			mapViewer.removeMouseListener(currentMouseListener);
		
		JDialog mapDialog = new JDialog(null, "Select Location", Dialog.ModalityType.APPLICATION_MODAL);
		mapDialog.add(mapViewer);
		mapDialog.setSize(800, 600);
		mapDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		currentMouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isLeftMouseButton(e)) {
					GeoPosition position = mapViewer.convertPointToGeoPosition(e.getPoint());
					fieldToUpdate.setText(position.toString().substring(1, position.toString().length() - 1).replace(" ", ""));
					mapDialog.dispose();
				}
			}
		};
		
		mapViewer.addMouseListener(currentMouseListener);
		mapDialog.setVisible(true);
	}
}


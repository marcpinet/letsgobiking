package com.polytech.mwsoc.map;

import com.polytech.mwsoc.Main;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapViewer {
	
	public static final int frameWidth = 1280;
	public static final int frameHeight = 720;
	public static volatile List<ArrayList<double[]>> firstItineraryReceivedCoordinates = new ArrayList<>();
	public static boolean firstItineraryReceived = true;
	public static volatile List<ArrayList<double[]>> coordinates = new ArrayList<>();
	public static volatile JButton updateButton;
	private static JXMapViewer mapViewer;
	private static TransparentTextPanel textPanel = new TransparentTextPanel();
	
	public static void showMap(List<ArrayList<double[]>> coordinates) {
		if(firstItineraryReceived) {
			// Deep copy of the coordinates list inside the firstItineraryReceivedCoordinates list
			for(ArrayList<double[]> it : coordinates) {
				ArrayList<double[]> temp = new ArrayList<>();
				for(double[] coord : it) {
					double[] tempCoord = new double[2];
					tempCoord[0] = coord[0];
					tempCoord[1] = coord[1];
					temp.add(tempCoord);
				}
				firstItineraryReceivedCoordinates.add(temp);
			}
			firstItineraryReceived = false;
		}
		
		
		MapViewer.coordinates = coordinates;
		mapViewer = new JXMapViewer();
		TileFactoryInfo info = new OSMTileFactoryInfo();
		DefaultTileFactory tileFactory = new DefaultTileFactory(info);
		mapViewer.setTileFactory(tileFactory);
		
		List<GeoPosition> track = new ArrayList<>();
		for(List<double[]> coordinate : coordinates) {
			for(double[] coord : coordinate) {
				track.add(new GeoPosition(coord[0], coord[1]));
			}
		}
		
		List<Integer> concatenatedIndices = new ArrayList<>();
		int sum = 0;
		for(ArrayList<double[]> it : coordinates) {
			sum += it.size();
			concatenatedIndices.add(sum);
		}
		
		RoutePainter routePainter = new RoutePainter(track, concatenatedIndices);
		
		int distance = (int) getFarthestDistance(track);
		int zoom = computeZoom(distance);
		mapViewer.setZoom(zoom);
		
		updateButton = new JButton("Refresh");
		updateButton.addActionListener(e -> {
			SwingUtilities.invokeLater(() -> updateButton.setEnabled(false));
			updateMap(MapViewer.coordinates);
			Main.requestUpdate();
		});
		
		mapViewer.add(updateButton);
		
		JLabel bikeLabel = new JLabel("<html><body style='color: blue;'>🔵 = JCDecaux bike itinerary</body></html>");
		JLabel walkLabel = new JLabel("<html><body style='color: red;'>🔴 = Walking itinerary</body></html>");
		JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		legendPanel.setOpaque(false);
		legendPanel.add(bikeLabel);
		legendPanel.add(walkLabel);
		mapViewer.add(legendPanel, BorderLayout.SOUTH);
		
		List<List<Double>> concatenatedCoordinates = new ArrayList<>();
		for(List<double[]> it : coordinates) {
			for(double[] coord : it) {
				List<Double> temp = new ArrayList<>();
				temp.add(coord[0]);
				temp.add(coord[1]);
				concatenatedCoordinates.add(temp);
			}
		}
		
		if(!coordinates.isEmpty()) {
			GeoPosition startPos = getApproximateCenter(concatenatedCoordinates);
			mapViewer.setAddressLocation(startPos);
		}
		
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
		
		// Waypoints
		Set<Waypoint> waypoints = new HashSet<>();
		
		if(!coordinates.isEmpty()) {
			double[] firstCoord = coordinates.get(0).get(0);
			waypoints.add(new GeoPositionWaypoint(new GeoPosition(firstCoord[0], firstCoord[1])));
		}
		
		for(List<double[]> coordinateList : coordinates) {
			if(!coordinateList.isEmpty()) {
				double[] lastCoord = coordinateList.get(coordinateList.size() - 1);
				waypoints.add(new GeoPositionWaypoint(new GeoPosition(lastCoord[0], lastCoord[1])));
			}
		}
		
		WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
		waypointPainter.setWaypoints(waypoints);
		
		CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>();
		compoundPainter.setPainters(routePainter, waypointPainter);
		mapViewer.setOverlayPainter(compoundPainter);
		
		JFrame frame = new JFrame("Itinerary Map");
		frame.getContentPane().add(mapViewer);
		textPanel.setPreferredSize(new Dimension(frameWidth / 3, frameHeight));
		frame.add(textPanel, BorderLayout.WEST);
		frame.setSize(frameWidth, frameHeight);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				frame.setVisible(false);
				frame.dispose();
				Main.main(new String[]{"reset"});
			}
		});
		frame.setVisible(true);
	}
	
	public static void updateMap(List<ArrayList<double[]>> coordinates) {
		// Update the track
		List<GeoPosition> updatedTrack = new ArrayList<>();
		for(List<double[]> coordinateList : coordinates) {
			for(double[] coord : coordinateList) {
				updatedTrack.add(new GeoPosition(coord[0], coord[1]));
			}
		}
		
		// Update concatenated indices for route segments
		List<Integer> concatenatedIndices = new ArrayList<>();
		int sum = 0;
		for(ArrayList<double[]> it : coordinates) {
			sum += it.size();
			concatenatedIndices.add(sum);
		}
		
		// Update RoutePainter
		RoutePainter routePainter = new RoutePainter(updatedTrack, concatenatedIndices);
		
		// Update waypoints
		Set<Waypoint> waypoints = new HashSet<>();
		if(!coordinates.isEmpty()) {
			double[] firstCoord = coordinates.get(0).get(0);
			waypoints.add(new GeoPositionWaypoint(new GeoPosition(firstCoord[0], firstCoord[1])));
		}
		
		for(List<double[]> coordinateList : coordinates) {
			if(!coordinateList.isEmpty()) {
				double[] lastCoord = coordinateList.get(coordinateList.size() - 1);
				waypoints.add(new GeoPositionWaypoint(new GeoPosition(lastCoord[0], lastCoord[1])));
			}
		}
		
		WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
		waypointPainter.setWaypoints(waypoints);
		
		CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>();
		compoundPainter.setPainters(routePainter, waypointPainter);
		mapViewer.setOverlayPainter(compoundPainter);
	}
	
	public static void updateText(String newText) {
		textPanel.addMessage(newText);
	}
	
	private static GeoPosition getApproximateCenter(List<List<Double>> coordinates) {
		int mid = coordinates.size() / 2;
		return new GeoPosition(coordinates.get(mid).get(0), coordinates.get(mid).get(1));
	}
	
	private static double getFarthestDistance(List<GeoPosition> coordinates) {
		double maxDistance = 0;
		for(int i = 1; i < coordinates.size(); i++) {
			double distance = getDistance(coordinates.get(0), coordinates.get(i));
			if(distance > maxDistance)
				maxDistance = distance;
		}
		return maxDistance;
	}
	
	public static double getDistance(GeoPosition pos1, GeoPosition pos2) {
		final int EARTH_RADIUS = 6371;
		
		double lat1 = pos1.getLatitude();
		double lon1 = pos1.getLongitude();
		double lat2 = pos2.getLatitude();
		double lon2 = pos2.getLongitude();
		
		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
				           Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
						           Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		
		return EARTH_RADIUS * c;
	}
	
	private static int computeZoom(int distance) {  // Kill me
		if(distance < 10)
			return 7;
		else if(distance < 20)
			return 8;
		else if(distance < 50)
			return 9;
		else if(distance < 100)
			return 10;
		else if(distance < 200)
			return 11;
		else if(distance < 500)
			return 12;
		else if(distance < 1000)
			return 13;
		else if(distance < 2000)
			return 14;
		else if(distance < 5000)
			return 15;
		else if(distance < 10000)
			return 16;
		else if(distance < 20000)
			return 17;
		else if(distance < 50000)
			return 18;
		else if(distance < 100000)
			return 19;
		else
			return 20;
		
	}
	
	public static void reset() {
		coordinates = new ArrayList<>();
		textPanel = new TransparentTextPanel();
		
		if(mapViewer != null) {
			mapViewer.removeAll();
			mapViewer = null;
		}
		
		if(updateButton != null) {
			updateButton.removeAll();
			updateButton = null;
		}
		
		firstItineraryReceivedCoordinates = new ArrayList<>();
		firstItineraryReceived = true;
	}
}

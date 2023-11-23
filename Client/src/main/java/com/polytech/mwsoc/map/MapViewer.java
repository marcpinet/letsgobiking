package com.polytech.mwsoc.map;

import com.polytech.mwsoc.Main;
import com.polytech.mwsoc.utils.JsonUtils;
import com.soap.ws.client.generated.Itinerary;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MapViewer {
	
	public static final int frameWidth = 1280;
	public static final int frameHeight = 720;
	private static JXMapViewer mapViewer;
	private static JButton updateButton;
	public static volatile List<ArrayList<double[]>> coordinates = new ArrayList<>();
	private static TransparentTextPanel textPanel = new TransparentTextPanel();
	
	public static void showMap(List<ArrayList<double[]>> coordinates) {
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
		mapViewer.setOverlayPainter(routePainter);
		
		int distance = (int) getFarthestDistance(track);
		int zoom = computeZoom(distance);
		mapViewer.setZoom(zoom);
		
		updateButton = new JButton("Refresh");
		updateButton.addActionListener(e -> {
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
		mapViewer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				boolean right = SwingUtilities.isRightMouseButton(evt);
				if(right) {
					toggle.toggle();
				}
			}
		});
		
		JFrame frame = new JFrame("Itinerary Map");
		frame.getContentPane().add(mapViewer);
		textPanel.setPreferredSize(new Dimension(frameWidth / 3, frameHeight));
		frame.add(textPanel, BorderLayout.WEST);
		frame.setSize(frameWidth, frameHeight);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
		mapViewer.setOverlayPainter(routePainter);
		
		// Optionally, if you need to adjust the view to fit the new route, you can use the following code.
		// Otherwise, keep the current zoom level and center position as is.
	    /*
	    int distance = (int) getFarthestDistance(updatedTrack);
	    int zoom = computeZoom(distance);
	    mapViewer.setZoom(zoom);
	
	    if (!coordinates.isEmpty()) {
	        GeoPosition newCenter = getApproximateCenter(concatenatedCoordinates);
	        mapViewer.setAddressLocation(newCenter);
	    }
	    */
	}
	
	public static void updateText(String newText) {
		textPanel.addMessage(newText);
	}
	
	private static GeoPosition getCenter(List<List<Double>> coordinates) {
		double lat = 0;
		double lon = 0;
		for(List<Double> coordinate : coordinates) {
			lat += coordinate.get(0);
			lon += coordinate.get(1);
		}
		return new GeoPosition(lat / coordinates.size(), lon / coordinates.size());
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
	
	private static int computeZoom(int distance) {
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
}

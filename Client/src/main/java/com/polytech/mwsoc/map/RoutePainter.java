package com.polytech.mwsoc.map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class RoutePainter implements Painter<JXMapViewer> {
	
	private List<GeoPosition> track;
	private List<Integer> concatenatedIndices;
	
	public RoutePainter(List<GeoPosition> track, List<Integer> concatenatedIndices) {
		this.track = track;
		this.concatenatedIndices = concatenatedIndices;
	}
	
	@Override
	public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
		g = (Graphics2D) g.create();
		
		Rectangle rect = map.getViewportBounds();
		g.translate(-rect.x, -rect.y);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Color[] colors = {Color.RED, Color.BLUE};
		int colorIndex = 0;
		
		g.setColor(colors[colorIndex]);
		g.setStroke(new BasicStroke(4));
		
		Point2D prevPoint = null;
		for(int i = 0; i < track.size(); i++) {
			GeoPosition gp = track.get(i);
			Point2D point = map.getTileFactory().geoToPixel(gp, map.getZoom());
			// If concatenated indices size is 1, color is red
			if(concatenatedIndices.size() == 1) {
				g.setColor(colors[0]);
			}
			// If concatenated indices size is 2, color is blue and red
			else if(concatenatedIndices.size() == 2) {
				if(i < concatenatedIndices.get(0)) {
					g.setColor(colors[1]);
				}
				else {
					g.setColor(colors[0]);
				}
			}
			// If concatenated indices size is 3, color is red, blue and red
			else if(concatenatedIndices.size() == 3) {
				if(i < concatenatedIndices.get(0)) {
					g.setColor(colors[0]);
				}
				else if(i < concatenatedIndices.get(1)) {
					g.setColor(colors[1]);
				}
				else {
					g.setColor(colors[0]);
				}
			}
			
			if(prevPoint != null)
				g.drawLine((int) prevPoint.getX(), (int) prevPoint.getY(), (int) point.getX(), (int) point.getY());
			prevPoint = point;
		}
		g.dispose();
	}
	
}

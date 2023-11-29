package com.polytech.mwsoc.map;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

class GeoPositionWaypoint extends DefaultWaypoint {
	public GeoPositionWaypoint(GeoPosition coord) {
		super(coord);
	}
}

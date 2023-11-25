package com.polytech.mwsoc.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soap.ws.client.generated.*;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final ObjectFactory objectFactory = new ObjectFactory();
	
	public static Itinerary convertJsonToItinerary(String json) throws IOException {
		JsonNode rootNode = mapper.readTree(json);
		JsonNode featuresNode = rootNode.path("features").get(0);
		JsonNode segmentsNode = featuresNode.path("properties").path("segments");
		JsonNode geometryNode = featuresNode.path("geometry").path("coordinates");
		
		Itinerary itinerary = objectFactory.createItinerary();
		
		ArrayOfSegment arrayOfSegment = objectFactory.createArrayOfSegment();
		List<Segment> segmentList = arrayOfSegment.getSegment();
		
		if(segmentsNode.isArray()) {
			for(JsonNode seg : segmentsNode) {
				Segment segment = mapper.treeToValue(seg, Segment.class);
				segmentList.add(segment);
			}
		}
		
		JAXBElement<ArrayOfSegment> segments = objectFactory.createItinerarySegments(arrayOfSegment);
		itinerary.setSegments(segments);
		
		ArrayOfArrayOfdouble arrayOfArrayOfDouble = objectFactory.createArrayOfArrayOfdouble();
		List<ArrayOfdouble> coordinatesList = arrayOfArrayOfDouble.getArrayOfdouble();
		
		if(geometryNode.isArray()) {
			for(JsonNode coordNode : geometryNode) {
				ArrayOfdouble arrayOfDouble = objectFactory.createArrayOfdouble();
				List<Double> coordList = arrayOfDouble.getDouble();
				coordList.add(coordNode.get(1).asDouble()); // latitude
				coordList.add(coordNode.get(0).asDouble()); // longitude
				coordinatesList.add(arrayOfDouble);
			}
		}
		
		JAXBElement<ArrayOfArrayOfdouble> coordinatesElement = objectFactory.createGeometryCoordinates(arrayOfArrayOfDouble);
		Geometry geometry = objectFactory.createGeometry();
		geometry.setCoordinates(coordinatesElement);
		
		JAXBElement<Geometry> geometryElement = objectFactory.createItineraryGeometry(geometry);
		itinerary.setGeometry(geometryElement);
		
		return itinerary;
	}
	
	public static String tempMethodToPrintItinerary(List<Itinerary> itineraries) {
		String result = "Itineraries: \n";
		
		for(Itinerary itinerary : itineraries) {
			result += "Itinerary: \n";
			for(Segment segment : itinerary.getSegments().getValue().getSegment()) {
				result += "Segment: " + "Distance = " + segment.getDistance() + "m Duration = " + segment.getDuration() + "s\n";
				for(Step step : segment.getSteps().getValue().getStep()) {
					result += "Step: " + "Distance = " + step.getDistance() + "m Duration = " + step.getDuration() + "s Instruction = " + step.getInstruction().getValue() + "\n";
				}
			}
		}
		return result;
	}
	
	public static List<List<Double>> extractCoordinatesFromItinerary(Itinerary response) {
		List<ArrayOfdouble> coordinatesList = response.getGeometry().getValue().getCoordinates().getValue().getArrayOfdouble();
		List<List<Double>> coordinates = new java.util.ArrayList<>();
		for(ArrayOfdouble arrayOfdouble : coordinatesList) {
			List<Double> coordinatePair = arrayOfdouble.getDouble();
			coordinates.add(coordinatePair);
		}
		return coordinates;
	}
	
	public static double[] getLastCoordinateFromStep(Step currentStep) {
		ArrayOfArrayOfdouble coordinates = currentStep.getCoordinates().getValue();
		List<ArrayOfdouble> coordinatesList = coordinates.getArrayOfdouble();
		ArrayOfdouble lastCoordinate = coordinatesList.get(coordinatesList.size() - 1);
		List<Double> lastCoordinateList = lastCoordinate.getDouble();
		double[] result = new double[2];
		result[0] = lastCoordinateList.get(0);
		result[1] = lastCoordinateList.get(1);
		return result;
	}
	
	public static List<ArrayList<double[]>> parseCoordinates(String input) {
		ArrayList<ArrayList<double[]>> coordinates = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\[(\\[.*?\\])\\]");
		Matcher matcher = pattern.matcher(input);
		
		while(matcher.find()) {
			ArrayList<double[]> itineraryCoordinates = new ArrayList<>();
			String listString = matcher.group(1);
			
			String[] pairs = listString.split("\\],\\[");
			
			for(String pair : pairs) {
				String[] values = pair.replaceAll("\\[|\\]", "").split(",");
				double[] coordinatePair = new double[2];
				coordinatePair[1] = Double.parseDouble(values[0]);
				coordinatePair[0] = Double.parseDouble(values[1]);
				
				itineraryCoordinates.add(coordinatePair);
			}
			
			coordinates.add(itineraryCoordinates);
		}
		
		return coordinates;
	}
}

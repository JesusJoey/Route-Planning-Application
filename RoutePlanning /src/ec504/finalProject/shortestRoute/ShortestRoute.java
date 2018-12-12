// @author Yang Qiao, Haoyu Xu, Jianqing Gao, Yuqiang Ning

package ec504.finalProject.shortestRoute;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ec504.finalProject.graph.Graph;
import ec504.finalProject.graph.WeightInterface;
import ec504.finalProject.graph.GraphInterface.Edge;
import ec504.finalProject.graph.GraphInterface.Vertex;

/*
 * In this class, we download Boston Map from Open Street Map in the format of a txt file
 * In our program, we locate the closest road to a given latitude/longitude location, determine the shortest
 * route between two latitude/longitude locations,  and convert the list of vertices into street names. 
 * Also we calculate the distance of the shortest route.
 */

public class ShortestRoute {

	private Graph<VertexData, EdgeData> graph;

	/*
	 * Method to get route direction from one location(latitude, longitude)
	 * to another.
	 */
	public static void showShortestRoute(String[] args) {

		// load existing map
		ShortestRoute showMap = new ShortestRoute();
		showMap.loadMap("src/ec504/finalProject/shortestRoute/BostonMap.txt");

		// parse location list
		ArrayList<Location> locationList = new ArrayList<Location>();
		try {
			// get input from given txt file which concludes two pairs of latitude and longitude
			Scanner scanner = new Scanner(new File("src/ec504/finalProject/shortestRoute/location.txt"));
			while (scanner.hasNextLine()) {
				double latitude = scanner.nextDouble();
				double longitude = scanner.nextDouble();
				locationList.add(new Location(latitude, longitude));
				//scanner.nextLine();
			}
			scanner.close();
		// fail to find route 
		} catch (FileNotFoundException e) {
			System.out.println("failed to parse file, exiting");
			return;
		}

		// find shortest route and print it
		List<String> vertexList = showMap.ShortestRoute(locationList.get(0), locationList.get(1));
		List<String> streetList = showMap.StreetRoute(vertexList);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < streetList.size()-1; i++) {
			sb.append(streetList.get(i));
			sb.append(" -> ");		
		}
		sb.append(streetList.get(streetList.size() - 1 ));
		String shortestPath = sb.toString();
		System.out.println("\nThe shortest path is:");
		System.out.println(shortestPath);
	}

	/*
	 * Constructor for new map
	 */
	public ShortestRoute() {
		graph = new Graph<VertexData, EdgeData>();
		graph.setDirectedGraph();
	}

	/*
	 * Getter method for graph, used by some test cases.
	 */
	public Graph<VertexData, EdgeData> getGraph() {
		return graph;
	}

	/**
	 * Get distance from location1 to location2
	 */
	public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		return calculateDistanceUtil(lat1, lon1, lat2, lon2);
	}

	/*
	 * Method to load an existing txt file into the graph
	 */
	public void loadMap(String filename) {

		// clear the graph
		if (graph.getVertices().size() != 0) {
			graph = new Graph<VertexData, EdgeData>();
			graph.setDirectedGraph();
		}

		// use document builder to parse txt
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document document;

		try {
			builder = factory.newDocumentBuilder();
			document = builder.parse(new File(filename));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.out.println("failed to parse txt, exiting");
			return;
		}

		document.normalize();

		// add vertices to graph
		NodeList vertexList = document.getElementsByTagName("node");
		int length = vertexList.getLength();
		for (int i = 0; i < length; i++) {
			addVertex(vertexList.item(i));
		}

		// add edges to graph
		NodeList edgeList = document.getElementsByTagName("way");
		length = edgeList.getLength();
		for (int i = 0; i < length; i++) {
			addEdge(edgeList.item(i));
		}
	}

	// method to add vertex from node
	private void addVertex(Node node) {
		Element item = (Element) node;

		// vertex name is id
		String id = item.getAttribute("id");

		// get latitude and longitude, save as VertexData
		double latitude = Double.parseDouble(item.getAttribute("lat"));
		double longitude = Double.parseDouble(item.getAttribute("lon"));
		VertexData data = new VertexData(latitude, longitude);

		// add new vertex to graph
		graph.addVertex(id, data);
	}

	// method to add edges from node
	private void addEdge(Node node) {
		boolean hasHighway = false;
		boolean hasName = false;
		boolean isOneWay = false;
		String streetName = "";

		// array list to store vertices listed in edge
		ArrayList<String> vertexList = new ArrayList<String>();

		// get children nodes of edge, iterate through each one
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node childNode = children.item(i);

			// ignore non-element node types
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) childNode;

				// check for highway, name, oneway, and yes tags
				if (childNode.getNodeName().equals("tag")) {
					if (childElement.getAttribute("k").equals("highway")) {
						hasHighway = true;
					}
					if (childElement.getAttribute("k").equals("name")) {
						hasName = true;
						streetName = childElement.getAttribute("v");
					}
					if (childElement.getAttribute("k").equals("oneway")) {
						if (childElement.getAttribute("v").equals("yes")) {
							isOneWay = true;
						}
					}
				}

				// get vertices for edges, add to list
				if (childNode.getNodeName().equals("nd")) {
					vertexList.add(childElement.getAttribute("ref"));
				}
			}
		}

		// if node is a highway and has name, add edges to graph using vertex list
		if (hasHighway && hasName) {

			// iterate through vertex list, make new edge for each pair
			for (int i = 0; i < vertexList.size() - 1; i++) {

				// calculate distance, store as EdgeData
				double distance = calculateDistance(vertexList.get(i), vertexList.get(i + 1));
				EdgeData data = new EdgeData(streetName, distance);

				// add to graph, if it is not oneway, also add edge in opposite direction
				graph.addEdge(vertexList.get(i), vertexList.get(i + 1), data);
				if (!isOneWay) {
					graph.addEdge(vertexList.get(i + 1), vertexList.get(i), data);
				}
			}
		}
	}

	// calculates the distance between two vertices using the Haversine formula
	private double calculateDistance(String vertexName1, String vertexName2) {
		double lat1 = graph.getVertexData(vertexName1).getLatitude();
		double lon1 = graph.getVertexData(vertexName1).getLongitude();
		double lat2 = graph.getVertexData(vertexName2).getLatitude();
		double lon2 = graph.getVertexData(vertexName2).getLongitude();

		// utility method to calculate distance using latitude/longitude
		return calculateDistanceUtil(lat1, lon1, lat2, lon2);
	}

	// calculates the distance between two latitude/longitude locations using the Haversine formula
	private static double calculateDistanceUtil(double lat1, double lon1, double lat2, double lon2) {
		double radius = 3959; // average radius of earth in miles
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
				Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = radius * c;
		return distance;
	}



	/**
	 * Method to find the closest vertex to a given location(latitude, longitude)
	 */
	public String ClosestRoad(Location location) {

		// start minimum distance at max size
		double minDistance = Double.MAX_VALUE;
		String closestRoad = null;

		// get list of vertices in graph
		List<Vertex<VertexData>> vertexList = graph.getVertices();
		int length = vertexList.size();

		// get latitude and longitude from location object
		double lat1 = location.getLatitude();
		double lon1 = location.getLongitude();

		// calculate distance from every vertex
		for (int i = 0; i < length; i++) {

			// get latitude and longitude of vertex
			double lat2 = vertexList.get(i).getVertexData().getLatitude();
			double lon2 = vertexList.get(i).getVertexData().getLongitude();
			double distance = calculateDistanceUtil(lat1, lon1, lat2, lon2);

			// get closest road
			if (distance < minDistance) {
				if (graph.getNeighbors(vertexList.get(i).getVertexName()).size() > 0) {
					minDistance = distance;
					closestRoad = vertexList.get(i).getVertexName();
				}
			}
		}

		return closestRoad;
	}

	/**
	 * Method to find the shortest route and compute the distance between two 
	 * latitude/longitude locations. Uses the closest road method to locate vertices, 
	 * and then uses Dijkstra's algorithm for shortest path.
	 */
	public List<String> ShortestRoute(Location fromLocation, Location toLocation) {
		String vertexStart = ClosestRoad(fromLocation);
		String vertexEnd = ClosestRoad(toLocation);

		// use GraphAlgorithms to calculate shortest path
		List<Edge<EdgeData>> edgePath = GraphAlgorithms.ShortestPath(graph, vertexStart, vertexEnd);
		List<String> shortestRoute = new ArrayList<String>();
		double distance=0;
		int length = edgePath.size();

		// add first vertex in each edge, compute the total distance of the shortest path
		for (int i = 0; i < length; i++) {
			shortestRoute.add(edgePath.get(i).getVertexName1());
			distance += edgePath.get(i).getEdgeData().getWeight();
		}

		// add final vertex
		shortestRoute.add(vertexEnd);
		System.out.println("The total distance of the route is:");
		System.out.println(distance + " miles");
		return shortestRoute;
	}

	/**
	 * Method to convert a list of vertices into a list of street names.
	 */
	public List<String> StreetRoute(List<String> vertexList) {
		List<String> streetRoute = new ArrayList<String>();
		int length = vertexList.size();

		// set current street as first edge using first two vertices in list
		String currentStreet = graph.getEdge(vertexList.get(0), vertexList.get(1)).getEdgeData().getStreetName();

		// iterate through list of vertices, add street name any time next street is different than current street
		for (int i = 0; i < length - 2; i++) {
			String nextStreet = graph.getEdge(vertexList.get(i), vertexList.get(i + 1)).getEdgeData().getStreetName();
			if (!currentStreet.equals(nextStreet)) {
				streetRoute.add(currentStreet);
				currentStreet = nextStreet;
			}
		}

		// add final street name
		if (!streetRoute.get(streetRoute.size() - 1).equals(currentStreet)) {
			streetRoute.add(currentStreet);
		}

		return streetRoute;
	}

	// inner class for vertex data
	public class VertexData {

		private double latitude;
		private double longitude;

		public VertexData(double lat, double lon) {
			this.latitude = lat;
			this.longitude = lon;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}
	}

	// inner class for edge data
	public class EdgeData implements WeightInterface {

		private String streetName;
		private double distance;

		public EdgeData(String name, double dist) {
			this.streetName = name;
			this.distance = dist;
		}

		public String getStreetName() {
			return streetName;
		}

		@Override
		public double getWeight() {
			return distance;
		}

	}

	// inner class for location
	public static class Location {

		private double latitude;
		private double longitude;

		public Location(double lat, double lon) {
			this.latitude = lat;
			this.longitude = lon;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}
	
	}
	
	
	public static void main(String[] args) {
		
		ShortestRoute.showShortestRoute(args);
		
	}
}

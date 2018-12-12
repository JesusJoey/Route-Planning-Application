// @author Yang Qiao, Haoyu Xu, Jianqing Gao, Yuqiang Ning

package ec504.finalProject.shortestRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ec504.finalProject.graph.GraphInterface;
import ec504.finalProject.graph.WeightInterface;
import ec504.finalProject.graph.GraphInterface.Edge;
import ec504.finalProject.graph.GraphInterface.Vertex;



public class GraphAlgorithms {
	
	
	/**
	 * This method uses Dijkstra's algorithm to return a list of edges
	 * representing the shortest path between two vertices in a graph.
	 */
	public static <V, E extends WeightInterface> List<Edge<E>> ShortestPath(GraphInterface<V, E> g, String vertexStart, String vertexEnd) {

		// check for null inputs
		if (g == null || vertexStart == null || vertexEnd == null) {
			throw new NullPointerException();
		}

		// instantiate maps for distance and predecessor
		HashMap<String, Double> dist = new HashMap<String, Double>();
		HashMap<String, String> pred = new HashMap<String, String>();

		// instantiate lists for open and closed sets
		ArrayList<String> open = new ArrayList<String>();
		ArrayList<String> closed = new ArrayList<String>();

		// add all vertices as max distance and null predecessor
		for (Vertex<V> vertex : g.getVertices()) {
			dist.put(vertex.getVertexName(), Double.MAX_VALUE);
			pred.put(vertex.getVertexName(), null);
		}
		dist.put(vertexStart, 0.0);
		open.add(vertexStart);

		// while open set is not empty
		while (!open.isEmpty()) {

			// a = x such that x is in open and x has minimum cost
			String a = getMin(open, dist);
			closed.add(a);
			open.remove(a);

			// for each neighbor of a
			for (Vertex<V> v : g.getNeighbors(a)) {
				if (!closed.contains(v)) {
					Double alt = dist.get(a) + g.getEdge(a, v.getVertexName()).getEdgeData().getWeight();
					if (alt < dist.get(v.getVertexName())) {
						dist.put(v.getVertexName(), alt);
						open.add(v.getVertexName());
						pred.put(v.getVertexName(), a);
					}
				}
			}
		}

		// use predecessor list to build path
		ArrayList<Edge<E>> path = new ArrayList<Edge<E>>();
		String currentVertex = vertexEnd;
		while (pred.get(currentVertex) != null) {
			path.add(g.getEdge(pred.get(currentVertex), currentVertex));
			currentVertex = pred.get(currentVertex);
		}

		// reverse path and return
		Collections.reverse(path);
		return path;
	}

	// helper method to find vertex in open set with minimum cost
	private static String getMin(ArrayList<String> open, HashMap<String, Double> dist) {
		Double minDistance = Double.MAX_VALUE;
		String minVertex = null;

		// check each vertex in open set
		for (String vertex : open) {
			if (dist.get(vertex) < minDistance) {
				minVertex = vertex;
				minDistance = dist.get(vertex);
			}
		}

		if (minVertex == null) {
			minVertex = open.get(0);
		}

		return minVertex;
	}


}

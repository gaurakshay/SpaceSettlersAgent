package gaur4004;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * Finds the A* Path between two points in the world.
 *
 */
public class AStarPathFinder {
	
	Grid grid;
	Toroidal2DPhysics space;
	double diagonalDist;
	double straightDist;
	
	/**
	 * Constructor to initialize values.
	 * @param _grid
	 * @param _space
	 */
	AStarPathFinder(Grid _grid, Toroidal2DPhysics _space){
		this.grid = _grid;
		this.space = _space;
		diagonalDist = Math.sqrt(2 * (grid.getNodeSize() * grid.getNodeSize()));
		straightDist = grid.getNodeSize();
	}
	
	/**
	 * Find shortest path between starting position and target position 
	 * using A* algorithm.
	 * @param startPos
	 * @param targetPos
	 * @return
	 */
	public ArrayList<Node> findPath(Position startPos, Position targetPos) {
		
		
		// Reset the different node parameters from previous search.
		for (Node node : grid.getGrid()) {
			node.setgCost(Double.MAX_VALUE);
			node.sethCost(Double.MAX_VALUE);
			node.setPartOfPath(false);
			node.setColor(null);
			node.setParentNode(null);
		}
		
		// Find the starting node and the terminating node.
		Node startNode = grid.getNodeFromLocation(startPos);
		startNode.setColor(new Color(255, 255, 0, 80));;
		startNode.setgCost(0);
		Node targetNode = grid.getNodeFromLocation(targetPos);
		targetNode.setColor(new Color(0, 0, 255, 80));
		startNode.sethCost(this.getDistance(startNode, targetNode));
		
		// Variable that will contain the nodes of the path.
		ArrayList<Node> path = null;
		
		// If the ship and object are very close then they might be
		// in the same node.
		if (startNode == targetNode) {
			path = new ArrayList<Node>();
			path.add(startNode);
			return path;
		}
		
		List<Node> openSet = new ArrayList<Node>();
		List<Node> closedSet = new ArrayList<Node>();
		
		openSet.add(startNode);
		
		// A* implementation.
		while (!openSet.isEmpty()) {
			Node currentNode = openSet.get(0);
			for (int i = 0; i < openSet.size(); i++) {
				if((openSet.get(i).getfCost() < currentNode.getfCost()))	{
					currentNode = openSet.get(i);
				} else if (openSet.get(i).getfCost() == currentNode.getfCost()) {
						if (openSet.get(i).gethCost() < currentNode.gethCost()) {
							currentNode = openSet.get(i);
						}
				}
			}
			
			openSet.remove(currentNode);
			closedSet.add(currentNode);
			
			if(currentNode == targetNode) {
				path = this.retracePath(startNode, targetNode);
				return path;
			}
			
			List<Node> neighbors = grid.getNeighbors(currentNode);
			
			for(Node neighbor : neighbors) {
				if(neighbor != null) {
					if(neighbor != targetNode) {
						if(neighbor != null) {
							if(neighbor.isOccupied() || closedSet.contains(neighbor)) {
									continue;
							}
						}
					}
					
					double newCostNeighbor = currentNode.getgCost() + this.getDistance(currentNode, neighbor);
					if ((newCostNeighbor < neighbor.getgCost()) || (!openSet.contains(neighbor))) {
						neighbor.setgCost(newCostNeighbor);
						neighbor.sethCost(this.getDistance(neighbor, targetNode));
						neighbor.setParentNode(currentNode);
						if(!openSet.contains(neighbor)) {
							openSet.add(neighbor);
						}
					}
				}
			}
		}
		return path;
	}
	
	
	/**
	 * Retrace the nodes that were traversed from source node to target
	 * node.
	 * @param startNode
	 * @param targetNode
	 * @return
	 */
	private ArrayList<Node> retracePath(Node startNode, Node targetNode) {
		ArrayList<Node> path = new ArrayList<Node>();
		Node currentNode = targetNode;
		
		while(currentNode != startNode) {
			currentNode.setPartOfPath(true);
			path.add(currentNode);
			currentNode = currentNode.getParentNode();
		}
		
		Collections.reverse(path);
		
		return path;
	}
	
	/**
	 * Get distance between two nodes.
	 * @param a
	 * @param b
	 * @return
	 */
	private double getDistance(Node a, Node b) {
		double distX = Math.abs(a.getX() - b.getX());
		double distY = Math.abs(a.getY() - b.getY());
		if (distX > distY) {
			return (diagonalDist * distY + straightDist * (distX - distY));
		} else {
			return (diagonalDist * distX + straightDist * (distY - distX));
		}
	}

}

package gaur4004;

import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * This class stores Nodes as a ArrayList.
 *
 */
public class Grid{

	// total width, height, nodesize of the grid in pixels
	// also the team name and whether to set the nodes
	// containing the home base to occupied or not.
	private int totalWidth;
	private int totalHeight;
	private int nodeSize;
	private String teamName;
	private boolean setHomeBaseAsOccupied;

	// List of nodes that will form a part of the grid.
	private List<Node> grid;
	
	// Constructor to initialize values.
	Grid(int _totalWidth, int _totalHeight, int _nodeSize, Toroidal2DPhysics _space, String _teamName) {
		totalWidth = _totalWidth;
		totalHeight = _totalHeight;
		nodeSize = _nodeSize;
		teamName = _teamName;
		setHomeBaseAsOccupied = false;
		grid = new ArrayList<Node>();
		generateGrid();
	}
	
	/**
	 * Generate the nodes that will form the grid.
	 */
	private void generateGrid() {
		int nodeAlongXAxis = (int)(totalWidth / nodeSize);
		int nodeAlongYAxis = (int)(totalHeight / nodeSize);
		for (int i = 0; i < nodeAlongXAxis; i++) {
			int xPos = i * nodeSize;
			for (int j = 0; j < nodeAlongYAxis; j++) {
				int yPos = j * nodeSize;
				grid.add(new Node(nodeSize, xPos, yPos));
			}
		}
	}
	
	// Getter for grid.
	public List<Node> getGrid() {
		return grid;
	}

	/**
	 * Method to update whether a node is occupied or not.
	 * A node is occupied if it contains a non-mineable asteroid or a base.
	 * Sets the node containing the object position and neighboring nodes
	 * in a 3X3 grid as occupied.
	 * @param space
	 * @param currentTargets 
	 */
	public void updateGridByNeighbor(Toroidal2DPhysics space){
		for(Node node : grid) {
			node.setOccupied(false);
		}
		
		// Update the occupied nodes.
		Set<Asteroid> asteroids = space.getAsteroids();
		Set<Base> bases = space.getBases();
		for (Asteroid asteroid : asteroids) {
			if(!asteroid.isMineable()) {
				Node asteroidNode = this.getNodeFromLocation(asteroid.getPosition());
				if(asteroidNode != null) {
					List<Node> asteroidNeighbor = this.getNeighbors(asteroidNode);
					for(Node node : asteroidNeighbor) {
						if (node != null) {
							node.setOccupied(true);
						}
					}
				}
			}
		}
		for (Base base : bases) {
			Node baseNode = this.getNodeFromLocation(base.getPosition());
			if (baseNode != null) {
				List<Node> baseNeighbor = this.getNeighbors(baseNode);
				for(Node node : baseNeighbor) {
					if(node != null) {
						node.setOccupied(true);
					}
				}
			}
		}
		
	}
	
	
	/**
	 * Method to update whether a node is occupied or not.
	 * A node is occupied if it contains a non-mineable asteroid or a base.
	 * Nodes are set to occupied only if the shape of the object intersects
	 * with the node.
	 * @param space
	 * @param currentTargets 
	 */
	public void updateGridByShape(Toroidal2DPhysics space){
		for(Node node : grid) {
			node.setOccupied(false);
		}
		
		// Update the occupied nodes.
		Set<Asteroid> asteroids = space.getAsteroids();
		Set<Base> bases = space.getBases();
		Set<Ship> ships = space.getShips(); 
		for (Asteroid asteroid : asteroids) {
			if(!asteroid.isMineable()) {
				Node asteroidNode = this.getNodeFromLocation(asteroid.getPosition());
				if(asteroidNode != null) {
					asteroidNode.setOccupied(true);
					Ellipse2D.Double asteroidShape = getShapeofObject(asteroid);
					List<Node> asteroidNeighbor = this.getNeighbors(asteroidNode);
					for(Node node : asteroidNeighbor) {
						if (node != null) {
							if(asteroidShape.intersects(node.getRect())){
								node.setOccupied(true);
							}
						}
					}
				}
			}
		}
		for (Base base : bases) {
			if (!base.getTeamName().equalsIgnoreCase(teamName)) {
				// if the base is not homebase.
				Node baseNode = this.getNodeFromLocation(base.getPosition());
				if (baseNode != null) {
					baseNode.setOccupied(true);
					Ellipse2D.Double baseShape = getShapeofObject(base);
					List<Node> baseNeighbor = this.getNeighbors(baseNode);
					for(Node node : baseNeighbor) {
						if(node != null) {
							if(baseShape.intersects(node.getRect())){
								node.setOccupied(true);
							}
						}
					}
				}
			} else {
				// if the base is the homebase:
				if(this.isSetHomeBaseAsOccupied()) {
					Node baseNode = this.getNodeFromLocation(base.getPosition());
					if (baseNode != null) {
						baseNode.setOccupied(true);
						Ellipse2D.Double baseShape = getShapeofObject(base);
						List<Node> baseNeighbor = this.getNeighbors(baseNode);
						for(Node node : baseNeighbor) {
							if(node != null) {
								if(baseShape.intersects(node.getRect())){
									node.setOccupied(true);
								}
							}
						}
					}
				}
			}
		}
		
		for (Ship ship : ships) {
			if(!ship.getTeamName().equalsIgnoreCase(teamName)) {
				Node shipNode = this.getNodeFromLocation(ship.getPosition());
				if(shipNode != null) {
					shipNode.setOccupied(true);
					Ellipse2D.Double shipShape = getShapeofObject(ship);
					List<Node> shipNeighbor = this.getNeighbors(shipNode);
					for(Node node : shipNeighbor) {
						if (node != null) {
							if(shipShape.intersects(node.getRect())){
								node.setOccupied(true);
							}
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Get the node that a position is located at.
	 */
	public Node getNodeFromLocation(Position position) {
		// Probably not very efficient because basic math can do this.
		for(Node node : grid) {
			if(node.contains(position.getX(), position.getY())) {
				return node;
			}
		}
		return null;
	}
	
	/**
	 * Get the node that a position is located at.
	 */
	public Node getNodeFromLocation(double x, double y) {
		// Probably not very efficient because basic math can do this.
		for(Node node : grid) {
			if(node.contains(x, y)) {
				return node;
			}
		}
		return null;
	}
	
	
	/**
	 * Returns the immediate neighbors of the source node in a 3x3 grid layout
	 * with the source node at center.
	 * @param node
	 * @return
	 */
	public List<Node> getNeighbors(Node node) {
		List<Node> neighbors = new ArrayList<Node>();
		for(int x = -1 * nodeSize; x < 2 * nodeSize; x += nodeSize) {
			for (int y = -1 * nodeSize; y < 2 * nodeSize; y += nodeSize) {
				if (x == 0 && y == 0) {
					continue;
				}
				double neighborX = node.getX() + x;
				double neighborY = node.getY() + y;
				
				if (neighborX < 0) {
					neighborX = this.totalWidth + neighborX;
				} else if (neighborX > this.totalWidth) {
					neighborX = neighborX - this.totalWidth;
				}
				if (neighborY < 0) {
					neighborY = this.totalHeight + neighborY;
				} else if (neighborY > this.totalHeight) {
					neighborY = neighborY - this.totalHeight;
				}
				
				neighbors.add(this.getNodeFromLocation(neighborX, neighborY));
			}
		}
		return neighbors;
	}
	
	// Getter for nodesize.
	public int getNodeSize() {
		return this.nodeSize;
	}
	
	/**
	 * Generates the shape of the object based on its location and radius.
	 * @param obj
	 * @return
	 */
	public Ellipse2D.Double getShapeofObject(AbstractObject obj){
		double objRad = obj.getRadius();
		double objX = obj.getPosition().getX() - objRad;
		double objY = obj.getPosition().getY() - objRad;
		double objSize = 2 * objRad;
		Ellipse2D.Double baseShape = new Ellipse2D.Double(objX, objY, objSize, objSize);
		return baseShape;
	}
	
	
	public boolean isSetHomeBaseAsOccupied() {
		return setHomeBaseAsOccupied;
	}

	public void setSetHomeBaseAsOccupied(boolean setHomeBaseAsOccupied) {
		this.setHomeBaseAsOccupied = setHomeBaseAsOccupied;
	}
}

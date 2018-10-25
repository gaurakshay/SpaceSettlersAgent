package gaur4004;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class BanweerGaurGAAgent extends TeamClient {
	
	// Class variables for the grid and path finder classes.
	// Path finding algorithms available are:
	// A* Search, DFS and Hill Climbing.
	private Grid grid;
	private AStarPathFinder aspf;
	
	// Update intervals in time steps.
	private final int GRID_UPDATE_INTERVAL = 10;
	
	// Size of each node in the grid (pixels).
	private final int NODESIZE = 40;
	double diagonalDist;
	double straightDist;
	
	// Ship and the target that it is pursuing. Target can be:
	// a. Mineable asteroid,
	// b. Beacon or 
	// c. Base.
	// First ID is that of the ship and the second is that of the target.
	// Target node refers to the node in which the target resides.
	Map<Ship, AbstractObject> targetIDMap;
	Map<Ship, Node> targetNodeMap;
	
	// Store the paths currently being followed for each target.
	Map<Ship, ArrayList<Node>> targetPathMap;
	
	// Store the current action associated with the ship.
	// Also store the current node that the ship is moving to.
	Map<Ship, AbstractAction> currentActionMap;
	Map<Ship, Node> currentNodeMap;
	
	
	
	// Genetic Algorithm related variables.
	Individual currentIndividual;
	Population population;
	GeneticAlgorithm ga;
	State state;
	
	
	// Individual update interval.
	int individualUpdateInterval = 2500;
	
	
	boolean graphicsOn = true;
	
	
	
	/*********************************************************************
	 * Overridden Methods.
	 *********************************************************************/
	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		int currentTimestep = space.getCurrentTimestep();
		HashMap<UUID, AbstractAction> action = new HashMap<UUID, AbstractAction>();		
		
		// Update the grid nodes every 10 time steps to update the nodes that are
		// occupied and can't be traversed.
		if (currentTimestep % GRID_UPDATE_INTERVAL == 0) {
			grid.updateGridByShape(space);
//			updatePathToTarget(space);
		}
		
		for( AbstractActionableObject object : actionableObjects) {
			if (object instanceof Ship) {
				AbstractAction act = getMovement(space, (Ship)object);
				action.put(object.getId(), act);
			}
		}
		
		return action;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		
//		// if the path has been traversed, set target and path to null;
//				// else check if the path has been compromised.
//				for (UUID shipID : targetPathMap.keySet()) {
//					List<Node> path = targetPathMap.get(shipID);
//					
//					// If path is null, reset target and path values for the ships.
//					if (path == null || path.size() == 0) {
////						AbstractObject targetObj = space.getObjectById(targetIDMap.get(shipID));
////						if(targetObj == null || !targetObj.isAlive()) {
//							targetIDMap.put(shipID, null);
//							targetNodeMap.put(shipID, null);
//							targetPathMap.put(shipID, null);
//							currentActionMap.put(shipID, null);
//							currentNodeMap.put(shipID, null);
////						}
////					} else {
////						// If path is not null, check if any of the nodes on the path
////						// is now an occupied node. If yes, nullify the path and also
////						// current action.
////						for(Node pathNode : path) {
////							if (pathNode.isOccupied()) {
//////										targetIDMap.put(shipID, null);
//////										targetNodeMap.put(shipID, null);
////								targetPathMap.put(shipID, null);
////								currentActionMap.put(shipID, null);
////								currentNodeMap.put(shipID, null);
////							}
////						}
//					}
//				}
//				
				// if the ship has reached the intended node, finish current action.
				for (Ship ship : currentNodeMap.keySet()) {
					Node targetNode = currentNodeMap.get(ship);
					if (targetNode.contains(ship.getPosition())) {
						System.out.println("Target node reached.");
						currentActionMap.clear();
						currentNodeMap.clear();;
					}
				}
				
				// if the current action is 
				
				// Check if the current target object exists or not. Nullify if doesn't.
				// if the target has moved, update current actions and path to null
				for(Ship ship : targetIDMap.keySet()) {
					AbstractObject targetObj = targetIDMap.get(ship);
					if(targetObj == null || !targetObj.isAlive()) {
						targetIDMap.clear();
						targetNodeMap.clear();
						currentActionMap.clear();
						currentNodeMap.clear();
						targetPathMap.clear();
					}
				}
				
				// if time is modulo 5000, update individual.
				if(space.getCurrentTimestep() % individualUpdateInterval == 0) {
					
					// Check if the generation is complete, evolve if true
					if(population.isGenerationComplete()) {
						population = ga.evolvePopulation(population);
					}
					// change the individual.
					currentIndividual = population.getNextIndividual();
				}
		
	}

	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, ResourcePile resourcesAvailable,
			PurchaseCosts purchaseCosts) {
		
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		
		boolean boughtShip = false;
		
		// Get those ships!!!
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					boughtShip = true;
					break;
				}

			}
		}
		
		if(boughtShip) {
			int noOfShip = 0;
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					noOfShip++;
				}
			}
			System.out.println("Number of ships = " + noOfShip);
		}
		
		return purchases;
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		// Initialize various state variables.
		grid = new Grid(space.getWidth(), space.getHeight(), NODESIZE, space, this.getTeamName());
		aspf = new AStarPathFinder(this.grid, space);
		
		targetIDMap = new HashMap<Ship, AbstractObject>();
		targetNodeMap = new HashMap<Ship, Node>();
		currentActionMap = new HashMap<Ship, AbstractAction>();
		currentNodeMap = new HashMap<Ship, Node>();
		targetPathMap = new HashMap<Ship, ArrayList<Node>>();
		
		diagonalDist = Math.sqrt(2 * (NODESIZE * NODESIZE));
		straightDist = NODESIZE;
		
		population = new Population(true);
		ga = new GeneticAlgorithm();
		state = new State(grid, aspf, NODESIZE);
		currentIndividual = population.getNextIndividual();
		
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		// Add graphics to draw the grid.
		if(graphicsOn) {
			HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
			graphics.addAll(grid.getGrid());
			return graphics;
		} else {
			return null;
		}
	}
	
	
	/*********************************************************************
	 * Helper Methods.
	 *********************************************************************/
	
	/**
	 * Checks current status of the system using state variables and takes
	 * appropriate actions such as assigning target, finding and assigning
	 * path to the target or assigning current action to be taken.
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getMovement(Toroidal2DPhysics space, Ship ship) {
		
		// Id of the ship that is used in various maps.
//		UUID shipID = ship.getId();
		
		
		
		////////////////////////////////////////////////////////////
		// Check if the ship has been assigned a target.
		////////////////////////////////////////////////////////////
		AbstractObject targetObject = targetIDMap.get(ship);
		Node targetNode = null;
		if(targetObject == null) {
			// If target is not assigned, assign one.
			targetObject = getTarget(space, ship);
			targetNode = grid.getNodeFromLocation(targetObject.getPosition());

			// put the target and the its node in corresponding maps.
			targetIDMap.put(ship, targetObject);
			targetNodeMap.put(ship, targetNode);
		}
		
		////////////////////////////////////////////////////////////
		// Make sure that the ship has been assigned a path to target.
		////////////////////////////////////////////////////////////
		ArrayList<Node> pathToTarget = targetPathMap.get(ship);
		if (pathToTarget == null) {
			// If path to the target is not assigned, get the path
			// and assign it to the ship.
			pathToTarget = calculatePath(space, ship, targetObject);
			System.out.println("Path being set is of length : " + pathToTarget.size());
			targetPathMap.put(ship, pathToTarget);
		}
		
		
		////////////////////////////////////////////////////////////
		// Check if the ship has current action and node.
		////////////////////////////////////////////////////////////
		AbstractAction currentAction = currentActionMap.get(ship);
		if (currentAction == null){
			// assign current action using the path assigned for the
			// target.
			if(pathToTarget == null) {
				System.out.println("Path to target is null.");
			} else {
				System.out.println("Length of path is   " + pathToTarget.size());
			}
			currentAction = getActionFromPath(space, ship, pathToTarget);
			return currentAction;
		} else {
			return currentAction;
		}
		
	}
	
	
	/**
	 * Depending on current status of the ship, assign a target of either:
	 * a. Mineable Asteroid,
	 * b. Base or
	 * c. Beacon.
	 * 
	 * @param space
	 * @param ship
	 */
	private AbstractObject getTarget(Toroidal2DPhysics space, Ship ship) {
		
		// By default, the home base will be considered as an occupied node that needs
		// to be avoided.
		grid.setSetHomeBaseAsOccupied(true);
		grid.updateGridByShape(space);
		
		int currentTime = space.getCurrentTimestep();
		int totalTime = space.getMaxTime();
		
		
		AbstractObject targetObj;
		
		// If game is about to end, return to the home base
		// with whatever resources the ship has so far.
		if (totalTime - currentTime < 300) {
			return getBase(space, ship);
		}
		
		// if the energy is low, locate the nearest energy provider
		// which could be a beacon or a base.
		if (ship.getEnergy() < 2000) {
			targetObj = getBaseOrBeacon(space, ship);
		} else {
			// if the ship has enough resources, bring it back to the nearest base.
			if (ship.getResources().getTotal() > 4000) {
				targetObj = getBase(space, ship);
			} else {
				// Otherwise, target an asteroid.
				targetObj = getAsteroid(space, ship);
			}
		}
		
		return targetObj;
	}
	
	
private AbstractObject getBaseOrBeacon(Toroidal2DPhysics space, Ship ship) {
		
		Node shipNode = grid.getNodeFromLocation(ship.getPosition());
		
		// Find nearest base.
		Set<Base> bases = space.getBases();
		double nearestBaseDist = Double.MAX_VALUE;
		Base nearestBase = null;
		for (Base base : bases) {
			if(base.getTeamName().equalsIgnoreCase(this.getTeamName())) {
				Node baseNode = grid.getNodeFromLocation(base.getPosition());
				double dist = this.getDistance(shipNode, baseNode);
				if (dist < nearestBaseDist) {
					nearestBaseDist = dist;
					nearestBase = base;
				}
			}
		}
		
		// Find nearest beacon.
		Set<Beacon> beacons = space.getBeacons();
		double nearestBeaconDist = Double.MAX_VALUE;
		Beacon nearestBeacon = null;
		for (Beacon beacon : beacons) {
			Node beaconNode = grid.getNodeFromLocation(beacon.getPosition());
			double dist = this.getDistance(shipNode, beaconNode);
			if (dist < nearestBeaconDist) {
				nearestBeaconDist = dist;
				nearestBeacon = beacon;
			}
		}
		
		// Finally, assign the one that is nearest.
		if(nearestBaseDist < nearestBeaconDist) {
			if(nearestBase.getEnergy() > 1000) {
				// If the target is being set as a base, set the home base
				// as not occupied.
				grid.setSetHomeBaseAsOccupied(false);
				grid.updateGridByShape(space);
				return nearestBase;
			} else {
				return nearestBeacon;
			}
		} else {
			return nearestBeacon;
		}
	}
	
	private AbstractObject getBase(Toroidal2DPhysics space, Ship ship) {
		// Find nearest base.
		Set<Base> bases = space.getBases();
		double nearestBaseDist = Double.MAX_VALUE;
		Base nearestBase = null;
		Node shipNode = grid.getNodeFromLocation(ship.getPosition());
		for (Base base : bases) {
			if(base.getTeamName().equalsIgnoreCase(this.getTeamName())) {
				Node baseNode = grid.getNodeFromLocation(base.getPosition());
				double dist = this.getDistance(shipNode, baseNode);
//				double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
				if (dist < nearestBaseDist) {
					nearestBaseDist = dist;
					nearestBase = base;
				}
			}
		}
		// If the target is being set as a base, set the home base
		// as not occupied.
		grid.setSetHomeBaseAsOccupied(false);
		grid.updateGridByShape(space);
		return nearestBase;
	}
	
	private AbstractObject getAsteroid(Toroidal2DPhysics space, Ship ship) {
		
		// Get the individual to find the asteroid.
		state.refresh(space, ship);
		Asteroid bestAsteroid = currentIndividual.targetAsteroid(state);
		
//		if(bestAsteroid == null) {
//			System.out.println("Best asteroid is null.");
//		}
//		if(bestAsteroid.isAlive()) {
//			System.out.println("Asteroid is alive.");
//		}
		
		return bestAsteroid;
		
		
		
//		Asteroid nearestAsteroid = null;
//		Set<Asteroid> asteroids = space.getAsteroids();
//		double minDistance = Double.MAX_VALUE;
//		Node shipNode = grid.getNodeFromLocation(ship.getPosition());
//		for(Asteroid asteroid : asteroids) {
//			// Make sure that the asteroid is worth it.
//			if(asteroid.isMineable() && asteroid.getResources().getTotal() > 300){
//				Node asteroidNode = grid.getNodeFromLocation(asteroid.getPosition());
////				double dist = space.findShortestDistance(asteroid.getPosition(), ship.getPosition());
//				double dist = this.getDistance(shipNode, asteroidNode);
//				if(dist < minDistance) {
//					nearestAsteroid = asteroid;
//					minDistance = dist;
//				}
//			}
//		}
//
//		return nearestAsteroid;
	}
	
	
	/**
	 * Calls path finding method in the class that implements different search
	 * algorithms. Path will be searched between the ship and the targetObj.
	 * 
	 * @param space
	 * @param ship
	 * @param targetObj
	 * @return
	 */
	private ArrayList<Node> calculatePath(Toroidal2DPhysics space, Ship ship, AbstractObject targetObj) {
		
		ArrayList<Node> path = new ArrayList<Node>();
		if (targetObj != null && targetObj.isAlive()) {
			System.out.println("FINDING THE PATHHHHHHHH");
			path = aspf.findPath(ship.getPosition(), targetObj.getPosition());
		}
		
		System.out.println("Path length is :" + path.size());
		
		return path;
	}
	
	
	/**
	 * Gets first node in the path and creates action to move to the
	 * center of that node.
	 * Also updates ship's current action and current node that it will target.
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getActionFromPath(Toroidal2DPhysics space, Ship ship, List<Node> targetPath) {
		MoveAction ma = null;
		// Set target as the first node in the path.
		// The actual position is set as the middle of the node
		// where the node is actually a square.
		if(targetPath.size() > 0) {
			Node currentTargetNode = targetPath.get(0);
			Position currentTargetPos = new Position((currentTargetNode.getX() + this.NODESIZE/2), (currentTargetNode.getY() + this.NODESIZE/2));
			
			// Create the action to go the node.
			ma = new MoveAction(space, ship.getPosition(), currentTargetPos);
			ma.setKpTranslational(1.0);
			ma.setKvTranslational(1.0);
			
			// store this node.
			currentNodeMap.put(ship, currentTargetNode);
			// since this is the action that the ship is going to continue to take
			// until finish, cache this until this movement ends.
			currentActionMap.put(ship, ma);
			
			// remove the target node so that we can move to the next node when
			// next movement for the ship is called for.
			targetPath.remove(currentTargetNode);
		} else {
			// set the action to the target.
			AbstractObject target = targetIDMap.get(ship);
			ma = new MoveAction(space, ship.getPosition(), target.getPosition());
			ma.setKpTranslational(1.0);
			ma.setKvTranslational(1.0);
		}
		
		return ma;
	}
	
	
	/**
	 * Gets the distance between two nodes in grid.
	 * 
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
	
	
	
	/**
	 * Method to update the path to the current target of the ship every
	 * specified timestep.
	 * @param space
	 */
	private void updatePathToTarget(Toroidal2DPhysics space) {
		for(Ship ship : this.targetIDMap.keySet()) {
			ArrayList<Node> path = calculatePath(space, ship, targetIDMap.get(ship));
//			if(path != null && path.size() > 40) {
//				this.targetIDMap.put(id, null);
//				this.targetNodeMap.put(id, null);
//				this.targetPathMap.put(id, null);
//				this.currentActionMap.put(id, null);
//				this.currentNodeMap.put(id, null);
//				return;
//			}
			this.targetPathMap.put(ship, path);
		}
	}
	
	
}

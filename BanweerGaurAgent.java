package gaur4004;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
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

public class BanweerGaurAgent extends TeamClient{
	
	// Class variables for the grid and path finder classes.
	// Path finding algorithms available are:
	// A* Search, DFS and Hill Climbing.
	private Grid grid;
	private AStarPathFinder aspf;
	
	// Update intervals in time steps.
	private final int GRID_UPDATE_INTERVAL = 10;
	
	// Size of each node in the grid (pixels).
	private final int NODESIZE = 40;
	private double diagonalDist;
	private double straightDist;
	
	// Ship and the target that it is pursuing. Target can be:
	// a. Mineable asteroid,
	// b. Beacon or 
	// c. Base.
	// First ID is that of the ship and the second is that of the target.
	// Target node refers to the node in which the target resides.
	private Map<UUID, UUID> targetIDMap;
	private Map<UUID, Node> targetNodeMap;
	
	// Store the paths currently being followed for each target.
	private Map<UUID, List<Node>> targetPathMap;
	
	
	// Store the current action associated with the ship.
	// Also store the current node that the ship is moving to.
	private Map<UUID, AbstractAction> currentActionMap;
	private Map<UUID, Node> currentNodeMap;
	
	// Genetic Algorithm related variables.
	private Individual currentIndividual;
	private Population population;
	private GeneticAlgorithm ga;
	private State state;
	
	
	// Individual update interval.
	final int individualUpdateInterval = 5000;
	
	final boolean graphicsOn = true;
	
	final String knowlegeFilePath = "gaur4004GA/knowledge.ser";
	
	
	@Override
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		
		int currentTimestep = space.getCurrentTimestep();
		HashMap<UUID, AbstractAction> action = new HashMap<UUID, AbstractAction>();		
		
		// Update the grid nodes every 10 time steps to update the nodes that are
		// occupied and can't be traversed.
		if (currentTimestep % GRID_UPDATE_INTERVAL == 0) {
			grid.updateGridByShape(space);
			updatePathToTarget(space);
		}
		
		// Get action for every ship.
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
		
		// if the ship has reached the intended node, finish current action.
		for (UUID shipID : currentNodeMap.keySet()) {
			Node targetNode = currentNodeMap.get(shipID);
			if (targetNode != null) {
				if (targetNode.contains(space.getObjectById(shipID).getPosition())) {
					currentActionMap.put(shipID, null);
					currentNodeMap.put(shipID, null);
				}
			}
			// or if the current action is complete, remove it as well.
			AbstractAction aa = currentActionMap.get(shipID);
			if(aa == null || aa.isMovementFinished(space)) {
				currentActionMap.put(shipID, null);
				currentNodeMap.put(shipID, null);
			}
		}
		
		// Check if the current target object exists or not. Nullify if doesn't.
		// if the target has moved, update current actions and path to null
		for(UUID shipID : targetIDMap.keySet()) {
			AbstractObject targetObj = space.getObjectById(targetIDMap.get(shipID));
			if(targetObj == null || !targetObj.isAlive()) {
				targetIDMap.put(shipID, null);
				targetNodeMap.put(shipID, null);
				currentActionMap.put(shipID, null);
				currentNodeMap.put(shipID, null);
				targetPathMap.put(shipID, null);
			} else if (!targetNodeMap.get(shipID).contains(targetObj.getPosition())) {
				currentActionMap.put(shipID, null);
				currentNodeMap.put(shipID, null);
				targetPathMap.put(shipID, null);
			}
		}
		
		if(space.getCurrentTimestep() % individualUpdateInterval == 0) {
			System.out.println(space.getCurrentTimestep());
			// Check if the generation is complete, evolve if true
			if(population.isGenerationComplete()) {
				System.out.println("Evolving population.");
				population = ga.evolvePopulation(population);
			}
			// change the individual.
			System.out.println("Current individual's fitness : " + currentIndividual.getFitness());
			currentIndividual = population.getNextIndividual();
			System.out.println("Individual changed.");
		}
		
	}

	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		return null;
	}

	@Override
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, ResourcePile resourcesAvailable,
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		boolean bought_base = false;

		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					purchases.put(ship.getId(), PurchaseTypes.BASE);
					bought_base = true;
					break;
				}
			}		
		} 
		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable) && bought_base == false) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}

			}

		}
		return purchases;
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		
		// Initialize various state variables.
		
		grid = new Grid(space.getWidth(), space.getHeight(), NODESIZE, space, this.getTeamName());
		aspf = new AStarPathFinder(this.grid, space);
//		hcpf = new HillClimbingPathFinder(this.grid, space);
		
		targetIDMap = new HashMap<UUID, UUID>();
		targetNodeMap = new HashMap<UUID, Node>();
		currentActionMap = new HashMap<UUID, AbstractAction>();
		currentNodeMap = new HashMap<UUID, Node>();
		targetPathMap = new HashMap<UUID, List<Node>>();
		
		diagonalDist = Math.sqrt(2 * (NODESIZE * NODESIZE));
		straightDist = NODESIZE;
		
		// For the population, check if it exists. If exists, load that.
		// If doesn't exists, start from scratch.
		XStream xstream = new XStream();
		xstream.alias("BanweerGaurPopulation", BanweerGaurAgent.class);

		// try to load the population from the existing saved file.  If that failes, start from scratch
		try { 
			population = (Population) xstream.fromXML(new File(knowlegeFilePath));
			System.out.println("Knowledge file loaded.");
		} catch (XStreamException e) {
			// if you get an error, handle it other than a null pointer because
			// the error will happen the first time you run
			System.out.println("No existing population found - starting a new one from scratch");
			population = new Population(true);
		}
		
//		population = new Population(true);
		ga = new GeneticAlgorithm();
		state = new State(grid, aspf, NODESIZE);
		currentIndividual = population.getNextIndividual();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		XStream xstream = new XStream();
		xstream.alias("BanweerGaurPopulation", BanweerGaurAgent.class);

		try { 
			// if you want to compress the file, change FileOuputStream to a GZIPOutputStream
			xstream.toXML(population, new FileOutputStream(new File(knowlegeFilePath)));
			System.out.println("Saved knowledge file successfully.");
		} catch (XStreamException e) {
			// if you get an error, handle it somehow as it means your knowledge didn't save
			System.out.println("Can't save knowledge file in shutdown ");
			System.out.println(e.getMessage());
		} catch (FileNotFoundException e) {
			// file is missing so start from scratch (but tell the user)
			System.out.println("Can't save knowledge file in shutdown ");
			System.out.println(e.getMessage());
		}
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		if(graphicsOn) {
			// Add graphics to draw the grid.
			HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
			graphics.addAll(grid.getGrid());
			return graphics;
		}
		return null;
	}
	
	
	
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
		UUID shipID = ship.getId();
		
		if (ship.getEnergy() < 2500) {
			this.targetIDMap.put(shipID, null);
			this.targetNodeMap.put(shipID, null);
			this.targetPathMap.put(shipID, null);
			this.currentActionMap.put(shipID, null);
			this.currentNodeMap.put(shipID, null);
			AbstractObject targetObj = getBaseOrBeacon(space, ship);
			Node targetNode = grid.getNodeFromLocation(targetObj.getPosition());

			// put the target and the its node in corresponding maps.
			targetIDMap.put(shipID, targetObj.getId());
			targetNodeMap.put(shipID, targetNode);
		}
		
		
		
		
		
		////////////////////////////////////////////////////////////
		// Check if the ship has been assigned a target.
		////////////////////////////////////////////////////////////
		UUID targetObjectID = targetIDMap.get(shipID);
		if(targetObjectID == null) {
			// If target is not assigned, assign one.
			AbstractObject targetObj = getTarget(space, ship);
			
			targetObjectID = targetObj.getId();
			Node targetNode = grid.getNodeFromLocation(targetObj.getPosition());

			// put the target and the its node in corresponding maps.
			targetIDMap.put(shipID, targetObjectID);
			targetNodeMap.put(shipID, targetNode);
		}
		
		////////////////////////////////////////////////////////////
		// Check if the ship has been assigned a path to target.
		////////////////////////////////////////////////////////////
		List<Node> pathToTarget = targetPathMap.get(shipID);
		if (pathToTarget == null) {
			
			// If path to the target is not assigned, get the path
			// and assign it to the ship.
			AbstractObject targetObj = space.getObjectById(targetObjectID);
			
			pathToTarget = calculatePath(space, ship, targetObj);
			
			targetPathMap.put(shipID, pathToTarget);
		}
		
		
		////////////////////////////////////////////////////////////
		// Check if the ship has current action and node.
		////////////////////////////////////////////////////////////
		AbstractAction currentAction = currentActionMap.get(shipID);
		if (currentAction == null){
			
			// assign current action using the path assigned for the
			// target.
			currentAction = getActionFromPath(space, ship, pathToTarget, targetObjectID);
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
		if (totalTime - currentTime < 500) {
			return getBase(space, ship);
		}
		
		// if the energy is low, locate the nearest energy provider
		// which could be a beacon or a base.
		if (ship.getEnergy() < 2500) {
			targetObj = getBaseOrBeacon(space, ship);
		} else {
			// if the ship has enough resources, bring it back to the nearest base.
			if (ship.getResources().getTotal() > 2500) {
				
//				System.out.println("Going to drop the resources off innit");
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
//				double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
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
//			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
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
		Asteroid nearestAsteroid = null;
		Set<Asteroid> asteroids = space.getAsteroids();
		double minDistance = Double.MAX_VALUE;
		Node shipNode = grid.getNodeFromLocation(ship.getPosition());
		
		state.refresh(space, ship);
		Asteroid bestAsteroid = currentIndividual.targetAsteroid(state);
		
//		if(bestAsteroid == null) {
//			System.out.println("Asteroid by individual is null");
//		} else {
//			System.out.println("Resources of the asteroid are: " + bestAsteroid.getResources().getTotal());
//		}
		
		if (bestAsteroid == null) {
			System.out.println("Best asteroid is not here so picking nearest asteroid.");
			for(Asteroid asteroid : asteroids) {
				// Make sure that the asteroid is worth it.
				if(asteroid.isMineable() && asteroid.getResources().getTotal() > 300){
					Node asteroidNode = grid.getNodeFromLocation(asteroid.getPosition());
	//				double dist = space.findShortestDistance(asteroid.getPosition(), ship.getPosition());
					double dist = this.getDistance(shipNode, asteroidNode);
					if(dist < minDistance) {
						nearestAsteroid = asteroid;
						minDistance = dist;
					}
				}
			}
			bestAsteroid = nearestAsteroid;
		}
//		System.out.println("Getting an asteroid.");
		return bestAsteroid;
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
	private List<Node> calculatePath(Toroidal2DPhysics space, Ship ship, AbstractObject targetObj) {
		
		List<Node> path = new ArrayList<Node>();
		if (targetObj != null && targetObj.isAlive()) {
			path = aspf.findPath(ship.getPosition(), targetObj.getPosition());
		} else {
			path.add(grid.getNodeFromLocation(ship.getPosition()));
		}
		
		return path;
	}
	
	/**
	 * Calls path finding method in the class that implements different search
	 * algorithms. Path will be searched between the ship and the targetObj.
	 * 
	 * @param space
	 * @param ship
	 * @param targetNode
	 * @return
	 */
//	private List<Node> calculatePath(Toroidal2DPhysics space, Ship ship, Node targetNode) {
//		List<Node> path = new ArrayList<Node>();
//		if (targetNode != null) {
//			path = aspf.findPath(ship.getPosition(), new Position(targetNode.getX(), targetNode.getY()));
//	//		List<Node> path = hcpf.findPath(ship.getPosition(), targetObj.getPosition());
//		} else {
//			path.add(grid.getNodeFromLocation(ship.getPosition()));
//		}
//		return path;
//	}
	
	/**
	 * Gets first node in the path and creates action to move to the
	 * center of that node.
	 * Also updates ship's current action and current node that it will target.
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getActionFromPath(Toroidal2DPhysics space, Ship ship, List<Node> targetPath, UUID targetObjectID) {
		
		// Set target as the first node in the path.
		// The actual position is set as the middle of the node
		// where the node is actually a square.
		
		AbstractAction aa = new DoNothingAction();
		
		if(targetPath.size() > 1) {
			Node currentTargetNode = targetPath.get(0);
			Position currentTargetPos = new Position((currentTargetNode.getX() + this.NODESIZE/2), (currentTargetNode.getY() + this.NODESIZE/2));
			
			// Create the action to go the node.
			MoveAction ma = new MoveAction(space, ship.getPosition(), currentTargetPos);
			ma.setKpTranslational(1.0);
			ma.setKvTranslational(1.0);
			
			aa = ma;
			
			// store this node.
			currentNodeMap.put(ship.getId(), currentTargetNode);
			// since this is the action that the ship is going to continue to take
			// until finish, cache this until this movement ends.
			currentActionMap.put(ship.getId(), ma);
			
			// remove the target node so that we can move to the next node when
			// next movement for the ship is called for.
			targetPath.remove(currentTargetNode);
		} else {
//			System.out.println("Path is finished but the target has not.");
			// current target:
			AbstractObject target = space.getObjectById(targetObjectID);
			// if it is not base, it means that we haven't reached the beacon or
			// the asteroid which we have set as a target right now.
			if(!(target instanceof Base)) {
				// Create the action to go the final target.
				MoveAction ma = new MoveAction(space, ship.getPosition(), target.getPosition());
				ma.setKpTranslational(1.0);
				ma.setKvTranslational(1.0);
				
				aa = ma;
				
				// store this node.
				currentNodeMap.put(ship.getId(), grid.getNodeFromLocation(target.getPosition()));
				// since this is the action that the ship is going to continue to take
				// until finish, cache this until this movement ends.
				currentActionMap.put(ship.getId(), ma);
			} else {
//				System.out.println("Target is a base so, resetting the target.");
				// The current target is base which means that once path is
				// finished, we need to reset our target.
				targetIDMap.put(ship.getId(), null);
				targetNodeMap.put(ship.getId(), null);
				currentActionMap.put(ship.getId(), null);
				currentNodeMap.put(ship.getId(), null);
				targetPathMap.put(ship.getId(), null);
			}
		}
		
		return aa;
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
		for(UUID shipID : this.targetIDMap.keySet()) {
			List<Node> path = calculatePath(space, (Ship)space.getObjectById(shipID), space.getObjectById(targetIDMap.get(shipID)));
			if(path != null && path.size() > 25) {
				this.targetIDMap.put(shipID, null);
				this.targetNodeMap.put(shipID, null);
				this.targetPathMap.put(shipID, null);
				this.currentActionMap.put(shipID, null);
				this.currentNodeMap.put(shipID, null);
				return;
			}
			this.targetPathMap.put(shipID, path);
		}
	}
	
}

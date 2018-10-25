package gaur4004;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class State {
	
//	Ship ship;
	Grid grid;
	double diagonalDist;
	double straightDist;
	AStarPathFinder aspf;
	Set<Asteroid> asteroids;
	ArrayList<Asteroid> movingAsteroids;
	ArrayList<Asteroid> stationaryAsteroids;
	HashMap<UUID, Double> ratioMap;
	HashMap<UUID, ArrayList<Node>> asteroidPathMap;
//	HashMap<UUID, Double> distanceMap;
	
	
	State(Grid _grid, AStarPathFinder _aspf, int nodeSize) {
		grid = _grid;
		aspf = _aspf;
		diagonalDist = Math.sqrt(2 * (nodeSize * nodeSize));
		straightDist = nodeSize;
		movingAsteroids = new ArrayList<>();
		stationaryAsteroids = new ArrayList<>();
		ratioMap = new HashMap<>();
		asteroidPathMap = new HashMap<>();
		asteroids = new HashSet<>();
//		distanceMap = new HashMap<>();
	}
	
	public void refresh(Toroidal2DPhysics _space, Ship _ship) {
		movingAsteroids.clear();
		stationaryAsteroids.clear();
		ratioMap.clear();
		asteroids = _space.getAsteroids();
		createRatioMap(_ship);
	}
	
	private void createRatioMap(Ship ship) {
		for(Asteroid asteroid : asteroids) {
			if(asteroid.isMineable()) {
				if(asteroid.isMoveable()) {
					movingAsteroids.add(asteroid);
				} else {
					stationaryAsteroids.add(asteroid);
				}
				ArrayList<Node> path = aspf.findPath(ship.getPosition(), asteroid.getPosition());
				double distance = 0.0;
				if(path != null && path.size() > 0) {
					distance = path.size();
				} else {
					distance = Double.MAX_VALUE;
				}
				double ratio = (double)asteroid.getResources().getTotal() / distance;
				ratioMap.put(asteroid.getId(), ratio);
				asteroidPathMap.put(asteroid.getId(), path);
			}
		}
		
		
	}
	
	public Asteroid getAsteroidByRatio(double minRatio, double maxRatio, boolean movable) {
		Asteroid pickedAsteroid = null;
		if(movable) {
			for(Asteroid ast : movingAsteroids) {
				UUID id = ast.getId();
				double ratio = ratioMap.get(id);
				if(ratio >= maxRatio && ratio <= minRatio) {
					pickedAsteroid = ast;
					return pickedAsteroid;
				}
			}
		} else {
			for(Asteroid ast : stationaryAsteroids) {
				UUID id = ast.getId();
				double ratio = ratioMap.get(id);
				if(ratio >= maxRatio && ratio <= minRatio) {
					pickedAsteroid = ast;
					return pickedAsteroid;
				}
			}
		}
		// if picked asteroid is null that means no asteroid was available for the provided
		// criteria.
		
		
		return pickedAsteroid;
	}
	
	public HashMap<Asteroid, Double> getClosest() {
		Asteroid pickedAsteroid = null;
		double ratio = 0.0;
		int minPathLength = Integer.MAX_VALUE;
		if(pickedAsteroid == null) {
			for(Asteroid asteroid : asteroids) {
//				if(asteroid == null)
				ArrayList<Node> path = asteroidPathMap.get(asteroid.getId());
//				if(path == null)
				if(path == null) {
					continue;
				}
				int pathLength = path.size();
				if(pathLength < minPathLength) {
					minPathLength = pathLength;
					pickedAsteroid = asteroid;
					ratio = ratioMap.get(asteroid.getId());
				}
			}
		}
		HashMap<Asteroid, Double> backup = new HashMap<>();
		backup.put(pickedAsteroid, ratio);
		return backup;
	}
	
	public ArrayList<Node> getPath(UUID id){
		return asteroidPathMap.get(id);
	}
	
	public double getRatio(UUID id) {
		return ratioMap.get(id);
	}

}

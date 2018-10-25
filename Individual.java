package gaur4004;

import java.util.HashMap;
import java.util.Random;

import spacesettlers.objects.Asteroid;

public class Individual {
	
	private double maxRatio;
	private double minRatio;
	private int resourcesObtained;
	private double distanceTravelled;
	private double movingAsteroidProb; // probability to select moving asteroid.
	private int movingAsteroidCount;
	private int stationaryAsteroidCount;
	
	Random rand;
	

	Individual(double _minRatio, double _maxRatio, double _movingAsteroidProb){
		maxRatio = _maxRatio;
		minRatio = _minRatio;
		resourcesObtained = 0;
		distanceTravelled = 0;
		movingAsteroidProb = _movingAsteroidProb;
		movingAsteroidCount = 0;
		stationaryAsteroidCount = 0;
		rand = new Random();
	}
	
	
	public void resetResources() {
		resourcesObtained = 0;
		distanceTravelled = 0;
		movingAsteroidCount = 0;
		stationaryAsteroidCount = 0;
	}
	
	public int getFitness() {
		// Fitness of an individual is defined by how much resources it was
		// able to collect with the resources to distance ratio and probability
		// to select either movable or stationary asteroid it is working with.
		
		return resourcesObtained;
		
	}
	
	public Asteroid targetAsteroid(State currState) {
		// Target asteroid that is within the target ratio.
		boolean movable = false;
		if(rand.nextDouble() <= movingAsteroidProb) {
			movable = true;
		}
		Asteroid targetAsteroid = currState.getAsteroidByRatio(minRatio, maxRatio, movable);
		if(targetAsteroid == null) {
			HashMap<Asteroid, Double> backup = currState.getClosest();
			for(Asteroid asteroid : backup.keySet()) {
				targetAsteroid = asteroid;
				double newRatio = backup.get(asteroid);
				double range = maxRatio - minRatio;
				minRatio = newRatio - range/2;
				maxRatio = newRatio + range/2;
			}
		}
		double resources = targetAsteroid.getResources().getTotal();
		resourcesObtained += resources;
		if(movable) {
			movingAsteroidCount++;
		} else {
			stationaryAsteroidCount++;
		}
		
//		ArrayList<Node> pathToAsteroid = currState.getPath(targetAsteroid.getId());
		distanceTravelled =  resources  / currState.getRatio(targetAsteroid.getId());
		
		return targetAsteroid;
		
	}
	
	public void updateProbability() {
		double probabilityMoving = movingAsteroidCount / (movingAsteroidCount + stationaryAsteroidCount);
		movingAsteroidProb = probabilityMoving;
	}
	
//	public double getMovingProbability() {
//		return movingAsteroidCount / (movingAsteroidCount + stationaryAsteroidCount);
//	}
	
	public double getRatio() {
		return (double)resourcesObtained / distanceTravelled;
	}
	
	
	public void setProbability(double prob) {
		movingAsteroidProb = prob;
	}
	
	public double getProbability() {
		return movingAsteroidProb;
	}
	
//	public int getResources() {
//		return resourcesObtained;
//	}

}

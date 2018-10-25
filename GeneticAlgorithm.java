package gaur4004;

import java.util.ArrayList;

public class GeneticAlgorithm {
	
	double mutationRate = 0.015;
	int tournamentSize = 4;
	boolean saveElite = true;
	int populationSize = 20;
	
	public Population evolvePopulation(Population pop) {
		Population newPop = new Population(false);
		
		
		Individual fittest = pop.getFittestIndividual();
		System.out.println("Fittest individual fitness : " + fittest.getFitness());
		
		// Keep the best individual
		int offset = 0;
		if(saveElite) {
			fittest.resetResources();
			newPop.add(fittest);
			offset = 1;
		}
		
		// Crossover.
		for(int i = offset; i < newPop.getSize(); i++) {
			Individual parent1 = tournamentSelection(pop);
			Individual parent2 = tournamentSelection(pop);
			
			Individual child = crossover(parent1, parent2);
			newPop.add(child);
		}
		
		// Mutate the pop.
		for(int i = offset; i < newPop.getSize(); i++) {
			mutate(newPop.getIndividual(i));
		}
		
		return newPop;
	}
	
	
	private Individual tournamentSelection(Population pop) {
		ArrayList<Individual> tournamentIndividuals = new ArrayList<>();
		
		for(int i = 0; i < tournamentSize; i++) {
			int rand = (int)(Math.random() * pop.getSize());
			tournamentIndividuals.add(pop.getIndividual(rand));
		}
		
		Individual fittest = tournamentIndividuals.get(0);
		
		for(Individual ind : tournamentIndividuals) {
			if (ind.getFitness() > fittest.getFitness()){
				fittest = ind;
			}
		}
		
		return fittest;
	}
	
	
	private Individual crossover(Individual parent1, Individual parent2) {
		double maxRatio1 = parent1.getRatio();
		parent1.updateProbability();
		double prob1 = parent1.getProbability();
		
		double maxRatio2 = parent2.getRatio();
		parent2.updateProbability();
		double prob2 = parent2.getProbability();
		
		Individual child = parent1;
		
		if(maxRatio1 > maxRatio2) {
			child = new Individual(maxRatio2, maxRatio1, (prob1+prob2)/2.0);
		} else {
			child = new Individual(maxRatio1, maxRatio2, (prob1+prob2)/2.0);
		}
		
		child.resetResources();
		
		return child;
	}
	
	private void mutate(Individual ind) {
		// mutate the probability of the individual selecting movable asteroid.
		double prob = ind.getProbability();
		if(Math.random() < 0.5) {
			double mutatedProb = prob - 0.2;
			if(mutatedProb > 0) {
				ind.setProbability(mutatedProb);
			} else {
				ind.setProbability(0.3);
			}
		} else {
			double mutatedProb = prob + 0.2;
			if(mutatedProb <1.0) {
				ind.setProbability(mutatedProb);
			} else {
				ind.setProbability(0.7);
			}
		}
	}

}

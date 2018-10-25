package gaur4004;

import java.util.ArrayList;
import java.util.Random;

public class Population {
	
	int populationSize = 20;
	ArrayList<Individual> population;
	int currentMember = 0;
	
	double minRatio = 2.0;
	double maxRatio = 645;
	double defaultProb = 0.5;
	Random rand;
	
	Population(boolean initialize){
		population = new ArrayList<>();
		if(initialize) {
			rand = new Random();
			for(int i = 0; i < populationSize; i++) {
				// get random min and max ratio.
				double ratio1 = rand.nextDouble() * (maxRatio - minRatio) + minRatio;
				double ratio2 = rand.nextDouble() * (maxRatio - minRatio) + minRatio;
				if (ratio1 > ratio2) {
					population.add(new Individual(ratio2, ratio1, defaultProb));
				} else {
					population.add(new Individual(ratio1, ratio2, defaultProb));
				}
			}
		}
	}
	
	public Individual getNextIndividual() {
		Individual i = population.get(currentMember);
		currentMember++;
		return i;
	}
	
	public Individual getIndividual(int i) {
		return population.get(i);
	}
	
	public Individual getFittestIndividual() {
		Individual fittest = population.get(0);
		for(Individual ind : population) {
			if(ind.getFitness() > fittest.getFitness()) {
				fittest = ind;
			}
		}
		return fittest;
	}

	public void add(Individual ind) {
		population.add(ind);
	}
	
	
	
	public int getSize() {
		return populationSize;
	}
	
	public void setSize(int size) {
		populationSize = size;
	}
	
	public boolean isGenerationComplete() {
		if (currentMember == population.size())
				return true;
		return false;
	}
}

/*
 * Copyright (C) 2016 Emmanuel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package constraint_solvers;

import graph.GraphColorGraph;
import graph.GraphColorVertex;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import ui.GraphColorVisualizer;

/**
 *
 * @author Emmanuel
 */
public class GeneticAlgorithm {
    
    GraphColorGraph graph;
    GraphColorVisualizer visualizer;
    int maxSteps;
    int numIterations;
    ArrayList<GraphColorVertex> vertexList;
    ArrayList<GraphColorVertex> unassignedVerts;
    
    public GeneticAlgorithm(GraphColorGraph inGraph, int inMaxSteps){
        graph = inGraph;    
        inMaxSteps = maxSteps;
    }
    
    public GeneticAlgorithm(GraphColorGraph inGraph, int inMaxSteps, GraphColorVisualizer inVisualizer) {
        graph = inGraph;
        inMaxSteps = maxSteps;
        visualizer = inVisualizer;
    }
    
    public void initialPop(ArrayList<GraphColorVertex> population){
        vertexList = graph.getVertices();
        population = vertexList;
        
    }
    
    public int[] fitness(GraphColorVertex  vertex, ArrayList<Color> colors){
        int[] numConflicts = new int[colors.size()];
        ArrayList<Integer> connectedVerts = vertex.getConnections();
        for (int i = 0; i < connectedVerts.size(); i++) {
            for (int j = 0; j < colors.size(); j++) {
                if (graph.getVertex(connectedVerts.get(i)).getColor() == colors.get(j)) {
                    numConflicts[j]++;
                }
            }
        }
        return numConflicts;
        
    }
    
    public GraphColorVertex tournament(){
       Random random = new Random();
       GraphColorVertex parent = null;
       //if(terminate() == false){
            //while (isConflicted(vertex, color)){
                for(int i = 0; i < maxSteps; i++){
                    GraphColorVertex gladiator1 = vertexList.get(random.nextInt(vertexList.size()));
                    GraphColorVertex gladiator2 = vertexList.get(random.nextInt(vertexList.size()));
            
                    int[] fitGladiator1 = fitness(gladiator1, graph.getColors());
                    int[] fitGladiator2 = fitness(gladiator2, graph.getColors());
            
                    if (fitGladiator1.length < fitGladiator2.length){
                        parent = gladiator1;
                    }
                    else if (fitGladiator1.length == fitGladiator2.length){
                        parent = gladiator1;
                    }
                    else{    
                        parent = gladiator2;
                    }
                }
            //}
        //}
               
        return parent;
    }
    
    public GraphColorVertex crossover(GraphColorVertex parent, ArrayList<Color> colors){
        GraphColorVertex child = null;
        GraphColorVertex parent1 = tournament();
        GraphColorVertex parent2 = tournament();
        initializeColorDomain(colors);
        
        if((!isConflicted(parent1, parent1.getColor())) && (isConflicted (parent2, parent2.getColor()))){
            child = parent1;
            updateVisualizer();
            return child;
        }
        
        else if((isConflicted(parent1, parent1.getColor())) && (!isConflicted(parent2, parent2.getColor()))){
            child = parent2;
            updateVisualizer();
            return child;
        }
        else if (isConflicted(parent1, parent1.getColor()) && (isConflicted(parent2, parent2.getColor()))){
            child = mutate(parent1, parent1.getColorDomain());
            
            updateVisualizer();
            return child;
            
        }
            return child;
    }
    
    public GraphColorVertex mutate(GraphColorVertex parent, ArrayList<Color> colors){

        initializeColorDomain(colors);
        for (int i = 0; i < colors.size(); i++) {
            if (!isConflicted(parent, colors.get(i))) {
                parent.setColor(colors.get(i));
                
                // Update Visualizer view if applicable
                updateVisualizer();
            }
            else if(isConflicted(parent, colors.get(i))){
                parent.setColor(Color.BLUE);
                
                updateVisualizer();
            }
        }
        return parent;
      
    }
    
    public boolean terminate(){
        for(int i = 0; i < vertexList.size(); i++){
            if (graph.isColored()) {
                return true;
            }
        }
        return false;
        //finish if graph is colored, or if the rest is not possible
    }
    
    protected void initializeColorDomain(ArrayList<Color> colors) {
        for (int i = 0; i < vertexList.size(); i++) {
            vertexList.get(i).setColorDomain(colors);
        }
    }
    
     private void updateVisualizer(){
        if (visualizer != null && visualizer.isRunning()){
            visualizer.updateVisualizer(graph);
        }
    }
         
     private boolean isConflicted(GraphColorVertex vertex, Color color) {
        ArrayList<Integer> connectedVerts = vertex.getConnections();
        for (int i = 0; i < connectedVerts.size(); i++) {
            if (color.equals(graph.getVertex(connectedVerts.get(i)).getColor())) {


                return true;
            }
        }
        return false;
    }
    
}

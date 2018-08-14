/*
 * Copyright (C) 2016 matthewrohrlach, nwmoore, emmanuel-armstrong
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
package algorithms;

import data_components.DataPoint;
import data_components.DataSet;
import data_components.ProcessData;
import data_components.tan.Edge;
import data_components.tan.NodeTan;
import data_components.tan.WeightComparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author matthewrohrlach, nwmoore
 */
public class TANaiveBayes {
    
    // Global variable that stores a parent DataSet
    protected final DataSet dataSet;
    
    // Global variable that stores a testing DataSet
    protected DataSet testingDataSet;
    
    // Global variable that stores list of training DataSets
    protected DataSet trainingDataSet;
    
    // Global variable that stores a DataSet file name
    protected final String dataSetName;
    
    // Global variable that stores an arraylist that stores lists of processed datapoints
    protected ArrayList<ArrayList<DataPoint>> processedPointsList;
    
    // Global variable that stores an arraylist that stores lists of separated training points
    protected ArrayList<ArrayList<DataPoint>> separatedPointsList;
    
    // Global variable that stores the index of DataPoints' actual class values
    protected final int classIndex;
    
    // Global variable that stores the number of unique classes in the DataSet
    protected int classCounter;
    
    // Global variable that stores the names of classes that correspond to lists in array
    protected ArrayList<String> observedClasses;
    
    // Global variable that stores the number of unique values per attribute index
    protected int[] potentialAttributeValues;
    protected ArrayList<ArrayList<String>> observedAttributeValues;
    
    // Global variable that stores probabilities per attribute per value per class
    double[][][] featureProbabilities;
    
    //Global variable that contains unweighted graph
    protected NodeTan rootNode;
    protected ArrayList<NodeTan> graphNodes;
    protected ArrayList<Edge> nodeEdges;
    
    // Global variable that stores max weighted spanning tree
    protected ArrayList<Edge> maxWeightedSpanTree;
    protected ArrayList<Edge> protectedEdges;
  
    
    // Constructor that takes a ProcessData object
    public TANaiveBayes (ProcessData processData) {
        
        // Set dataSet object using processData
        dataSet = processData.getDataSet();
        
        // Set dataSet name using processData
        dataSetName = processData.getDataSetName();
        
        // Set classIndex using processData
        classIndex = processData.getClassIndex();
        
        // Count the number of unique classes
        classCounter = 0;
        observedClasses = new ArrayList<>();
        String observedClass;
        for (DataPoint observedDataPoint : dataSet.getDataSet()) {
            
            // Test for a unique class
            observedClass = observedDataPoint.getFeatures()[classIndex];
            if (!observedClasses.contains(observedClass)) {
                classCounter++;
                observedClasses.add(observedClass);
            }
        }
        
        // Calculate the number of potential values for every attribute (and class)
        int numAttributes = dataSet.getDataPoint(0).getFeatures().length;
        observedAttributeValues = new ArrayList<>();
        
        // Initialize observed attribute value lists
        for (int addedLists = 0; addedLists < numAttributes; addedLists++) {
            ArrayList<String> listToAdd = new ArrayList<>();
            observedAttributeValues.add(listToAdd);
        }
        
        // Track unique values for each attribute
        potentialAttributeValues = new int[numAttributes];
        for (int attributeIterator = 0; attributeIterator < numAttributes; attributeIterator++) {
            
            potentialAttributeValues[attributeIterator] = this.numberOfFeatureValues(attributeIterator);
        }
        
        // Constructor calls final method to classify dataset
        processedPointsList = new ArrayList<>();
        separatedPointsList = new ArrayList<>();
        
        // Classify with tenFoldCrossValidation
        for (int testIndexIterator = 0; testIndexIterator < 10; testIndexIterator++) {

            // Set the appropriate testing dataSet
            testingDataSet = this.dataSet.getSubsetAt(testIndexIterator);

            // Initialize processed datapoint arraylist to size of number of unique classes
            processedPointsList.clear();
            for (int addedLists = 0; addedLists < classCounter; addedLists++) {
                ArrayList<DataPoint> listToAdd = new ArrayList<>();
                processedPointsList.add(listToAdd);
            }

            // Rebuild the training dataSet
            trainingDataSet = new DataSet();
            for (int setIterator = 0; setIterator < 10; setIterator++) {

                if (setIterator != testIndexIterator) {
                    
                    for (DataPoint trainPoint : this.dataSet.getSubsetAt(setIterator).getDataSet()) {
                        
                        trainingDataSet.addToSet(trainPoint);
                    }
                }
            }

            // Run the classification algorithm
            classifyDataSet();
        }
    }
    
    
    // Method that processes dataset (no arguments, uses globally-stored sets)
    protected final void classifyDataSet() {
        
        // Run algorithm
        
        // ========= TRAINING ============================
        
        // Calculate Naive Bayes probabilities
        // Separate training points into lists and calculate prior probabilities
        separateTrainingSet();
        double[] priorProbabilities = new double[separatedPointsList.size()];
        for (int listIterator = 0; listIterator < separatedPointsList.size(); listIterator++) {
            
            priorProbabilities[listIterator] = ((double) separatedPointsList.get(listIterator).size()
                    / (double) trainingDataSet.size());
        }
        
        // Calculate the probability of every feature with regards to class
        int featureSize = (dataSet.getDataPoint(0).getFeatures().length - 1);
        int thisPotentialAttributeValues;
        
        // Calculate max of potential values for each attribute
        int greatestNumberOfFeatures = -1;
        int thisNumberOfFeatures;
        for (ArrayList<String> observedAttributeValueList : observedAttributeValues) {
            
            thisNumberOfFeatures = observedAttributeValueList.size();
            if (thisNumberOfFeatures > greatestNumberOfFeatures) {
                greatestNumberOfFeatures = thisNumberOfFeatures;
            }
        }
        featureProbabilities = new double[featureSize][greatestNumberOfFeatures][separatedPointsList.size()+1];
        String featureInQuestion;
        String thisFeature;
        int featureCount;
        int featureCountOfAllClasses;
        
        // For every feature
        for (int featureIterator = 0; featureIterator < featureSize; featureIterator++) {
            
            if (featureIterator != classIndex) {
                thisPotentialAttributeValues = potentialAttributeValues[featureIterator];
                // For every potential value of that feature
                for (int valueIterator = 0; valueIterator < thisPotentialAttributeValues; valueIterator++) {

                    // For every class to which a potential value may belong
                    featureCountOfAllClasses = 0;
                    for (int classListIterator = 0; classListIterator < separatedPointsList.size(); classListIterator++) {

                        // Calculate the number of occurences of this value within this class list
                        featureInQuestion = observedAttributeValues.get(featureIterator).get(valueIterator);
                        featureCount = 0;
                        for (DataPoint point : separatedPointsList.get(classListIterator)) {

                            thisFeature = point.getFeatures()[valueIterator];
                            if (thisFeature.equals(featureInQuestion)) {

                                featureCount++;
                            }
                        }

                        // Track this feature count as part of a sum
                        featureCountOfAllClasses += featureCount;

                        // Probability of (this feature value) of (this feature) of (this specific class) is stored
                        featureProbabilities[featureIterator][valueIterator][classListIterator] = ((double) featureCount / (double) separatedPointsList.get(classListIterator).size());
                    }
                    // Probability of (this feature value) over all classes is stored
                    featureProbabilities[featureIterator][valueIterator][separatedPointsList.size()] = ((double) featureCountOfAllClasses / (double) trainingDataSet.size());
                }
            }
        }
        
        // Construct unweighted graph
        graphNodes = new ArrayList<>();
        nodeEdges = new ArrayList<>();
        for (int featureIterator = 0; featureIterator < featureSize; featureIterator++) {
            
            // Create a node for every attribute
            if (featureIterator != classIndex) {
                graphNodes.add(new NodeTan(null, featureIterator));
            }
        }
        // Create an unweighted edge between all attribute nodes
        for (NodeTan graphNodeOuter : graphNodes) {
            
            for (NodeTan graphNodeInner : graphNodes) {
                
                if (graphNodeOuter != graphNodeInner) {
                    Edge newEdge = new Edge(graphNodeOuter,graphNodeInner);
                    graphNodeOuter.addEdge(newEdge);
                    nodeEdges.add(newEdge);
                }
            }
        }
        
        // Conditional mutual information function to weight edges
        double newWeight;
        for (NodeTan graphNode : graphNodes) {
            
            for (int featureIterator = 0; featureIterator < featureSize; featureIterator++) {
            
                if (featureIterator != classIndex && featureIterator != graphNode.getAttributeIndex()) {
                    // Calculate weight with conditional mutual information function
                    newWeight = conditionalMutualInformationFunction(graphNode.getAttributeIndex(), featureIterator);

                    // Assign weight to corresponding edge
                    graphNode.findEdge(featureIterator).setWeight(newWeight);
                }
            }
        }
        
        // Turn into maximum weighted spanning tree
        // Sort weights in descending order
        Collections.sort(nodeEdges, new WeightComparator());
        
        //Remove duplicates
        ArrayList<Edge> edgesToRemove = new ArrayList<>();
        for (Edge sortedEdge : nodeEdges) {
            
            for (Edge sortedEdgeInner : nodeEdges) {
                
                if (!edgesToRemove.contains(sortedEdge)) {
                    
                    if (sortedEdge.getOrigin() == sortedEdgeInner.getDestination()) {

                        if (sortedEdge.getDestination() == sortedEdgeInner.getOrigin()) {

                            edgesToRemove.add(sortedEdgeInner);
                        }
                    }
                }
            }
        }
        for (Edge edgeToRemove : edgesToRemove) {
            
            nodeEdges.remove(edgeToRemove);
        }
        
        // Add edges to set of edges comprising maximum spanning tree
        maxWeightedSpanTree = new ArrayList<>();
        
        // Remove previously-removed duplicate edges
        ArrayList<Edge> edgesToDrop = new ArrayList<>();
        for (NodeTan graphNode : graphNodes) {
            
            edgesToDrop.clear();
            for (Edge graphEdge : graphNode.getEdges()) {
                
                if (!nodeEdges.contains(graphEdge)) {
                    edgesToDrop.add(graphEdge);
                }
            }
            
            for (Edge dropEdge : edgesToDrop) {
                graphNode.dropEdge(dropEdge);
            }
        }
        
        // Share edges between nodes
        NodeTan currentDestination;
        NodeTan currentOrigin;
        for (NodeTan graphNode : graphNodes) {
            
            for (Edge graphEdge : nodeEdges) {
                
                currentDestination = graphEdge.getDestination();
                currentOrigin = graphEdge.getOrigin();
                if (currentDestination == graphNode || currentOrigin == graphNode) {
                    graphNode.addEdge(graphEdge);
                }
            }
        }
        
        // Start max weighted creating spanning tree
        ArrayList<NodeTan> visitedNodes = new ArrayList<>();
        NodeTan startingPoint = nodeEdges.get(0).getOrigin();
        visitedNodes.add(startingPoint);
        
        Edge chosenEdge;
        ArrayList<Edge> currentChosenEdges = new ArrayList<>();
        int count = 0;
        double bestWeight;
        Edge bestWeightEdge;
        double currentWeight;
        int currentIndex;
        NodeTan targetDestination;
        NodeTan visitedNode;
        ArrayList<Edge> trueNodeEdges;
        
        // Loop until max weighted spanning tree is reached
        while (count < graphNodes.size()-1) {
            
            currentChosenEdges.clear();
            for (int nodeIterator = 0; nodeIterator < visitedNodes.size(); nodeIterator++) {

                bestWeight = -1;
                
                bestWeightEdge = null;
                visitedNode = visitedNodes.get(nodeIterator);
                trueNodeEdges = visitedNode.getEdges();
                currentIndex = visitedNode.getAttributeIndex();
                
                for (int i = 0; i < trueNodeEdges.size(); i++) {

                    chosenEdge = trueNodeEdges.get(i);
                    currentWeight = chosenEdge.getWeight();
                    
                    if (visitedNode == trueNodeEdges.get(i).getOrigin()) {
                        
                        targetDestination = trueNodeEdges.get(i).getDestination();
                    }
                    else {
                        
                        targetDestination = trueNodeEdges.get(i).getOrigin();
                    }
                    
                    if ((currentWeight > bestWeight) && !visitedNodes.contains(targetDestination)) {

                        bestWeight = currentWeight;
                        bestWeightEdge = chosenEdge;
                    }
                }
                
                if (bestWeight > 0) {
                    currentChosenEdges.add(bestWeightEdge);
                }
            }
            if (!currentChosenEdges.isEmpty()) {
                Collections.sort(currentChosenEdges, new WeightComparator());
                if (!visitedNodes.contains(currentChosenEdges.get(0).getOrigin())) {
                    currentChosenEdges.get(0).reverse();
                }
                maxWeightedSpanTree.add(currentChosenEdges.get(0));
                
                
                if (!visitedNodes.contains(currentChosenEdges.get(0).getOrigin())) {
                    visitedNodes.add(currentChosenEdges.get(0).getOrigin());
                }
                if (!visitedNodes.contains(currentChosenEdges.get(0).getDestination())) {
                    visitedNodes.add(currentChosenEdges.get(0).getDestination());
                }
            }
            else {
                break;
            }
            
            count++;
        }
        
        // Replace list of edges with maxWeightedSpanningTree
        nodeEdges = new ArrayList<>(maxWeightedSpanTree);
        rootNode = startingPoint;
        
        // Set parents of nodes
        // Remove previously-removed duplicate edges
        for (NodeTan graphNode : graphNodes) {
            
            edgesToDrop.clear();
            for (Edge graphEdge : graphNode.getEdges()) {
                
                if (!nodeEdges.contains(graphEdge)) {
                    edgesToDrop.add(graphEdge);
                }
            }
            
            for (Edge dropEdge : edgesToDrop) {
                graphNode.dropEdge(dropEdge);
            }
            
            graphNode.findParent();
        }
        
        
        // ========= TESTING ============================
        
        // Classify points with tree
        for (int pointIterator = 0; pointIterator < testingDataSet.size(); pointIterator++) {
            
            this.classifyTestPoint(testingDataSet.getDataPoint(pointIterator));
        }
        
        //=============================================
        
        // Run method to print metrics
        printMetrics();
    }
    
    /**
     * Direct subtree according to origin point
     * @param origin 
     */
    protected void directNodes(NodeTan origin) {
        
        // Flip unexamined adjacent edges that are turned the wrong direction
        for (Edge originEdge : origin.getEdges()) {
            
            if (!protectedEdges.contains(originEdge)) {
                
                if (originEdge.getDestination() == origin) {
                    
                    originEdge.reverse();
                }
                protectedEdges.add(originEdge);
            }
        }
        
        // Recurse through all destination nodes
        for (Edge originEdge : origin.getEdges()) {
            
            if (originEdge.getDestination() != origin) {
                
                directNodes(originEdge.getDestination());
            }
        }
    }
    
    /**
     * Classify a single datapoint, add it to the proper list
     * @param point 
     */
    protected void classifyTestPoint(DataPoint point) {
        
        int indexOfClassification = 0;
        double bestProbability = -1;
        int bestClassIndex = 0;
        double currentProbability;
        int currentParentIndex;
        int currentNodeIndex;
        int currentFeatureIndex;
        
        for (int classIterator = 0; classIterator < observedClasses.size(); classIterator++) {
            
            currentProbability = 1.0;
            for (NodeTan graphNode : graphNodes) {

                currentNodeIndex = graphNode.getAttributeIndex();
                currentFeatureIndex = this.observedAttributeValues.get(currentNodeIndex).indexOf(point.getFeatures()[currentNodeIndex]);
                if (graphNode.getParent() == null) {
                    currentProbability *= this.featureProbabilities[graphNode.getAttributeIndex()][currentFeatureIndex][classIterator];
                }
                else {
                    
                    currentParentIndex = graphNode.getParent().getAttributeIndex();
                    currentProbability *= this.conditionalMutualInformationFunctionSingleClass(currentNodeIndex, currentParentIndex, classIterator);
                }
            }
            
            if (currentProbability > bestProbability) {
                
                bestProbability = currentProbability;
                bestClassIndex = classIterator;
            }
        }
        indexOfClassification = bestClassIndex;
        
        this.processedPointsList.get(indexOfClassification).add(point);
    }
    
    /**
     * Determines the amount of information that two attributes share with each other
     * @param attributeIndexOne
     * @param attributeIndexTwo
     * @return 
     */
    protected double conditionalMutualInformationFunction(int attributeIndexOne, int attributeIndexTwo) {
        
        if (attributeIndexOne == attributeIndexTwo) {
            
            return -1;
        }
        
        double summation = 0.0;
        double probabilityAll;
        double probabilityNumerator;
        double probabilityDenominator;
        String attributeValueOne;
        String attributeValueTwo;
        String classValue;
        
        int oneSize = this.numberOfFeatureValues(attributeIndexOne);
        int twoSize = this.numberOfFeatureValues(attributeIndexTwo);
        int numClasses = this.observedClasses.size();
        
        for (int classIterator = 0; classIterator < numClasses; classIterator++) {
            
            for (int oneValueIterator = 0; oneValueIterator < oneSize; oneValueIterator++) {
                    
                for (int twoValueIterator = 0; twoValueIterator < twoSize; twoValueIterator++) {
            
                    // P(x,y,z) =
                    probabilityAll = ((double) this.separatedPointsList.get(classIterator).size() / (double) this.trainingDataSet.size());
                    probabilityAll = probabilityAll * this.featureProbabilities[attributeIndexOne][oneValueIterator][classIterator];
                    probabilityAll = probabilityAll * this.featureProbabilities[attributeIndexTwo][twoValueIterator][classIterator];
                    
                    attributeValueOne = observedAttributeValues.get(attributeIndexOne).get(oneValueIterator);
                    attributeValueTwo = observedAttributeValues.get(attributeIndexTwo).get(twoValueIterator);
                    classValue = this.observedClasses.get(classIterator);
                    
                    // P(x,y|z)
                    probabilityNumerator = conjoinedProbabilities(attributeIndexOne,
                            attributeValueOne, attributeIndexTwo, attributeValueTwo,
                            classValue, this.separatedPointsList.get(classIterator));
                    
                    // P(x|z)*P(y|z)
                    probabilityDenominator = this.featureProbabilities[attributeIndexOne][oneValueIterator][classIterator];
                    probabilityDenominator = (probabilityDenominator * this.featureProbabilities[attributeIndexTwo][twoValueIterator][classIterator]);
                    
                    if (Math.abs(probabilityNumerator - probabilityDenominator) < .00000000001) {
                        probabilityNumerator = -1;
                    }
                    
                    if (probabilityDenominator > 0 && probabilityNumerator > 0 && probabilityAll > 0) {
                        summation += (probabilityAll / Math.abs(Math.log(Math.abs((probabilityNumerator/probabilityDenominator)))));
                    }
                    
                }
            }
        }
        
        return summation;
    }
    
    /**
     * Determines the amount of information that two attributes share with each other with respect to a single class
     * @param attributeIndexOne
     * @param attributeIndexTwo
     * @param classListIndex
     * @return 
     */
    protected double conditionalMutualInformationFunctionSingleClass(int attributeIndexOne, int attributeIndexTwo, int classListIndex) {
        
        if (attributeIndexOne == attributeIndexTwo) {
            
            return -1;
        }
        
        double summation = 0.0;
        double probabilityAll;
        double probabilityNumerator;
        double probabilityDenominator;
        String attributeValueOne;
        String attributeValueTwo;
        String classValue;
        
        int oneSize = this.numberOfFeatureValues(attributeIndexOne);
        int twoSize = this.numberOfFeatureValues(attributeIndexTwo);
        int numClasses = this.observedClasses.size();
        
        for (int classIterator = classListIndex; classIterator < (classListIndex+1); classIterator++) {
            
            for (int oneValueIterator = 0; oneValueIterator < oneSize; oneValueIterator++) {
                    
                for (int twoValueIterator = 0; twoValueIterator < twoSize; twoValueIterator++) {
            
                    // P(x,y,z) =
                    probabilityAll = ((double) this.separatedPointsList.get(classIterator).size() / (double) this.trainingDataSet.size());
                    probabilityAll = probabilityAll * this.featureProbabilities[attributeIndexOne][oneValueIterator][classIterator];
                    probabilityAll = probabilityAll * this.featureProbabilities[attributeIndexTwo][twoValueIterator][classIterator];
                    
                    attributeValueOne = observedAttributeValues.get(attributeIndexOne).get(oneValueIterator);
                    attributeValueTwo = observedAttributeValues.get(attributeIndexTwo).get(twoValueIterator);
                    classValue = this.observedClasses.get(classIterator);
                    
                    // P(x,y|z)
                    probabilityNumerator = conjoinedProbabilities(attributeIndexOne,
                            attributeValueOne, attributeIndexTwo, attributeValueTwo,
                            classValue, this.separatedPointsList.get(classIterator));
                    
                    // P(x|z)*P(y|z)
                    probabilityDenominator = this.featureProbabilities[attributeIndexOne][oneValueIterator][classIterator];
                    probabilityDenominator = (probabilityDenominator * this.featureProbabilities[attributeIndexTwo][twoValueIterator][classIterator]);
                    
                    if (Math.abs(probabilityNumerator - probabilityDenominator) < .00000000001) {
                        probabilityNumerator = -1;
                    }
                    
                    if (probabilityDenominator > 0 && probabilityNumerator > 0 && probabilityAll > 0) {
                        summation += (probabilityAll / Math.abs(Math.log(Math.abs((probabilityNumerator/probabilityDenominator)))));
                    }
                    
                }
            }
        }
        
        return summation;
    }
    
    /**
     * Returns the probability of a datapoint belonging to a certain class given two specific values of two specific attributes
     * @param attributeIndexOne
     * @param attributeValueOne
     * @param attributeIndexTwo
     * @param attributeValueTwo
     * @param classValue
     * @param relevantScope
     * @return 
     */
    protected double conjoinedProbabilities(int attributeIndexOne, String attributeValueOne,
            int attributeIndexTwo, String attributeValueTwo, String classValue, DataSet relevantScope) {
        
        int count = 0;
        
        for (DataPoint point : relevantScope.getDataSet()) {
            
            if (point.getFeatures()[attributeIndexOne].equals(attributeValueOne)) {
            
                if (point.getFeatures()[attributeIndexTwo].equals(attributeValueTwo)) {
            
                    if (point.getFeatures()[classIndex].equals(classValue)) {
            
                        count++;
                    }
                }
            }
        }
        
        return ((double)count / (double)relevantScope.size());
    }
    
    /**
     * Returns the probability of a datapoint belonging to a certain class given two specific values of two specific attributes
     * @param attributeIndexOne
     * @param attributeValueOne
     * @param attributeIndexTwo
     * @param attributeValueTwo
     * @param classValue
     * @param relevantScope
     * @return 
     */
    protected double conjoinedProbabilities(int attributeIndexOne, String attributeValueOne,
            int attributeIndexTwo, String attributeValueTwo, String classValue, ArrayList<DataPoint> relevantScope) {
        
        int count = 0;
        
        for (DataPoint point : relevantScope) {
            
            if (point.getFeatures()[attributeIndexOne].equals(attributeValueOne)) {
            
                if (point.getFeatures()[attributeIndexTwo].equals(attributeValueTwo)) {
            
                    if (point.getFeatures()[classIndex].equals(classValue)) {
            
                        count++;
                    }
                }
            }
        }
        
        return ((double)count / (double)relevantScope.size());
    }
    
    /**
     * Separates training dataset into lists by class value
     */
    protected void separateTrainingSet() {
        
        // Initialize processed datapoint arraylist to size of number of unique classes
        separatedPointsList.clear();
        for (int addedLists = 0; addedLists < classCounter; addedLists++) {
            ArrayList<DataPoint> listToAdd = new ArrayList<>();
            separatedPointsList.add(listToAdd);
        }
        
        String thisPointClass;
        DataPoint thisDataPoint;
        int totalPointsToExamine = this.trainingDataSet.getDataSet().size();
        
        // Put each point where it belongs (cheating for testing purposes)
        for (int pointIterator = 0; pointIterator < totalPointsToExamine; pointIterator++) {
            
            thisDataPoint = trainingDataSet.getDataPoint(pointIterator);
            thisPointClass = thisDataPoint.getFeatures()[classIndex];
            separatedPointsList.get(observedClasses.indexOf(thisPointClass)).add(thisDataPoint);
        }
    }
    
    /**
     * Find the number of possible unique values that an attribute may have at an index
     * @param attributeIndex
     * @return 
     */
    protected final int numberOfFeatureValues(int attributeIndex) {
        
        ArrayList<String> theseObservedAttributeValues = new ArrayList<>();
        
        for (DataPoint point : dataSet.getDataSet()) {
            
            if (!theseObservedAttributeValues.contains(point.getFeatures()[attributeIndex])) {
                
                theseObservedAttributeValues.add(point.getFeatures()[attributeIndex]);
            }
        }
        
        observedAttributeValues.set(attributeIndex, theseObservedAttributeValues);
        return theseObservedAttributeValues.size();
    }
    
    /**
     * Hacky method to compile metrics without printing
     * @return array of (numCorrectPoints, numTotalPoints, numCorrectPointsClass 0 .. numCorrectPointsClass n)
     */
    protected int[] calculateMetrics() {
        
        // Track the total percentage of correct points 
        int correctPointCounter = 0;
        int totalPointCounter = 0;
        
        // We'll also track the percentage of correct points by class
        int[] metricArray = new int[classCounter+2];
        
        // For every list of classified data points
        int classListLength;
        for (int listIterator = 0; listIterator < processedPointsList.size(); listIterator++) {
            
            // Take the size of that list of classified data points
            classListLength = processedPointsList.get(listIterator).size();
            
            // Iterate through this list of classified data points
            for (int pointIterator = 0; pointIterator < classListLength; pointIterator++) {
                
                // Track the number of points, correctly-classified points, and correctly-classified points per class
                totalPointCounter++;
                String thisPointClass = processedPointsList.get(listIterator).get(pointIterator).getFeatures()[classIndex];
                if (thisPointClass.equals(observedClasses.get(listIterator))) {
                    
                    correctPointCounter++;
                    metricArray[listIterator+2] = (metricArray[listIterator+2] + 1);
                }
            }
        }
        
        metricArray[0] = correctPointCounter;
        metricArray[1] = totalPointCounter;
        
        return metricArray;
    }
    
    /**
     * Call method to calculate metrics, then print them.
     */
    public void printMetrics() {
        
        int[] calculatedMetrics = calculateMetrics();
        
        // Get the total percentage of correct points 
        int correctPointCounter = calculatedMetrics[0];
        int totalPointCounter = calculatedMetrics[1];
        
        // Get the percentage of correct points by class
        int[] correctPointsByClass = Arrays.copyOfRange(calculatedMetrics, 2, calculatedMetrics.length);
        
        // Build results to output
        String resultString =
                    "Tree-Augmented Naive Bayes Classification of " + dataSetName + ":\n"
                        +  "   =========================\n"
                        +  "   Total points: " + totalPointCounter + "\n"
                        +  "   Correct points: " + correctPointCounter + "\n"
                        +  "   Percentage correct: " + ((double) correctPointCounter 
                                / (double) totalPointCounter * (double) 100.0) + "%\n"
                        +  "   Random chance?: " + ((double) 1.0 / (double) observedClasses.size() * (double) 100.0) + "%\n"
                        +  "   =========================\n";
        
        // Add results per each class
        for (int i = 0; i < classCounter; i++) {
            
            resultString += "   Total classified as " + translateClassName(i) + ": "
                    + processedPointsList.get(i).size() + "\n";
            if (!processedPointsList.get(i).isEmpty()) {
                resultString += "   Percentage correct (" + translateClassName(i) + "): " 
                        + ((double)correctPointsByClass[i] / (double)processedPointsList.get(i).size() * 100.0)
                        + "%\n";
            }
            else {
                resultString += "   Percentage correct (" + translateClassName(i) + "): 0%\n";
            }
        }
        resultString += "   =========================\n";
        
        // Print results
        System.out.println(resultString + "\n");
    }
    
    /**
     * Several of the targeted datasets use numerical values to represent class names.
     * This method translates these values into their relevant names.
     * @param classIndex
     * @return 
     */
    public String translateClassName(int classIndex) {
        
        String untranslatedName = observedClasses.get(classIndex);
        String translatedName;
        switch (dataSetName) {
            
            case "breast-cancer-wisconsin.data.txt":
                switch (untranslatedName) {
                    
                    case "2":
                        translatedName = "benign";
                        break;
                    case "4":
                        translatedName = "malignant";
                        break;
                    default:
                        translatedName = untranslatedName;
                        break;
                }
                break;
                
            case "house-votes-84.data.txt":
                translatedName = untranslatedName;
                break;
                
            case "soybean-small.data.txt":
                translatedName = untranslatedName;
                break;
                
            case "glass.data.txt":
                switch (untranslatedName) {
                    
                    case "1":
                        translatedName = "building_windows_float_processed";
                        break;
                    case "2":
                        translatedName = "building_windows_non_float_processed";
                        break;
                    case "3":
                        translatedName = "vehicle_windows_float_processed";
                        break;
                    case "4":
                        translatedName = "vehicle_windows_non_float_processed";
                        break;
                    case "5":
                        translatedName = "containers";
                        break;
                    case "6":
                        translatedName = "tableware";
                        break;
                    case "7":
                        translatedName = "headlamps";
                        break;
                    default:
                        translatedName = untranslatedName;
                        break;
                }
                break;
                
            case "iris.data.txt":
                translatedName = untranslatedName;
                break;
                
            default:
                translatedName = untranslatedName;
                break;
        }
        
        return translatedName;
    }
}

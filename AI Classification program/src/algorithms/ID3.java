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
import data_components.id3.Node;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author matthewrohrlach
 */
public class ID3 {
    
    //=================== GLOBAL VARIABLES ======================
    
    // Global variable that stores a parent DataSet
    protected final DataSet dataSet;
    
    // Global variable that stores a testing DataSet
    protected DataSet testingDataSet;
    
    // Global variable that stores list of training DataSets
    protected DataSet trainingDataSet;
    
    // Global variable that represents the root node of a decision tree
    protected Node rootNode;
    
    // Global variable that stores a DataSet file name
    protected final String dataSetName;
    
    // Global variable that stores an arraylist that stores lists of processed datapoints
    protected ArrayList<ArrayList<DataPoint>> processedPointsList;
    
    // Global variable that stores the index of DataPoints' actual class values
    protected final int classIndex;
    
    // Global variable that stores the number of unique classes in the DataSet
    protected int classCounter;
    
    // Global variable that stores the names of classes that correspond to lists in array
    protected ArrayList<String> observedClasses;
    
    // Global variable that stores percentage correct of dataset pre-pruning
    protected double baseLinePercentCorrect;
    protected int baseLinePointsCorrect;
    
    // Global variable that stores list of leafNodes
    protected ArrayList<Node> leafNodes;
    
    // Global variable that stores list of parents of leafNodes
    protected ArrayList<Node> leafNodeParents;
    
    
    //=================== CONSTRUCTORS ======================
    
    /**
     * Regular constructor that takes a ProcessData object
     * @param processData 
     */
    public ID3 (ProcessData processData) {
        
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
        
        // Constructor calls final method to classify dataset
        processedPointsList = new ArrayList<>();
        
        // Classify with tenFoldCrossValidation
        for (int testIndexIterator = 0; testIndexIterator < 10; testIndexIterator++) {

            // Set the appropriate testing dataSet
            testingDataSet = this.dataSet.getSubsetAt(testIndexIterator);

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
    
    
    //=================== METHODS ======================
    
    /**
     * Method that processes training dataset, then classifies testing dataset (no arguments, uses globally-stored sets)
     * Reduced-error pruning is run after initial classification.
     */
    protected final void classifyDataSet() {
        
        // Run algorithm
        
        // Data points are added to their respective lists
        //================================================
        
        // Create root node
        ArrayList<Integer> allClassifiers = new ArrayList<>();
        for (int i = 0; i < trainingDataSet.getDataPoint(0).getFeatures().length; i++) {
            if (i != classIndex) {
                allClassifiers.add(i);
            }
        }
        rootNode = new Node(trainingDataSet, new ArrayList<>(allClassifiers));
        
        // Build tree with training set
        classifyNodeSet(rootNode);
        
        // Initialize processed datapoint arraylist to size of number of unique classes
        initializeProcessedPointLists();
        
        // Run classification on testing set
        for (DataPoint point : testingDataSet.getDataSet()) {
            
            classifyPoint(point);
        }
        
        // Reduced error pruning:
        reducedErrorPruning();
        
        // Re-classify after pruning or failing to achieve a better effort through pruning
        initializeProcessedPointLists();
        for (DataPoint point : testingDataSet.getDataSet()) {

            classifyPoint(point);
        }
        
        //=============================================
        
        // Run method to print metrics
        printMetrics();
    }
    
    /**
     * Classifies a given datapoint according to the decision tree that starts at rootNode
     * @param point 
     */
    protected void classifyPoint(DataPoint point) {
        
        Node currentNode = rootNode;
        int currentNodeAttributeIndex;
        String attributeToMatch;
        while (!currentNode.isLeafNode()) {
            
            currentNodeAttributeIndex = currentNode.getAttributeIndex();
            attributeToMatch = point.getFeatures()[currentNodeAttributeIndex];
            currentNode = currentNode.findChild(attributeToMatch);
        }
        String classification = currentNode.getLeafClass();
        processedPointsList.get(observedClasses.indexOf(classification)).add(point);
        
    }
    
    /**
     * Performs reduced-error pruning.
     * 1. Finds greatest error reduction in a single node -> leafnode change
     * 2. Loops with that node changed to a leafnode
     * 3. Ends loop when no improvements are made after a pruning cycle.
     * 4. Measures improvement against baseline, reverts if no improvement
     */
    protected void reducedErrorPruning() {
        
        boolean donePruning = false;
        
        int[] baseLineMetrics = calculateMetrics();
        int[] currentMetrics;
        baseLinePercentCorrect = ((double)baseLineMetrics[0] / (double)baseLineMetrics[1]);
        baseLinePointsCorrect = baseLineMetrics[0];
        
        double bestPercentCorrect = -1;
        double prevBestPercentCorrect;
        Node bestPercentCorrectLeaf = null;
        double currentPercentCorrect;
        leafNodes = new ArrayList<>();
        leafNodeParents = new ArrayList<>();
        ArrayList<Node> addedLeafNodes = new ArrayList<>();
        
        // Build list of leaf nodes
        findLeafNode(rootNode);
        
        while (!donePruning) {
            
            prevBestPercentCorrect = bestPercentCorrect;
            for (Node leafNodeParent : leafNodeParents) {
                
                // Clear lists of processed points
                initializeProcessedPointLists();
                
                // Set this leafNodeParent to a leaf node that assigns its majority class
                classifyLeafNodeAccordingToSelfMajorityClass(leafNodeParent);
                
                // Run classification on testing set
                for (DataPoint point : testingDataSet.getDataSet()) {

                    classifyPoint(point);
                }
                
                // Calculate error
                currentMetrics = calculateMetrics();
                currentPercentCorrect = ((double)currentMetrics[0] / (double)currentMetrics[1]);
                
                // Test to see if error is minimized relative to previous parents
                if (currentPercentCorrect > bestPercentCorrect) {
                    
                    bestPercentCorrect = currentPercentCorrect;
                    bestPercentCorrectLeaf = leafNodeParent;
                }
                
                // Unset parent for further testing
                leafNodeParent.unsetLeafNode();
            }
            
            // Add bestPercentCorrectLeaf as permanent leafNode
            classifyLeafNodeAccordingToSelfMajorityClass(bestPercentCorrectLeaf);
            addedLeafNodes.add(bestPercentCorrectLeaf);
            
            // Add the parent of the best pruning leaf to the list of potential pruning leafs
            if (bestPercentCorrectLeaf.getParent() != null) {
                leafNodeParents.add(bestPercentCorrectLeaf.getParent());
            }
            
            // If there was no improvement over all leaf node parents, end pruning
            if (bestPercentCorrect == prevBestPercentCorrect) {
                
                donePruning = true;
            }
        }
        
        if (bestPercentCorrect < baseLinePercentCorrect) {
            
            for (Node addedNode : addedLeafNodes) {
                
                addedNode.unsetLeafNode();
            }
        }
    }
    
    /**
     * Add every leaf node in the tree to the leafNodes list
     * @param nextNode 
     */
    protected void findLeafNode(Node nextNode) {
        
        Node currentNode = nextNode;
        if (currentNode.isLeafNode()) {
            
            if (!leafNodes.contains(currentNode)) {
                leafNodes.add(currentNode);
                
                if (!leafNodeParents.contains(currentNode.getParent())) {
                    leafNodeParents.add(currentNode.getParent());
                }
            }
        }
        else {
            
            // Recurse for all children
            for (int i = 0; i < currentNode.getChildren().size(); i++) {
                findLeafNode(currentNode.getChildren().get(i));
            }
        }
    }
    
    /**
     * Builds a decision tree of Node objects, recursively, until finished
     * @param nodeIn 
     */
    protected void classifyNodeSet(Node nodeIn) {
        
        Node thisNode = nodeIn;
        DataSet nodeData = thisNode.getDataSet();
        ArrayList<Integer> currentAvailableClassifiers = new ArrayList<>(thisNode.getAvailableClassifiers());
        
        if (thisNode.getDataSet().getDataSet().isEmpty()) {
            
            thisNode.setLeafNode(classIndex);
            
            // Calculate number of appearances of values for classes
            int[] pointsPerClass = new int[observedClasses.size()];
            String observedClass;
            int observedClassIndex;
            for (DataPoint point : thisNode.getParent().getDataSet().getDataSet()) {

                observedClass = point.getFeatures()[classIndex];
                observedClassIndex = observedClasses.indexOf(observedClass);
                pointsPerClass[observedClassIndex] = (pointsPerClass[observedClassIndex] + 1);
            }
            int maxPointsPerClassIndex = 0;
            int maxPointsPerClass = Integer.MIN_VALUE;
            int currentPointsPerClass;
            for (int classIterator = 0; classIterator < pointsPerClass.length; classIterator++) {
                
                currentPointsPerClass = pointsPerClass[classIterator];
                if (currentPointsPerClass < maxPointsPerClass) {
                    
                    maxPointsPerClassIndex = classIterator;
                    maxPointsPerClass = currentPointsPerClass;
                }
            }
            
            thisNode.setLeafClass(observedClasses.get(maxPointsPerClassIndex));
        }
                
        else if (thisNode.getDataSet().isPureSet(classIndex)) {
            
            thisNode.setLeafNode(classIndex);
        }
        
        else if (currentAvailableClassifiers.isEmpty()) {
            
            this.classifyLeafNodeAccordingToSelfMajorityClass(thisNode);
        }
        
        else {
            // Calculate the highest information gain out of all attributes of a given node
            double currentInformationGain;
            double maxInformationGain = -1;
            int indexOfBestInformationGain = -1;
            for (int classifier : currentAvailableClassifiers) {

                currentInformationGain = this.calculateInformationGain(nodeData, classifier);
                if (currentInformationGain > maxInformationGain) {

                    indexOfBestInformationGain = classifier;
                    maxInformationGain = currentInformationGain;
                }
            }
                
        
            // Set current node's attribute index to the chosen attribute
            thisNode.setAttributeIndex(indexOfBestInformationGain);
            thisNode.removeClassifier(indexOfBestInformationGain);
            buildNodeChildren(thisNode, indexOfBestInformationGain);
            
            // Recurse for all children
            for (int i = 0; i < thisNode.getChildren().size(); i++) {
                classifyNodeSet(thisNode.getChildren().get(i));
            }
        }
    }
    
    /**
     * Set a leaf node to assign the class shared by the majority of its dataset
     * @param nodeToClassify 
     */
    protected void classifyLeafNodeAccordingToSelfMajorityClass(Node nodeToClassify) {
        
        nodeToClassify.setLeafNode(classIndex);
            
        // Calculate number of appearances of values for classes
        int[] pointsPerClass = new int[observedClasses.size()];
        String observedClass;
        int observedClassIndex;
        for (DataPoint point : nodeToClassify.getDataSet().getDataSet()) {

            observedClass = point.getFeatures()[classIndex];
            observedClassIndex = observedClasses.indexOf(observedClass);
            pointsPerClass[observedClassIndex] = (pointsPerClass[observedClassIndex] + 1);
        }
        int maxPointsPerClassIndex = 0;
        int maxPointsPerClass = Integer.MIN_VALUE;
        int currentPointsPerClass;
        for (int classIterator = 0; classIterator < pointsPerClass.length; classIterator++) {

            currentPointsPerClass = pointsPerClass[classIterator];
            if (currentPointsPerClass < maxPointsPerClass) {

                maxPointsPerClassIndex = classIterator;
                maxPointsPerClass = currentPointsPerClass;
            }
        }

        nodeToClassify.setLeafClass(observedClasses.get(maxPointsPerClassIndex));
    }
    
    /**
     * Calculate the entropy of a given DataSet object
     * @param entropySet
     * @return 
     */
    protected double calculateEntropy(DataSet entropySet) {
        
        // Calculate the number of points belonging to each class in the set
        int[] pointsPerClass = new int[classCounter];
        int totalPoints = entropySet.getDataSet().size();
        int thisClassIndex;
        String thisPointClass;
        for (int pointIterator = 0; pointIterator < totalPoints; pointIterator++) {

            thisPointClass = entropySet.getDataSet().get(pointIterator).getFeatures()[classIndex];
            thisClassIndex = observedClasses.indexOf(thisPointClass);
            
            pointsPerClass[thisClassIndex] = (pointsPerClass[thisClassIndex] + 1);
        }
        
        // Then apply these counts as proportions in the Entropy function
        double entropy = 0.0;
        double proportion;
        
        for (int numPoints : pointsPerClass) {
            
            if (numPoints > 0) {
                proportion = ((double)numPoints / (double)totalPoints);
                entropy += (-1 * proportion * (Math.log(proportion)/Math.log(2)));
            }
        }
        
        return entropy;
    }
    
    /**
     * Calculate the information gain of a DataSet object with regards to a certain feature/attribute
     * @param informationSet
     * @param attributeIndex
     * @return 
     */
    protected double calculateInformationGain(DataSet informationSet, int attributeIndex) {
        
        double totalSetEntropy = calculateEntropy(informationSet);
        int numPoints = informationSet.size();
        int numAttributeValues;
        
        // Calculate number of potential values for given attribute
        ArrayList<String> observedAttributeValues = new ArrayList<>();
        for (DataPoint point : informationSet.getDataSet()) {
            
            if (!observedAttributeValues.contains(point.getFeatures()[attributeIndex])) {
                
                observedAttributeValues.add(point.getFeatures()[attributeIndex]);
            }
        }
        numAttributeValues = observedAttributeValues.size();
        
        // Calculate number of appearances of values for given attribute
        int[] pointsPerAttributeValue = new int[numAttributeValues];
        int observedAttributeIndex;
        for (DataPoint point : informationSet.getDataSet()) {
            
            observedAttributeIndex = observedAttributeValues.indexOf(point.getFeatures()[attributeIndex]);
            pointsPerAttributeValue[observedAttributeIndex] = (pointsPerAttributeValue[observedAttributeIndex] + 1);
        }
        
        // Calculate and store the entropy of every subset split on an attribute value
        DataSet setSplitOnAttribute;
        double[] entropiesOfSubsets = new double[numAttributeValues];
        for (int splitIterator = 0; splitIterator < numAttributeValues; splitIterator++) {
            
            if (pointsPerAttributeValue[splitIterator] > 0) {
                
                setSplitOnAttribute = createDataSetSplit(informationSet, attributeIndex, observedAttributeValues.get(splitIterator));
                entropiesOfSubsets[splitIterator] = this.calculateEntropy(setSplitOnAttribute);
            }
            else {
                entropiesOfSubsets[splitIterator] = 0;
            }
        }
        
        // Find the information gain of the information set
        double returnInformationGain = totalSetEntropy;
        for (int entropyIterator = 0; entropyIterator < entropiesOfSubsets.length; entropyIterator++) {
            
            returnInformationGain -= ((double) pointsPerAttributeValue[entropyIterator] 
                    / (double) numPoints) * entropiesOfSubsets[entropyIterator];
        }
        
        return (returnInformationGain);
    }
    
    /**
     * Build a subset of points that share a given attribute value at a given attribute index
     * @param sourceSet
     * @param attributeIndex
     * @param attributeValue
     * @return 
     */
    protected DataSet createDataSetSplit(DataSet sourceSet, int attributeIndex, String attributeValue) {
        
        DataSet returnSet = new DataSet();
        for (DataPoint point : sourceSet.getDataSet()) {
            
            if (attributeValue.equals(point.getFeatures()[attributeIndex])) {
                
                returnSet.addToSet(point);
            }
        }
        
        return returnSet;
    }
    
    /**
     * Builds the child list of a given node with the relevant datasets
     * @param node
     * @param attributeIndex 
     */
    protected void buildNodeChildren(Node node, int attributeIndex) {
        
        DataSet associatedDataSet = node.getDataSet();
        
        // Calculate potential values for given attribute to create node edges
        ArrayList<String> observedAttributeValues = new ArrayList<>();
        for (DataPoint point : dataSet.getDataSet()) {
            
            if (!observedAttributeValues.contains(point.getFeatures()[attributeIndex])) {
                
                observedAttributeValues.add(point.getFeatures()[attributeIndex]);
            }
        }
        int numAttributeValues = observedAttributeValues.size();
        int i = 0;
        
        // Create a child for every edge
        Node childNode;
        DataSet childDataSet;
        for (int edgeIterator = 0; edgeIterator < numAttributeValues; edgeIterator++) {
            
            childDataSet = createDataSetSplit(associatedDataSet, attributeIndex, observedAttributeValues.get(edgeIterator)); 
            childNode = new Node(node, childDataSet);
            node.addChild(childNode, observedAttributeValues.get(edgeIterator));
        }
        int j = 0;
    }
    
    /**
     * Clear the lists of processed points
     */
    protected void initializeProcessedPointLists() {
        
        // Initialize processed datapoint arraylist to size of number of unique classes
        processedPointsList.clear();
        for (int addedLists = 0; addedLists < classCounter; addedLists++) {
            ArrayList<DataPoint> listToAdd = new ArrayList<>();
            processedPointsList.add(listToAdd);
        }
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
                    "ID3 Classification of " + dataSetName + ":\n"
                        +  "   =========================\n"
                        +  "   Total points: " + totalPointCounter + "\n"
                        +  "   Correct points (baseline): " + (baseLinePointsCorrect) + "\n"
                        +  "   Correct points (reduced-error pruning): " + correctPointCounter + "\n"
                        +  "   Percentage correct (baseline): " + ((double) baseLinePercentCorrect * (double) 100.0) + "%\n"
                        +  "   Percentage correct (reduced-error pruning): " + ((double) correctPointCounter 
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

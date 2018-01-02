/*
 * Copyright (C) 2016 nwmoore
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

/**
 *
 * @author nwmoore
 */
public class SimpleBacktrack {

    GraphColorGraph graph;
    int maxSteps;
    int numIterations;
    ArrayList<GraphColorVertex> vertexList;

    /**
     * Constructor
     * @param inGraph 
     */
    public SimpleBacktrack(GraphColorGraph inGraph) {
        graph = inGraph;
    }

    /**
     * Starting method for simple backtracking
     * @param graph 
     * @param numColors
     * @return boolean true if colored false if not
     */
    public boolean simpleBacktrackSolver(GraphColorGraph graph, int numColors) {
        vertexList = graph.getVertices();
        if (!backtrack(graph, graph.getColors(), 0)) {
            System.out.println("Solution does not exist");
            return false;
        }
        System.out.println("Colored");
        return true;
    }

    /**
     * Main solving method for simple backtracking
     * @param graph 
     * @param numColors
     * @return boolean true if colored false if not
     */
    private boolean backtrack(GraphColorGraph graph, ArrayList<Color> colors, int vertexIndex) {
        if (graph.isColored()) {
            return true;
        }

        for (int i = 0; i < colors.size(); i++) {
            if (!isConflicted(vertexList.get(vertexIndex), colors.get(i))) {
                vertexList.get(vertexIndex).setColor(colors.get(i));
                if (backtrack(graph, graph.getColors(), vertexIndex + 1)) {
                    return true;
                }
                vertexList.get(vertexIndex).setColor(Color.WHITE);
            }
        }

        return false;
    }

    /**
     * Checks if vertex color combination will cause a conflict
     * @param vertex
     * @param color
     * @return boolean true if conflicted false if not
     */
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

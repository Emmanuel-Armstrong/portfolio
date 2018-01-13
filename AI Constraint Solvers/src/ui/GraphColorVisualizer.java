/*
 * Copyright (C) 2016 Matthew Rohrlach
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
package ui;

import graph.GraphColorGraph;
import graph.GraphColorVertex;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * Visualizer for Project 1
 * @author Matthew Rohrlach
 */
public class GraphColorVisualizer extends ParentVisualizer{
    
    /**
     * Regular constructor, takes a graph to display
     * @param visualGraphIn 
     */
    public GraphColorVisualizer(GraphColorGraph visualGraphIn){
        super(visualGraphIn);
    }
    
    /**
     * Regular constructor, takes a graph to display and length of sleep time
     * @param visualGraphIn
     * @param sleepTimeIn
     */
    public GraphColorVisualizer(GraphColorGraph visualGraphIn, long sleepTimeIn){
        super(visualGraphIn, sleepTimeIn);
    }
    
    /**
     * Build Visualizer component with line segments and vertex points
     * @return 
     */
    @Override
    public JComponent buildDrawnComponent(){
        
        // Override paintComponent method to draw segments and points
        JComponent returnComponent = new JComponent() {
            @Override
            public void paintComponent(Graphics graphic){
                Graphics2D graphic2D = (Graphics2D) graphic;
                RenderingHints rendering2D = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphic2D.setRenderingHints(rendering2D);
                graphic2D.setBackground(Color.BLACK);
                
                // First loop variables (segment drawing)
                double x1,y1,x2,y2;
                Line2D scaledLine = new Line2D.Double();
                Line2D segmentToGet = null;
                
                // Segment list clone
                List<Line2D> tempSegmentList = visualGraph.getSegmentList();
                
                graphic2D.setPaint(Color.WHITE);
                for (int i = 0; i < tempSegmentList.size(); i++){
                    
                    // Get next segment from our graph's segment list
                    segmentToGet = null;
                    while(segmentToGet == null){
                        try {
                            segmentToGet = tempSegmentList.get(i);
                        } 
                        catch (IndexOutOfBoundsException e){
                            // Do nothing, as this is a bengin inheritance bug
                            System.out.println("Segment " + i + " inheritance bug.");
                        }
                    }
                    
                    // Scale coords by 500, keep double precision
                    x1 = (segmentToGet.getX1()*500);
                    y1 = (segmentToGet.getY1()*500);
                    x2 = (segmentToGet.getX2()*500);
                    y2 = (segmentToGet.getY2()*500);
                    
                    // Make a new line
                    scaledLine.setLine(x1, y1, x2, y2);
                    
                    // Draw our new line segment
                    graphic2D.draw(scaledLine);
                }
                
                // Second loop variables (vertex list)
                double x,y;
                Ellipse2D vertexPoint;
                GraphColorVertex vertexToGet = null;
                
                // Vertex list clone
                List<GraphColorVertex> tempVertexList = visualGraph.getVertices();
                
                graphic2D.setPaint(Color.RED);
                for (int i = 0; i < tempVertexList.size(); i++){
                   
                    // Get next vertex from graph's vertex list
                    vertexToGet = null;
                    while(vertexToGet == null){
                        // Get next segment from our graph's segment list
                        try {
                            vertexToGet = (tempVertexList.get(i));
                        } 
                        catch (IndexOutOfBoundsException e){
                            // Do nothing, as this is a bengin inheritance bug
                            System.out.println("Vertex " + i + " inheritance bug.");
                        }
                    }
                    
                    // Scale coords by 500
                    x = (vertexToGet.getXPos()*500);
                    y = (vertexToGet.getYPos()*500);
                    
                    // Make a circle from coords
                    vertexPoint = new Ellipse2D.Double(x-3, y-3, 6, 6);
                    
                    // UNIQUE LOGIC: Set color of vertex by vertex's current color
                    graphic2D.setPaint(vertexToGet.getColor());
                    
                    // Draw our vertex circle
                    graphic2D.fill(vertexPoint);
                }
            }
        };
        returnComponent.setBackground(Color.WHITE);
        
        return returnComponent;
    }
}

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

import graph.ParentGraph;
import graph.ParentVertex;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Default methods and values for Visualizer class objects
 * @author Matthew Rohrlach
 */
public class ParentVisualizer {
    
    // This visualizer's target graph to display
    protected ParentGraph visualGraph;
    
    // This visualizer's display JFrame
    protected JFrame visualFrame = new JFrame();
    protected JPanel visualPanel;
    protected JComponent visualComponent;
    
    // Time to sleep in miliseconds
    protected long sleepTime = 1000;
    
    // Counter for repainting
    protected int updateCount = 0;
    
    /**
     * Regular constructor, takes a graph to display
     * @param visualGraphIn 
     */
    public ParentVisualizer(ParentGraph visualGraphIn){
        this.visualGraph = visualGraphIn;
    }
    
    /**
     * Regular constructor, takes a graph to display and length of sleep time
     * @param visualGraphIn
     * @param sleepTimeIn
     */
    public ParentVisualizer(ParentGraph visualGraphIn, long sleepTimeIn){
        this.visualGraph = visualGraphIn;
        this.sleepTime = sleepTimeIn;
    }
    
    /**
     * Build the visualizer's frame, complete with loading text
     */
    public void startVisualizer(){
        
        // Wipe visualizer frame and start over
        visualFrame.dispose();
        visualFrame = buildVisualizerFrame("Visualizer");
        
        // Add loading text (this would be blank, otherwise)
        visualPanel = buildVisualizerPanel();
        JLabel loadingText = new GUI().buildDefaultHeader();
        loadingText.setText("Loading...");
        visualPanel.add(loadingText);
        
        // Add ensemble to frame
        visualFrame.add(visualPanel);
        visualFrame.validate();
        visualFrame.setVisible(true);
    }
    
    /**
     * Tell the visualizer to dispose of the visualizer frame
     */
    public void endVisualizer(){
        visualFrame.dispose();
    }
    
    /**
     * Replaces existing JPanel with a drawing of the given graph
     * @param newVisualGraph 
     */
    public void drawVisualizer(ParentGraph newVisualGraph){
        
        if (isRunning()){
            // Replace old graph object, remove old panel
            this.visualGraph = newVisualGraph;
            visualFrame.remove(visualPanel);

            // Re-make graph and drawings
            visualPanel = buildVisualizerPanel();
            visualComponent = buildDrawnComponent();
            visualComponent.setDoubleBuffered(true);
            visualPanel.add(visualComponent);

            // Add back graph and drawings
            visualFrame.add(visualPanel);
            visualFrame.validate();
            
            try {
                Thread.sleep(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(ParentVisualizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Tells Visualizer to repaint the drawn component, or produce it if need be
     * @param newVisualGraph
     */
    public void updateVisualizer(ParentGraph newVisualGraph){
        if (visualComponent == null || (newVisualGraph)!=(visualGraph)){
            
            try {
                Thread.sleep(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(ParentVisualizer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            drawVisualizer(newVisualGraph);
        }
        else if (updateCount >= 1000){
            visualComponent.repaint();
            visualComponent.revalidate();
            updateCount = 0;
        }
        else{
            visualComponent.repaint();
            updateCount++;
        }
        
        try {
                if (sleepTime != 0) {
                    Thread.sleep(sleepTime);
                }
            }   
        catch (InterruptedException ex) {
            Logger.getLogger(ParentVisualizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Build Visualizer frame with default attributes
     * @param boxName
     * @return 
     */
    public JFrame buildVisualizerFrame(String boxName){
        
        JFrame returnFrame = new JFrame(boxName);
        returnFrame.setSize(615, 650);
        returnFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        returnFrame.setLocation(200, 200);
        returnFrame.setResizable(false);
        returnFrame.setAlwaysOnTop(true);
        
        return returnFrame;
    }
    
    /**
     * Build Visualizer panel with default attributes
     * @return 
     */
    public JPanel buildVisualizerPanel(){
        
        JPanel returnPanel = new JPanel(null);
        returnPanel.setLayout(new BoxLayout(returnPanel, BoxLayout.Y_AXIS));
        returnPanel.setBorder(BorderFactory.createEmptyBorder(50,50,50,50));
        returnPanel.setBackground(Color.BLACK);
        
        return returnPanel;
    }
        
    /**
     * Build Visualizer component with line segments and vertex points
     * @return 
     */
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
                ParentVertex vertexToGet = null;
                
                // Vertex list clone
                List<ParentVertex> tempVertexList = visualGraph.getVertices();
                
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
                    
                    // Draw our vertex circle
                    graphic2D.fill(vertexPoint);
                }
            }
        };
        returnComponent.setBackground(Color.WHITE);
        
        return returnComponent;
    }
    
    public void updateVisualizerTitle(String newTitle){
        if (visualFrame != null){
            visualFrame.setTitle(newTitle);
        }
    }
    
    /**
     * Returns the status of the visualizer window
     * @return true if frame is active
     */
    public boolean isRunning(){
        if (visualFrame != null){
            return (visualFrame.isVisible());
        }
        return false;
    }
    
    /**
     * Adjust sleep duration
     * @param sleepTimeIn 
     */
    public void setSleepTime(long sleepTimeIn){
        sleepTime = sleepTimeIn;
    }
}

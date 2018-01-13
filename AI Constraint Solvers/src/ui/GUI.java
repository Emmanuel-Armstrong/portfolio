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

import client.ProjectOne;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.IndexOutOfBoundsException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;

/**
 * Methods and parameters for GUI objects
 * @author Matthew Rohrlach
 */
public class GUI {
    
    GraphColorVisualizer newVisualizer;
    final GUI thisGUI = this;
    
    // Frame that contains all interactable objects, recreated when switching menus
    protected JFrame controlFrame;
    protected JPanel controlPanel;
    protected JScrollPane scrollOutput;
    
    // Frame that contains the display scene (graphs, etc)
    protected JFrame sceneFrame;
    
    // Objects that all frames have a version of
    protected JLabel header;
    
    // Values that will be used to recreate UI objects with chosen settings
    protected Boolean threeColorSelected;
    protected Boolean fourColorSelected;
    protected Boolean visualizerSelected;
    protected Boolean soundSelected;
    
    // Sound sequencer variables
    protected Sequencer soundSequencer;
    protected List<String> soundList;
    protected int currentSongNumber;
    
    // Constructor
    public GUI() {
        
        // Initialize arrayList
        controlFrame = new JFrame();
        
        // Create list of sound files
        currentSongNumber = 7;
        soundList = Arrays.asList("/sounds/sound1.mid", 
            "/sounds/sound2.mid", "/sounds/sound3.mid", "/sounds/sound4.mid",
            "/sounds/sound5.mid", "/sounds/sound6.mid", "/sounds/sound7.mid",
            "/sounds/sound8.mid", "/sounds/sound9.mid");
        
        // Set default values for objects
        threeColorSelected = true;
        fourColorSelected = true;
        visualizerSelected = true;
        soundSelected = false;
    }
    
    /**
     * This will reinitialize the control frame from scratch
     */
    public void rebuildFrame(){
        
        // Re-initialize frame with default attributes
        controlFrame.dispose();
        controlFrame = buildDefaultFrame("CSCI 446 - A.I. - Montana State University");
        
        // Initialize header with default attributes
        header = buildDefaultHeader();
        
        // Initialize control panel frame with default attributes
        controlPanel = buildDefaultPanel(BoxLayout.Y_AXIS);
        
        // Combine objects
        controlPanel.add(header, BorderLayout.NORTH);
        controlFrame.add(controlPanel);
        
    }
    
    /**
     * Build frame with default attributes
     * @param boxName
     * @return 
     */
    public JFrame buildDefaultFrame(String boxName){
        JFrame returnFrame = new JFrame(boxName);
        returnFrame.setBackground(Color.BLACK);
        returnFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        returnFrame.setLocationRelativeTo(null);
        returnFrame.setResizable(false);
        
        return returnFrame;
    }
    
    /**
     * Build header with default attributes
     * @return 
     */
    public JLabel buildDefaultHeader(){
        JLabel returnHeader = new JLabel();
        returnHeader.setHorizontalAlignment(SwingConstants.LEFT);
        returnHeader.setFont(new Font("Comic Sans MS", Font.PLAIN, 48)); // Great font, makes it pop
        returnHeader.setForeground(Color.WHITE);
        returnHeader.setBackground(Color.BLACK);
        
        return returnHeader;
    }
    
    /**
     * Build panel with default attributes and given layout axis
     * @param layoutAxis (BoxLayout.*)
     * @return 
     */
    public JPanel buildDefaultPanel(int layoutAxis){
        JPanel returnPanel = new JPanel(null);
        returnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        returnPanel.setLayout(new BoxLayout(returnPanel, layoutAxis));
        returnPanel.setBackground(Color.BLACK);
        
        return returnPanel;
    }
    
    /**
     * Build "vertically-scrolling text area for output purposes" with default attributes
     * @return 
     */
    public JScrollPane buildDefaultOutputBox(){
        JTextArea outputBox = new JTextArea(10, 15);
        outputBox.setEditable(false);
        outputBox.setLineWrap(true);
        outputBox.setMargin( new Insets(10,10,10,10) );
        DefaultCaret outputCaret = (DefaultCaret)outputBox.getCaret();
        outputCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        //outputBox.setFont(new Font("Papryus", 14, Font.PLAIN));
        JScrollPane scrollReturn = new JScrollPane(outputBox);
        scrollReturn.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        return scrollReturn;
    }
    
    /**
     * Build input text field with default attributes and given title
     * @param title
     * @return 
     */
    public JPanel buildDefaultInputPanel(String title){
        JLabel inputLabel = new JLabel(title);
        JTextField inputField = new JTextField();
        JPanel inputPanel = buildDefaultPanel(BoxLayout.X_AXIS);
        
        inputLabel.setFont(new Font("Comic Sans MS", Font.PLAIN, 12)); // Great font, makes it pop
        inputLabel.setForeground(Color.WHITE);
        inputField.setHorizontalAlignment(JTextField.CENTER);
        
        inputPanel.add(inputLabel);
        inputPanel.add(Box.createRigidArea(new Dimension(10,0)));
        inputPanel.add(inputField);
        inputPanel.add(Box.createRigidArea(new Dimension(15,0)));
        
        return inputPanel;
    }
    
    /**
     * Build check box with default attributes
     * @param boxText
     * @param isSelected
     * @return 
     */
    public JCheckBox buildDefaultCheckBox(String boxText, boolean isSelected){
        JCheckBox returnCheckBox = new JCheckBox(boxText, isSelected);
        
        return returnCheckBox;
    }
    
    /**
     * Builds the main window for choosing a project
     */
    public void buildMainGUI(){
        // Clear the frame
        rebuildFrame();
        
        // Header options in main GUI
        header.setText(" Problem choice: ");
        
        //------Unique panel objects here---------
        
        JButton project1Button = new JButton("Project 1 - Search, Constraint Satisfaction, and Graph Coloring");
        project1Button.addActionListener((ActionEvent e) -> {
            
            // If pressed, build project 1 GUI
            buildProject1GUI();
            
        });
        controlPanel.add(project1Button);
        
        
        //----------------------------------------
        
        // Display GUI after finished building
        controlFrame.pack();
        controlFrame.setVisible(true);
    }
    
    /**
     * Builds the window for testing functions in project 1
     * 
     * Header, output box, 3 and 4 color checkboxes, visualizer checkbox, 
     * sound checkbox, change sound button, number of graphs field,
     * number of tries field, visualizer speed field, visualizer speed update
     * button, run all tests button
     */
    public void buildProject1GUI(){
        
        // Clear the frame
        rebuildFrame();
        
        // Check for enabled sound
        if (soundSelected) {
            if (soundSequencer != null) {
                    soundSequencer.start();
                }
                else {
                    playSound(null);
                }
        }
        
        // Header options in Project 1 GUI
        header.setText(" A.I. Project 1: ");
        
        //------Unique panel objects here---------
        
        // Gap between title and output box
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Output box for function reports
        scrollOutput = buildDefaultOutputBox();
        controlPanel.add(scrollOutput, 2);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // 3 and 4 Color Checkboxes (Both selected = two sets of tests)
        JCheckBox threeColor = buildDefaultCheckBox("Three-color tests ", threeColorSelected);
        threeColor.addActionListener((ActionEvent e) -> {
            
            // Toggle status of three color testing
            if (threeColorSelected) {
                threeColorSelected = false;
            }
            else {
                threeColorSelected = true;
            }
            
        });
        JCheckBox fourColor = buildDefaultCheckBox("Four-color tests ", fourColorSelected);
        fourColor.addActionListener((ActionEvent e) -> {
            
            // Toggle status of four color testing
            if (fourColorSelected) {
                fourColorSelected = false;
            }
            else {
                fourColorSelected = true;
            }
            
        });
        JCheckBox visualizer = buildDefaultCheckBox("Visualizer ", visualizerSelected);
        visualizer.addActionListener((ActionEvent e) -> {
            
            // Toggle status of visualizer
            if (visualizerSelected) {
                visualizerSelected = false;
            }
            else {
                visualizerSelected = true;
            }
            
        });
        JCheckBox sound = buildDefaultCheckBox("Sound ", soundSelected);
        sound.addActionListener((ActionEvent e) -> {
            
            // Toggle status of sound playback
            if (soundSelected) {
                soundSelected = false;
                if (soundSequencer != null) {
                    soundSequencer.setTickPosition(0);
                    soundSequencer.stop();
                }
            }
            else {
                soundSelected = true;
                if (soundSequencer != null) {
                    soundSequencer.start();
                }
                else {
                    playSound(null);
                }
            }
            
        });
        JButton soundSwapButton = new JButton(" >> ");
        soundSwapButton.addActionListener((ActionEvent e) -> {
            
            // Switch between sounds
            if (soundSelected){
                swapSound(null);
            }
            
        });
        
        JPanel checkboxPanel = buildDefaultPanel(BoxLayout.X_AXIS);
        
        checkboxPanel.add(threeColor);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(fourColor);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(visualizer);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(sound);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(soundSwapButton);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        
        controlPanel.add(checkboxPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // "Number of graphs" text field and label
        JPanel numGraphsPanel = buildDefaultInputPanel("Number of graphs? ");
        ((JTextField)numGraphsPanel.getComponent(2)).setText("10");
        controlPanel.add(numGraphsPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // "Number of tries" text field and label
        JPanel numTriesPanel = buildDefaultInputPanel("Number of tries per graph? ");
        ((JTextField)numTriesPanel.getComponent(2)).setText("6000");
        controlPanel.add(numTriesPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Visualizer speed control text field, label, and update button
        JPanel visualizerSpeedPanel = buildDefaultInputPanel("Visualizer speed (ms)? ");
        ((JTextField)visualizerSpeedPanel.getComponent(2)).setText("0");
        JButton visualizerSpeedUpdateButton = new JButton("Update");
        visualizerSpeedUpdateButton.addActionListener((ActionEvent e) -> {
            
            // Update visualizer render speed
            ((JTextArea)scrollOutput.getViewport().getView()).append("\nReal-time speed adjustment not currently supported!");
            // testVisual.setSleepTime(Long.parseLong(((JTextField)visualizerSpeedPanel.getComponent(2)).getText()));
            
        });
        visualizerSpeedPanel.add(visualizerSpeedUpdateButton);
        visualizerSpeedPanel.add(Box.createRigidArea(new Dimension(15,0)));
        controlPanel.add(visualizerSpeedPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Generate graphs button
        JPanel solveButtonPanel = new JPanel();
        JButton solveConstraintsButton = new JButton("Take parameters and solve! ");
        solveConstraintsButton.setBackground(new Color(0,100,0));
        solveConstraintsButton.addActionListener((ActionEvent e) -> {
            
            //========Take parameters and run the project==========
            
            // Visual effect to show progress
            solveConstraintsButton.setBackground(Color.GREEN);
            
            // Default argument values
            int numGraphsArgument;
            int numTriesArgument;
            Long visualizerSpeedArgument;
            
            // Parse textfields to arguments-to-be-passed
            try {
                numGraphsArgument = Integer.parseInt(((JTextField)numGraphsPanel.getComponent(2)).getText());
                numTriesArgument = Integer.parseInt(((JTextField)numTriesPanel.getComponent(2)).getText());
                visualizerSpeedArgument = Long.parseLong(((JTextField)visualizerSpeedPanel.getComponent(2)).getText());
                
                System.out.println("Three colors: "+threeColorSelected);
                System.out.println("Four colors: "+fourColorSelected);
                System.out.println("Visualizer: "+visualizerSelected);
                System.out.println("Sound: "+soundSelected);
                System.out.println("Number of graphs: "+numGraphsArgument);
                System.out.println("Number of tries: "+numTriesArgument);
                System.out.println("Visualizer speed: "+visualizerSpeedArgument);
                System.out.println();
                
                // Call logic handler with arguments
                
                // Call must be made in a new thread
                Thread projectThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                       new ProjectOne(visualizerSelected, visualizerSpeedArgument, numGraphsArgument, 
                        numTriesArgument, thisGUI, threeColorSelected, fourColorSelected).solve();
                    }

                   });
                projectThread.start();
            } 
            catch (NumberFormatException ex) {
                System.out.println("Bad input! Please use integer values for text fields.\n");
            }
            
            
            
            
        });
        
        solveButtonPanel.add(solveConstraintsButton);
        solveButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        solveButtonPanel.setBackground(Color.BLACK);
        solveButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPanel.add(solveButtonPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        
        //----------------------------------------
        
        // Display GUI after finished building
        controlFrame.pack();
        controlFrame.setVisible(true);
    }
    
    /**
     * Plays sound of given .mid file, or a random sound if none given
     * Initializes sound sequencer if necessary
     * @param fileName
     */
    public void playSound(String fileName){
        
        Random rand = new Random();
        if (fileName == null) {
            int nextSongNumber  = rand.nextInt(soundList.size());
            while(nextSongNumber == currentSongNumber){
                nextSongNumber = rand.nextInt(soundList.size());
            }
            currentSongNumber = nextSongNumber;
            fileName = soundList.get(currentSongNumber);
        }
        
        try {
            
            
            soundSequencer = MidiSystem.getSequencer();
            soundSequencer.open();
            
            soundSequencer.setSequence(this.getClass().getResourceAsStream(fileName));
            soundSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            soundSequencer.start();
            
        } catch (IOException | InvalidMidiDataException | MidiUnavailableException ex) {
            //Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Swaps to a given sound or randomly chooses a new sound
     * @param fileName 
     */
    public void swapSound(String fileName){
        Random rand = new Random();
        if (fileName == null) {
            int nextSongNumber  = rand.nextInt(soundList.size());
            while(nextSongNumber == currentSongNumber){
                nextSongNumber = rand.nextInt(soundList.size());
            }
            currentSongNumber = nextSongNumber;
            fileName = soundList.get(currentSongNumber);
        }
        
        if (soundSequencer == null) {
            playSound(fileName);
        }
        else {
            try {
                soundSequencer.setTickPosition(0);
                soundSequencer.stop();
                soundSequencer.setSequence(this.getClass().getResourceAsStream(fileName));
                soundSequencer.start();
            } catch (IOException | InvalidMidiDataException ex) {
                //Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Provide access for printing to control panel output box
     * @param stringToPrint
     */
    public void printToOutputBox(String stringToPrint) {
        try {
            ((JTextArea)scrollOutput.getViewport().getView()).append("" + stringToPrint+"\n");
        }
        catch (NullPointerException | IndexOutOfBoundsException e) {
            // Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Template for adding new GUI menus (SHOULD NOT BE CALLED)
     */
    @Deprecated
    public void buildTemplateGUI(){
        // Clear the frame
        rebuildFrame();
        
        // Header options in [OBJECT] GUI
        header.setText("Descriptive button header: ");
        
        //------Unique panel objects here---------
        
        
        //----------------------------------------
        
        // Display GUI after finished building
        controlFrame.pack();
        controlFrame.setVisible(true);
    }
}

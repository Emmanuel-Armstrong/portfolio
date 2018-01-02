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

import client.ProjectTwo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
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
import javax.swing.JComboBox;
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
    protected Boolean visualizerSelected;
    protected Boolean soundSelected;
    protected Boolean ensureSolvabilitySelected;
    protected Boolean haltProject;
    protected long visualizerSpeedArgument;
    
    // Sound sequencer variables
    protected Sequencer soundSequencer;
    protected List<String> soundList;
    protected int currentSongNumber;
    
    // Constructor
    public GUI() {
        
        // Initialize arrayList
        controlFrame = new JFrame();
        
        // Create list of sound files
        currentSongNumber = 3;
        soundList = Arrays.asList("/sounds/hallofthemountainking.mid", 
            "/sounds/marchofthetrolls.mid", "/sounds/raindrops.mid",
            "/sounds/rideofthevalkryies.mid");
        
        // Set default values for objects
        visualizerSelected = true;
        ensureSolvabilitySelected = false;
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
     * Builds the main window for choosing a project
     */
    public void buildMainGUI(){
        // Clear the frame
        rebuildFrame();
        
        // Header options in main GUI
        header.setText(" Problem choice: ");
        
        //------Unique panel objects here---------
        
        JButton project2Button = new JButton("        Project 2 - Logical Inference and the Wumpus World        ");
        project2Button.addActionListener((ActionEvent e) -> {
            
            // If pressed, build project 2 GUI
            buildProject2GUI();
            
        });
        controlPanel.add(project2Button);
        
        
        //----------------------------------------
        
        // Display GUI after finished building
        controlFrame.pack();
        controlFrame.setVisible(true);
    }
    
    /**
     * Builds the window for testing functions in project 2
     * 
     * Header, output box, visualizer checkbox, 
     * sound checkbox, change sound button, grid size combo box,
     * number of tries field, visualizer speed field, visualizer speed update
     * button, start button, stop button
     */
    public void buildProject2GUI(){
        
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
        header.setText(" A.I. Project 2: ");
        
        //------Unique panel objects here---------
        
        // Gap between title and output box
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Output box for function reports
        scrollOutput = buildDefaultOutputBox();
        controlPanel.add(scrollOutput, 2);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Checkboxes
        
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
        JCheckBox solvability = buildDefaultCheckBox("Ensure solvability? ", soundSelected);
        solvability.addActionListener((ActionEvent e) -> {
            
            // Toggle status of solvability
            if (ensureSolvabilitySelected) {
                ensureSolvabilitySelected = false;
            }
            else {
                ensureSolvabilitySelected = true;
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
        
//        checkboxPanel.add(visualizer);
//        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(solvability);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(sound);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        checkboxPanel.add(soundSwapButton);
        checkboxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        
        controlPanel.add(checkboxPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Add drop-down combo box for grid-size selection
        
        JPanel comboBoxPanel = buildDefaultPanel(BoxLayout.X_AXIS);
        
        JLabel comboLabel = buildDefaultLabel("World square dimension: ");
        comboBoxPanel.add(comboLabel);
        comboBoxPanel.add(Box.createRigidArea(new Dimension(10,0)));
        String[] boxEntries = {"5","10","15","20","25","All"};
        comboBoxPanel.add(buildDefaultComboBox(boxEntries));
        comboBoxPanel.add(Box.createRigidArea(new Dimension(15,0)));
        controlPanel.add(comboBoxPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        JPanel comboBoxPanelDecision = buildDefaultPanel(BoxLayout.X_AXIS);
        
        JLabel comboLabelDecision = buildDefaultLabel("Decision Maker: ");
        comboBoxPanelDecision.add(comboLabelDecision);
        comboBoxPanelDecision.add(Box.createRigidArea(new Dimension(10,0)));
        String[] boxEntriesDecision = {"REASONING","REACTIONARY","All"};
        comboBoxPanelDecision.add(buildDefaultComboBox(boxEntriesDecision));
        comboBoxPanelDecision.add(Box.createRigidArea(new Dimension(15,0)));
        controlPanel.add(comboBoxPanelDecision);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        
        // "Number of worlds" text field and label
        JPanel numWorldsPanel = buildDefaultInputPanel("Number of Wumpus worlds? ");
        ((JTextField)numWorldsPanel.getComponent(2)).setText("10");
        controlPanel.add(numWorldsPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // "Number of actions" text field and label
        JPanel numActionsPanel = buildDefaultInputPanel("Max actions per world? ");
        ((JTextField)numActionsPanel.getComponent(2)).setText("Default");
        controlPanel.add(numActionsPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // "Probability of bottomless pit" text field and label
        JPanel pitProbPanel = buildDefaultInputPanel("Probability of bottomless pit (percent)? ");
        ((JTextField)pitProbPanel.getComponent(2)).setText("10");
        controlPanel.add(pitProbPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // "Probability of obstacle" text field and label
        JPanel obstacleProbPanel = buildDefaultInputPanel("Probability of obstacle (percent)? ");
        ((JTextField)obstacleProbPanel.getComponent(2)).setText("10");
        controlPanel.add(obstacleProbPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // "Probability of wumpus" text field and label
        JPanel wampusProbPanel = buildDefaultInputPanel("Probability of Wumpus (percent)? ");
        ((JTextField)wampusProbPanel.getComponent(2)).setText("10");
        controlPanel.add(wampusProbPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        
        // Visualizer speed control text field, label, and update button
        JPanel visualizerSpeedPanel = buildDefaultInputPanel("Visualizer speed (ms)? ");
        ((JTextField)visualizerSpeedPanel.getComponent(2)).setText("0");
        JButton visualizerSpeedUpdateButton = new JButton("Update");
        visualizerSpeedUpdateButton.addActionListener((ActionEvent e) -> {
            
            // Update visualizer render speed
            visualizerSpeedArgument = (Long.parseLong(((JTextField)visualizerSpeedPanel.getComponent(2)).getText()));
            
        });
        visualizerSpeedPanel.add(visualizerSpeedUpdateButton);
        visualizerSpeedPanel.add(Box.createRigidArea(new Dimension(15,0)));
//        controlPanel.add(visualizerSpeedPanel);
//        controlPanel.add(Box.createRigidArea(new Dimension(0,10)));
        
        // Generate graphs button
        JPanel solveButtonPanel = new JPanel();
        JButton solveConstraintsButton = new JButton("Take parameters and solve! ");
        JButton stopProjectTwoButton = new JButton("Halt at next iteration. ");
        solveConstraintsButton.setBackground(new Color(0,100,0));
        solveConstraintsButton.addActionListener((ActionEvent e) -> {
            
            //========Take parameters and run the project==========
            
            // Visual effect to show progress
            solveConstraintsButton.setBackground(Color.GREEN);
            stopProjectTwoButton.setBackground(new Color(100,0,0));
            haltProject = false;
            
            // Default argument values
            String worldSquareDimension;
            String decisionMaker;
            int numWorldsArgument;
            String numActionsArgument;
            int pitProbabilityArgument;
            int obstacleProbabilityArgument;
            int wumpusProbabilityArgument;
            
            
            // Parse textfields to arguments-to-be-passed
            try {
                worldSquareDimension = (String)((JComboBox)(comboBoxPanel.getComponent(2))).getSelectedItem();
                decisionMaker = (String)((JComboBox)(comboBoxPanelDecision.getComponent(2))).getSelectedItem();
                numWorldsArgument = Integer.parseInt(((JTextField)numWorldsPanel.getComponent(2)).getText());
                
                numActionsArgument = (((JTextField)numActionsPanel.getComponent(2)).getText());
                if (!numActionsArgument.equals("Default")) {
                    int numActionsTest = Integer.parseInt(numActionsArgument);
                }
                
                pitProbabilityArgument = Integer.parseInt(((JTextField)pitProbPanel.getComponent(2)).getText());
                obstacleProbabilityArgument = Integer.parseInt(((JTextField)obstacleProbPanel.getComponent(2)).getText());
                wumpusProbabilityArgument = Integer.parseInt(((JTextField)wampusProbPanel.getComponent(2)).getText());
//                visualizerSpeedArgument = Long.parseLong(((JTextField)visualizerSpeedPanel.getComponent(2)).getText());
                
//                printToOutputBox("Visualizer: "+visualizerSelected);
                printToOutputBox("Ensure solvability: "+ensureSolvabilitySelected);
                printToOutputBox("Sound: "+soundSelected);
                printToOutputBox("World square dimensions: "+worldSquareDimension+" x "+worldSquareDimension);
                printToOutputBox("Decision maker chosen: "+decisionMaker);
                printToOutputBox("Number of worlds: "+numWorldsArgument);
                printToOutputBox("Max actions per world: "+numActionsArgument);
                printToOutputBox("Probability of bottomless pit appearance: "+pitProbabilityArgument+" percent");
                printToOutputBox("Probability of obstacle appearance: "+obstacleProbabilityArgument+" percent");
                printToOutputBox("Probability of Wumpus appearance: "+wumpusProbabilityArgument+" percent");
//                printToOutputBox("Visualizer speed: "+visualizerSpeedArgument+"\n");
                
                // Call logic handler with arguments
                
                // Call must be made in a new thread
                Thread projectThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        
                        new ProjectTwo(thisGUI, decisionMaker, worldSquareDimension, wumpusProbabilityArgument,
                        pitProbabilityArgument, obstacleProbabilityArgument, numWorldsArgument, numActionsArgument,
                        ensureSolvabilitySelected);
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
        
        stopProjectTwoButton.setBackground(new Color(100,0,0));
        stopProjectTwoButton.addActionListener((ActionEvent e) -> {
            solveConstraintsButton.setBackground(new Color(0,100,0));
            stopProjectTwoButton.setBackground(Color.RED);
            haltProject = true;
        
        });
        
        solveButtonPanel.add(stopProjectTwoButton);
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
        
        JLabel inputLabel = buildDefaultLabel(title);
        JTextField inputField = new JTextField();
        JPanel inputPanel = buildDefaultPanel(BoxLayout.X_AXIS);
        
        inputPanel.add(inputLabel);
        inputPanel.add(Box.createRigidArea(new Dimension(10,0)));
        inputPanel.add(inputField);
        inputPanel.add(Box.createRigidArea(new Dimension(15,0)));
        
        return inputPanel;
    }
    
    /**
     * Build label with default attributes
     * @param labelText
     * @return 
     */
    public JLabel buildDefaultLabel(String labelText){
        
        JLabel returnLabel = new JLabel(labelText);
        returnLabel.setFont(new Font("Comic Sans MS", Font.PLAIN, 12)); // Great font, makes it pop
        returnLabel.setForeground(Color.WHITE);
        returnLabel.setHorizontalAlignment(JTextField.CENTER);
        
        return returnLabel;
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
     * Build combo box with default attributes
     * @param boxEntries
     * @return 
     */
    public JComboBox buildDefaultComboBox(String[] boxEntries){
        JComboBox returnComboBox = new JComboBox(boxEntries);
        
        return returnComboBox;
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
     * Logic handler will call this method and update its visualizer speed on the fly
     * @return 
     */
    public long getVisualizerSpeedArgument() {
        return visualizerSpeedArgument;
    }
    
    /**
     * Logic handler will call this method and halt if necessary
     * @return 
     */
    public boolean getHaltStatus() {
        return haltProject;
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
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
package data_components;

import java.io.Serializable;

/**
 *
 * @author nwmoore
 */
public class DataPoint implements Serializable{
    private String[] dataPoint;
    
    /**
     * Main constructor to create a DataPoint
     * @param dataIn
     */
    public DataPoint(String[] dataIn) {
        dataPoint = dataIn;
    }
    
    /**
     * Prints features and class of DataPoint
     */
    public void printPoint() {
        for (int i = 0; i < dataPoint.length; i++) {
            String feature = dataPoint[i];
            
            System.out.print(feature);
            if (i < dataPoint.length - 1) {
                System.out.print(", ");
            }
        }
    }
    
    /**
     * Returns the features of the DataPoint
     * @return 
     */
    public String[] getFeatures() {
        return dataPoint;
    }
}

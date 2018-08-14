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
package data_components.knn;

import data_components.DataPoint;

/**
 *
 * @author Emmanuel Armstrong & Nicolas Moore
 */

//Class to create list of results consisting of a distance value and a datapoint.
public class Result{
    protected double distance;
    protected DataPoint inData;
    
    /*
    * Contruct the Result
    */
    public Result(double distance, DataPoint inData){
        this.inData = inData;
        this.distance = distance;
    }
    
    /*
    * Get the DataPoint
    * @return
    */
    public DataPoint getDataPoint(){
        return inData;
    }
    
    /*
    * Get the distance
    * @return
    */
    public double getDistance() {
        return distance;
    }

    
    
}

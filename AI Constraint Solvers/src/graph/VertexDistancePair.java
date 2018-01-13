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
package graph;

/**
 * Simple object that stores a distance (double) / key (integer) pair
 * @author Matthew Rohrlach
 */
public class VertexDistancePair {
    
    public final double distance;
    public final int key;
    
    /**
     * Constructs the pairs that will fill the distance list of each vertex
     * @param keyIn 
     * @param distanceIn
     */
    public VertexDistancePair(int keyIn, double distanceIn) {
        this.key = keyIn;
        this.distance = distanceIn;
}
    
}

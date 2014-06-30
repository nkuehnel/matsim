/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.util.matrices;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.*;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;


public class MatrixUtils
{
    public static Matrices readMatrices(String file)
    {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Matrices matrices = new Matrices();
        new MatsimMatricesReader(matrices, scenario).readFile(file);
        return matrices;
    }
    
    
    public static Matrix createDenseMatrix(String id, Iterable<Id> ids, double[][] values)
    {
        return createMatrix(id, ids, values, true);
    }


    public static Matrix createSparseMatrix(String id, Iterable<Id> ids, double[][] values)
    {
        return createMatrix(id, ids, values, false);
    }


    public static Matrix createMatrix(String id, Iterable<Id> ids, double[][] values,
            boolean denseMatrix)
    {
        Matrix matrix = new Matrix(id, null);
        Id[] idArray = Iterables.toArray(ids, Id.class);

        for (int i = 0; i < idArray.length; i++) {
            for (int j = 0; j < idArray.length; j++) {
                if (denseMatrix || values[i][j] != 0) {
                    matrix.createEntry(idArray[i], idArray[j], values[i][j]);
                }
            }
        }

        return matrix;
    }


    public static Matrix getOrCreateMatrix(Matrices matrices, String key)
    {
        Matrix matrix = matrices.getMatrix(key);

        if (matrix == null) {
            matrix = matrices.createMatrix(key + "", null);
        }

        return matrix;
    }


    public static void setOrIncrementValue(Matrix matrix, Id fromId, Id toId, double value)
    {
        Entry entry = matrix.getEntry(fromId, toId);

        if (entry == null) {
            matrix.createEntry(fromId, toId, value);
        }
        else {
            entry.setValue(entry.getValue() + value);
        }
    }


    public static double calculateTotalValue(Matrix matrix)
    {
        return calculateTotalValue(createEntryIterable(matrix));
    }


    public static double calculateTotalValue(Iterable<Entry> entries)
    {
        double total = 0;
        for (Entry e : entries) {
            total += e.getValue();
        }
        return total;
    }


    public static Iterable<Entry> createEntryIterable(Matrix matrix)
    {
        return Iterables.concat(matrix.getFromLocations().values());
    }


    public static Matrices aggregateMatrices(Matrices input, Function<? super String, String> keyAggregator)
    {
        Matrices output = new Matrices();

        for (Map.Entry<String, Matrix> mapEntry : input.getMatrices().entrySet()) {
            String toKey = keyAggregator.apply(mapEntry.getKey());
            Matrix matrix = MatrixUtils.getOrCreateMatrix(output, toKey);

            for (Entry e : MatrixUtils.createEntryIterable(mapEntry.getValue())) {
                MatrixUtils.setOrIncrementValue(matrix, e.getFromLocation(), e.getToLocation(),
                        e.getValue());
            }
        }

        return output;
    }
    
    
    public static void scaleMatrices(Matrices matrices, double factor)
    {
        for (Matrix m : matrices.getMatrices().values()) {
            for (Entry e : MatrixUtils.createEntryIterable(m)) {
                e.setValue(e.getValue() * factor);
            }
        }
    }
}
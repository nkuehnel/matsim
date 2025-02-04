/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.gsv.popsim.analysis.*;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class ActTypeDistanceTask extends AnalyzerTask {

    @Override
    public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
        Map<String, Predicate<Segment>> actTypePredicates = Predicates.actTypePredicates(persons);
        ModePredicate modePredicate = new ModePredicate(CommonValues.LEG_MODE_CAR);
        LegCollector distColletor = new LegCollector(new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE));

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/acttypedist.txt"));

            writer.write("type");
            for(double key = 0; key <= 1000000; key += 100000) {
                writer.write("\t");
                writer.write(String.valueOf(key));
            }
            writer.newLine();
        for(Map.Entry<String, Predicate<Segment>> entry : actTypePredicates.entrySet()) {
            LegPurposePredicate purposePredicate = new LegPurposePredicate(entry.getValue());
            PredicateAndComposite<Segment> pred = new PredicateAndComposite<>();
            pred.addComponent(modePredicate);
            pred.addComponent(purposePredicate);

            distColletor.setPredicate(pred);

            List<Double> dists = distColletor.collect(persons);
            double[] distArray = CollectionUtils.toNativeArray(dists);
            TDoubleDoubleHashMap hist = Histogram.createHistogram(distArray, new LinearDiscretizer(100000), false);

            writer.write(entry.getKey());

            for(double key = 0; key <= 1000000; key += 100000) {
                double val = hist.get(key);
                writer.write("\t");
                writer.write(String.valueOf(val));
            }
            writer.newLine();
        }
        writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

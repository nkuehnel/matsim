/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.gsv.popsim.analysis;

import playground.johannes.synpop.data.Person;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author jillenberger
 */
public class AnalyzerTaskRunner {

    private static final String format = "%.4f";

    private static final String nullString = "NA";

    private static final String TAB = "\t";

    public static void run(Collection<? extends Person> persons, AnalyzerTask<Collection<? extends Person>> task, FileIOContext context) {
        run(persons, task, String.format("%s/stats.txt", context.getPath()));
    }

    public static void run(Collection<? extends Person> persons, AnalyzerTask<Collection<? extends Person>> task, String file) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("dimension\tmean\tmin\tmax\tsize\tmedian\tvariance");
            writer.newLine();

            ArrayList<StatsContainer> containers = new ArrayList<>();
            task.analyze(persons, containers);

            for (StatsContainer container : containers) {
                writer.write(container.getName());
                writer.write(TAB);
                writer.write(doubleToString(container.getMean()));
                writer.write(TAB);
                writer.write(doubleToString(container.getMin()));
                writer.write(TAB);
                writer.write(doubleToString(container.getMax()));
                writer.write(TAB);
                writer.write(intToString(container.getN()));
                writer.write(TAB);
                writer.write(doubleToString(container.getMedian()));
                writer.write(TAB);
                writer.write(doubleToString(container.getVariance()));

                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String doubleToString(Double value) {
        if(value == null) return nullString;
        else return String.format(format, value);
    }

    private static String intToString(Integer value) {
        if(value == null) return nullString;
        else return String.format("%d", value);
    }
}

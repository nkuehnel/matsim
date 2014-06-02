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

package playground.michalm.zone;

import java.io.IOException;
import java.util.Map;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.*;

import com.vividsolutions.jts.geom.Polygon;


public class Zones
{
    public static Map<Id, Zone> readZones(Scenario scenario, String zonesXmlFile,
            String zonesShpFile)
        throws IOException
    {
        ZoneXmlReader xmlReader = new ZoneXmlReader(scenario);
        xmlReader.parse(zonesXmlFile);
        Map<Id, Zone> zones = xmlReader.getZones();

        ZoneShpReader shpReader = new ZoneShpReader(scenario, zones);
        shpReader.readZones(zonesShpFile);
        return zones;
    }


    public static void writeZones(Map<Id, Zone> zones, String coordinateSystem,
            String zonesXmlFile, String zonesShpFile)
    {
        new ZoneXmlWriter(zones).write(zonesXmlFile);
        new ZoneShpWriter(zones, coordinateSystem).write(zonesShpFile);
    }


    public static void transformZones(Map<Id, Zone> zones, String fromCoordSystem,
            String toCoordSystem)
    {
        MathTransform transform = getTransform(fromCoordSystem, toCoordSystem);

        for (Zone z : zones.values()) {
            z.setPolygon(transformPolygon(z.getPolygon(), transform));
        }
    }


    public static MathTransform getTransform(String fromCoordSystem, String toCoordSystem)
    {
        CoordinateReferenceSystem fromCrs = MGC.getCRS(fromCoordSystem);
        CoordinateReferenceSystem toCrs = MGC.getCRS(toCoordSystem);

        try {
            return CRS.findMathTransform(fromCrs, toCrs, true);
        }
        catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }


    public static Polygon transformPolygon(Polygon polygon, MathTransform transform)
    {
        try {
            return (Polygon)JTS.transform(polygon, transform);
        }
        catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }
}
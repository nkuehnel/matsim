package playground.balac.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.balac.retailers.data.FacilityRetailersImpl;
import playground.balac.retailers.data.LinkRetailersImpl;
import playground.balac.retailers.data.PersonPrimaryActivity;
import playground.balac.retailers.utils.Utils;

public class MaxPotentialCustomersModel extends RetailerModelImpl {

	  private TreeMap<Id, LinkRetailersImpl> availableLinks = new TreeMap<Id, LinkRetailersImpl>();
	  private static final Logger log = Logger.getLogger(MaxPotentialCustomersModel.class);

	public MaxPotentialCustomersModel(Controler controler, Map<Id, ActivityFacilityImpl> retailerFacilities)
	  {
	    this.controler = controler;
	    this.retailerFacilities = retailerFacilities;
	    this.controlerFacilities = this.controler.getFacilities();
	    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

	    for (Person p : controler.getPopulation().getPersons().values()) {
	      PersonImpl pi = (PersonImpl)p;
	      this.persons.put(pi.getId(), pi);
	    }
	  }

	  public void init(TreeMap<Integer, String> first)
	  {
	    this.first = first;

	    setInitialSolution(this.first.size());
	    log.info("Initial solution = " + getInitialSolution());
	    findScenarioShops(this.controlerFacilities.getFacilities().values());
	    Gbl.printMemoryUsage();
	   /* for (PersonImpl pi : this.persons.values()) {
	      PersonRetailersImpl pr = new PersonRetailersImpl(pi);
	      this.retailersPersons.put(pr.getId(), pr);
	    }*/
	    Utils.setPersonPrimaryActivityQuadTree(Utils.createPersonPrimaryActivityQuadTree(this.controler));
	    
	    //set inside and outside shops
	    
	    Utils.setShopsQuadTree(Utils.createShopsQuadTreeWIthoutRetailers(this.controler, this.retailerFacilities));
	    Utils.setInsideShopsQuadTree(Utils.createInsideShopsQuadTreeWIthoutRetailers(this.controler, this.retailerFacilities));
	    Utils.setOutsideShopsQuadTree(Utils.createOutsideShopsQuadTreeWIthoutRetailers(this.controler, this.retailerFacilities));

	    for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
	      String linkId = this.first.get(i);
	      //double scoreSum = 0.0D;
	      LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(new IdImpl(linkId)), this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	      Collection<PersonPrimaryActivity> primaryActivities = Utils.getPersonPrimaryActivityQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 3000.0D);
	      
	      
	    
	      
	      link.setScoreSum(1.0/(double)primaryActivities.size());
	      link.setPotentialCustomers(1.0/(double)primaryActivities.size());
	      this.availableLinks.put(link.getId(), link);
	    }
	  }

	  private int computePotentialCustomers() {
		  
		  int fitness = 0;
		  Collection<PersonPrimaryActivity> primaryActivities = Utils.getPersonPrimaryActivityQuadTree().values();
	     Network netowrk = controler.getNetwork();
	     TripRouterFactoryInternal  tripRouterFactory = controler.getTripRouterFactory();
			
			TripRouter tripRouter = tripRouterFactory.instantiateAndConfigureTripRouter();
			double time = System.currentTimeMillis();
			 // log.info(System.currentTimeMillis());
	      for (PersonPrimaryActivity ppa : primaryActivities) {
	      
			  Coord c = new CoordImpl(netowrk.getLinks().get(ppa.getActivityLinkId()).getCoord());
			  FacilityRetailersImpl af = new FacilityRetailersImpl(new IdImpl("010"), c, ppa.getActivityLinkId());
			  ActivityFacility af1 = Utils.getInsideShopsQuadTree().get(c.getX(), c.getY());
			  ActivityFacility af2 = Utils.getOutsideShopsQuadTree().get(c.getX(), c.getY());
			  
	    	  if (af1 instanceof FacilityRetailersImpl ||
	    	    	  af2 instanceof FacilityRetailersImpl){
	    		  
	    		  
	    			double travelTime1 = 0.0;
	    			for(PlanElement pe1: tripRouter.calcRoute("car", af,
	    					af1, 61200, null)) {
	    		    	
	    				if (pe1 instanceof Leg) {
	    					
	    		    			travelTime1 += ((Leg) pe1).getTravelTime();
	    				}
	    			}
	    			
	    			double travelTime2 = 0.0;
	    			for(PlanElement pe1: tripRouter.calcRoute("car", af, af2, 61200, null)) {
	    		    	
	    				if (pe1 instanceof Leg) {
	    					
	    		    			travelTime2 += ((Leg) pe1).getTravelTime();
	    				}
	    			}
	    			double d = MatsimRandom.getRandom().nextDouble();
	    			if (d < 0.47) {
	    			if (travelTime1 + 90 < travelTime2 && af1 instanceof FacilityRetailersImpl) {
	    				fitness++;
	    				log.info("inside car");
	    			}
	    			else if (travelTime1 + 90 > travelTime2 && af2 instanceof FacilityRetailersImpl)
	    				fitness++;
	    			}
	    			else if (0.47 <= d && d < 0.65) {
	    				if (travelTime1 + 45 > travelTime2 && af2 instanceof FacilityRetailersImpl) {
	    					fitness++;
	    					//log.info("inside pt");
	    				}
	    				else if (travelTime1 + 45 < travelTime2 && af1 instanceof FacilityRetailersImpl) {
	    					fitness++;
	    					log.info("inside pt");
	    				}
	    			}
	    			else {if (travelTime1 > travelTime2 && af2 instanceof FacilityRetailersImpl)
    					fitness++;
    				else if (travelTime1  < travelTime2 && af1 instanceof FacilityRetailersImpl)
    					fitness++;
	    			}
	    	  }
	    	  
	      }
	      log.info(time - System.currentTimeMillis());
		return fitness;
		  
	  }

	  @Override
		public double computePotential(ArrayList<Integer> solution) {
		  
		  Double Fitness = 0.0D;

		  ArrayList<FacilityRetailersImpl> temp = new ArrayList<FacilityRetailersImpl>();
		  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
			  String linkId = this.first.get(solution.get(s));
			  FacilityRetailersImpl af = new FacilityRetailersImpl(new IdImpl("010"), new CoordImpl(this.availableLinks.get(new IdImpl(linkId)).getCoord().getX(), this.availableLinks.get(new IdImpl(linkId)).getCoord().getY()), new IdImpl(linkId));
			  temp.add(af);
			  
			  Utils.addShopToShopsQuadTree(this.availableLinks.get(new IdImpl(linkId)).getCoord().getX(), this.availableLinks.get(new IdImpl(linkId)).getCoord().getY(), af);
		  }
		  Fitness = (double) computePotentialCustomers();
		  log.info(Fitness);
		  
		  for(FacilityRetailersImpl af : temp) {
			 Utils.removeShopFromShopsQuadTree(af.getCoord().getX(), af.getCoord().getY(), af);
			  //System.out.println();
		  }
		
		  return Fitness;
	  }

	  public Map<Id, ActivityFacilityImpl> getScenarioShops() {
	    return this.shops;
	  }

}
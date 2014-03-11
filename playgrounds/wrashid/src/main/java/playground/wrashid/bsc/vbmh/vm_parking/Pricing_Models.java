package playground.wrashid.bsc.vbmh.vm_parking;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class Pricing_Models {
	private static List<Parking_Pricing_Model> parkingprices = new LinkedList<Parking_Pricing_Model>();
	
	@XmlElement(name = "Parking_Pricing_Model")
	public List<Parking_Pricing_Model> getParking_Pricing_Models() {
		return parkingprices;
	}
	
	public void add(Parking_Pricing_Model model){
		parkingprices.add(model);

	}

	public Parking_Pricing_Model get_model(int model_id){
		// Alle in Map schreiben zum beschleunigen?
		for (Parking_Pricing_Model model : parkingprices){
			if (model.id==model_id){
				return model;	
			}
		}
		
		//nichts gefunden
		return null;
		
	}
	
	public double calculate_parking_price(double duration, boolean ev,int model_id){
		double price = 0;
		Parking_Pricing_Model model = get_model(model_id);
		if (ev){
			price = model.price_of_first_minute_ev + duration * model.price_per_minute_ev;
		} else {
			price = model.price_of_first_minute_nev + duration * model.price_per_minute_nev;
		}
			
		
		
		return price;
	}
	
	
}
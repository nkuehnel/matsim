package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class CompareSelectedParkingPropertyOneRun {

	/**
	 * 
	 * compare the iterations of a run to a reference iteration and to consecutive iterations. 
	 * 
	 *  As when private parking is used, the strategy (group) does not matter, it is also possible to ignore such parking.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputFolder = "H:/data/experiments/parkingSearchOct2013/runs/run143/output/";
		int referenceIteration = 499;
		int startIteration = 400;
		int endIteration = referenceIteration - 1;
		boolean ignoreCasesWithBothPPUse=true;

		StringMatrix eventsReferenceMatrix = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						referenceIteration));
		StringMatrix eventsMatrixCurrentIter = GeneralLib
				.readStringMatrix(getEventsFileName(outputFolder,
						startIteration));

		System.out
				.println("iteration\tpctDiffConsequete-FacilityId\tpctDiffReference-FacilityId\tpctDiffConsequete-parkingStrategy\tpctDiffReference-parkingStrategy\tpctDiffConsequete-groupName\tpctDiffReference-groupName");
		for (int i = startIteration; i <= endIteration; i++) {
			StringMatrix eventsMatrixNextIter = GeneralLib
					.readStringMatrix(getEventsFileName(outputFolder, i + 1));

			System.out.print(i
					+ "\t"
					+ percentageOfDifferentParkingUsages(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"FacilityId",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfDifferentParkingUsages(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "FacilityId",ignoreCasesWithBothPPUse));
			System.out.print("\t"
					+ percentageOfDifferentParkingUsages(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"parkingStrategy",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfDifferentParkingUsages(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "parkingStrategy",ignoreCasesWithBothPPUse));
			System.out.print("\t"
					+ percentageOfDifferentParkingUsages(
							eventsMatrixCurrentIter, eventsMatrixNextIter,
							"groupName",ignoreCasesWithBothPPUse)
					+ "\t"
					+ percentageOfDifferentParkingUsages(eventsReferenceMatrix,
							eventsMatrixCurrentIter, "groupName",ignoreCasesWithBothPPUse));
			System.out.println();

			eventsMatrixCurrentIter = eventsMatrixNextIter;
		}
	}

	public static String getEventsFileName(String outputFolder,
			int iterationNumber) {
		String fileName = outputFolder + "/ITERS/" + iterationNumber
				+ ".parkingEvents.txt.gz";
		return fileName;
	}

	public static double percentageOfDifferentParkingUsages(
			StringMatrix matrixA,
			StringMatrix matrixB, String fieldName, boolean ignoreCasesWithBothPPUse) {
		
		// persondId, legIndex, rowId
		TwoHashMapsConcatenated<Id, Integer, Integer> indexMatrixB=getIndex(matrixB);
		
		int indexPersonId=matrixA.getColumnIndex("personId");
		int indexLeg=matrixA.getColumnIndex("legIndex");
		
		int indexSelectedField=matrixA.getColumnIndex(fieldName);
		
		int totalNumberOfParkingOperations=0;
		int numberOfTimesSameParkingFacilityChosen=0;
		for (int i=1;i<matrixA.getNumberOfRows();i++){
			Id personId=new IdImpl(matrixA.getString(i, indexPersonId));
			int legIndex=matrixA.getInteger(i, indexLeg);
			
			String selectedFieldIdA=matrixA.getString(i, indexSelectedField);
			String selectedFieldIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexSelectedField);
			
			if (ignoreCasesWithBothPPUse){
				int indexFacilityId=matrixA.getColumnIndex("FacilityId");
				String facilityIdA=matrixA.getString(i, indexFacilityId);
				String facilityIdB=matrixB.getString(indexMatrixB.get(personId, legIndex), indexFacilityId);
				
				if (!(facilityIdA.toString().contains("private") && facilityIdB.toString().contains("private"))){
					if (selectedFieldIdA.equalsIgnoreCase(selectedFieldIdB)){
						numberOfTimesSameParkingFacilityChosen++;
					}
					totalNumberOfParkingOperations++;
				}
			} else {
				totalNumberOfParkingOperations++;
				
				if (selectedFieldIdA.equalsIgnoreCase(selectedFieldIdB)){
					numberOfTimesSameParkingFacilityChosen++;
				}
			}
		}
		
		return (1-(1.0*numberOfTimesSameParkingFacilityChosen/totalNumberOfParkingOperations))*100;
	}

	public static TwoHashMapsConcatenated<Id, Integer, Integer> getIndex(
			StringMatrix matrix) {
		TwoHashMapsConcatenated<Id, Integer, Integer> index = new TwoHashMapsConcatenated<Id, Integer, Integer>();

		int indexPersonId = matrix.getColumnIndex("personId");
		int indexLeg = matrix.getColumnIndex("legIndex");

		for (int i = 1; i < matrix.getNumberOfRows(); i++) {
			index.put(new IdImpl(matrix.getString(i, indexPersonId)),
					matrix.getInteger(i, indexLeg), i);
		}

		return index;
	}

}
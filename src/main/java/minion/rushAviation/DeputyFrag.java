package minion.rushAviation;

public class DeputyFrag {

	public static enum ConnectOperation{OP_PREPART,OP_AFTPART,OP_STRAIGHTEN,OP_ALLPART,OP_NONEOP};
	
	private FlightFrag flightFrag=null;
	
	ConnectOperation connectOperation=ConnectOperation.OP_NONEOP;
	
	private int lastFlightIdIndex=10000;
	
	private int airplaneIdIndex;
	
	private int airplaneTypeIndex;
	
	private int startAirportIndex;
	
	private int endAirportIndex;
	
	private long startDateTime;
	
	private long endDateTime;
	
	private double earnScore=0;
	
	private double costScore=0;
	
	public DeputyFrag() {
		
	}
	
	public void copyFrom(DeputyFrag deputyFrag) {
		this.flightFrag=deputyFrag.flightFrag;
		this.connectOperation=deputyFrag.connectOperation;
		this.lastFlightIdIndex=deputyFrag.lastFlightIdIndex;
		this.airplaneIdIndex=deputyFrag.airplaneIdIndex;
		this.airplaneTypeIndex=deputyFrag.airplaneTypeIndex;
		this.startAirportIndex=deputyFrag.startAirportIndex;
		this.endAirportIndex=deputyFrag.endAirportIndex;
		this.startDateTime=deputyFrag.startDateTime;
		this.endDateTime=deputyFrag.endDateTime;
		this.earnScore=deputyFrag.earnScore;
		this.costScore=deputyFrag.costScore;
	}
	
	public FlightFrag getFlightFrag() {
		return flightFrag;
	}
	
	public ConnectOperation getConnectOperation() {
		return connectOperation;
	}
	
	public int getLastFlightIdIndex() {
		return lastFlightIdIndex;
	}
	
	public int getAirplaneIdIndex() {
		return airplaneIdIndex;
	}
	
	public int getAirplaneTypeIndex() {
		return airplaneTypeIndex;
	}
	
	public int getStartAirportIndex() {
		return startAirportIndex;
	}
	
	public int getEndAirportIndex() {
		return endAirportIndex;
	}
	
	public long getStartDateTime() {
		return startDateTime;
	}
	
	public long getEndDateTime() {
		return endDateTime;
	}
	
	public double getEarnScore() {
		return earnScore;
	}
	
	public double getCostScore() {
		return costScore;
	}
	
	public void resetHeadFrag(FlightFrag headFrag) {
		connectOperation=ConnectOperation.OP_NONEOP;
		flightFrag=headFrag;
		if(flightFrag.isEmptyFlightFrag()) {
			//empty head
			DynamicFlight dynamicFlight=flightFrag.getFirstDynamicFlight();
			lastFlightIdIndex=100000;
			airplaneIdIndex=dynamicFlight.getAirplaneIdIndex();
			airplaneTypeIndex=dynamicFlight.getAirplaneTypeIndex();
			startAirportIndex=dynamicFlight.getStartAirportIndex();
			endAirportIndex=dynamicFlight.getStartAirportIndex();
			startDateTime=endDateTime=0;
			earnScore=costScore=0;
		}else {
			//normal head
			DynamicFlight firstDynamicFlight=flightFrag.getFirstDynamicFlight();
			DynamicFlight lastDynamicFlight=flightFrag.getLastDynamicFlight();
			lastFlightIdIndex=lastDynamicFlight.getFlightIdIndex();
			airplaneIdIndex=firstDynamicFlight.getAirplaneIdIndex();
			airplaneTypeIndex=firstDynamicFlight.getAirplaneTypeIndex();
			startAirportIndex=firstDynamicFlight.getStartAirportIndex();
			endAirportIndex=lastDynamicFlight.getEndAirportIndex();
			startDateTime=firstDynamicFlight.getStartDateTime();
			endDateTime=lastDynamicFlight.getEndDateTime();
			earnScore=Configuration.getCancelFlightParam()*flightFrag.getTotalImportRatio();
			costScore=flightFrag.getTotalScore();
		}
	}
	
	public void resetBodySingleFrag(FlightFrag bodySingleFrag, DynamicFlight dynamicFlight) {
		connectOperation=ConnectOperation.OP_NONEOP;
		flightFrag=bodySingleFrag;
		lastFlightIdIndex=dynamicFlight.getFlightIdIndex();
		airplaneIdIndex=dynamicFlight.getAirplaneIdIndex();
		airplaneTypeIndex=dynamicFlight.getAirplaneTypeIndex();
		startAirportIndex=dynamicFlight.getStartAirportIndex();
		endAirportIndex=dynamicFlight.getEndAirportIndex();
		startDateTime=dynamicFlight.getStartDateTime();
		endDateTime=dynamicFlight.getEndDateTime();
		earnScore=Configuration.getCancelFlightParam()*dynamicFlight.getImportRatio();
		costScore=dynamicFlight.getScore();
	}
	
	public void resetBodyConnectFrag(FlightFrag bodyConnectFrag, DynamicFlight preDynamicFlight, DynamicFlight aftDynamicFlight, ConnectOperation connectOperation) {
		this.connectOperation=connectOperation;
		flightFrag=bodyConnectFrag;
		if(connectOperation==ConnectOperation.OP_PREPART) {
			lastFlightIdIndex=preDynamicFlight.getFlightIdIndex();
			airplaneIdIndex=preDynamicFlight.getAirplaneIdIndex();
			airplaneTypeIndex=preDynamicFlight.getAirplaneTypeIndex();
			startAirportIndex=preDynamicFlight.getStartAirportIndex();
			endAirportIndex=preDynamicFlight.getEndAirportIndex();
			startDateTime=preDynamicFlight.getStartDateTime();
			endDateTime=preDynamicFlight.getEndDateTime();
			earnScore=Configuration.getCancelFlightParam()*preDynamicFlight.getImportRatio();
			costScore=preDynamicFlight.getScore()+Configuration.getCancelFlightParam()*aftDynamicFlight.getImportRatio()*0.4;
		}else if(connectOperation==ConnectOperation.OP_ALLPART) {
			lastFlightIdIndex=aftDynamicFlight.getFlightIdIndex();
			airplaneIdIndex=preDynamicFlight.getAirplaneIdIndex();
			airplaneTypeIndex=preDynamicFlight.getAirplaneTypeIndex();
			startAirportIndex=preDynamicFlight.getStartAirportIndex();
			endAirportIndex=aftDynamicFlight.getEndAirportIndex();
			startDateTime=preDynamicFlight.getStartDateTime();
			endDateTime=aftDynamicFlight.getEndDateTime();
			earnScore=Configuration.getCancelFlightParam()*(preDynamicFlight.getImportRatio()+aftDynamicFlight.getImportRatio());
			costScore=preDynamicFlight.getScore()+aftDynamicFlight.getScore();
		}else if(connectOperation==ConnectOperation.OP_STRAIGHTEN) {
			lastFlightIdIndex=preDynamicFlight.getFlightIdIndex();
			airplaneIdIndex=preDynamicFlight.getAirplaneIdIndex();
			airplaneTypeIndex=preDynamicFlight.getAirplaneTypeIndex();
			startAirportIndex=preDynamicFlight.getStartAirportIndex();
			endAirportIndex=preDynamicFlight.getEndAirportIndex();
			startDateTime=preDynamicFlight.getStartDateTime();
			endDateTime=preDynamicFlight.getEndDateTime();
			earnScore=Configuration.getCancelFlightParam()*(preDynamicFlight.getImportRatio()+aftDynamicFlight.getImportRatio());
			costScore=preDynamicFlight.getScore();
		}else if(connectOperation==ConnectOperation.OP_AFTPART) {
			lastFlightIdIndex=aftDynamicFlight.getFlightIdIndex();
			airplaneIdIndex=aftDynamicFlight.getAirplaneIdIndex();
			airplaneTypeIndex=aftDynamicFlight.getAirplaneTypeIndex();
			startAirportIndex=aftDynamicFlight.getStartAirportIndex();
			endAirportIndex=aftDynamicFlight.getEndAirportIndex();
			startDateTime=aftDynamicFlight.getStartDateTime();
			endDateTime=aftDynamicFlight.getEndDateTime();
			earnScore=Configuration.getCancelFlightParam()*aftDynamicFlight.getImportRatio();
			costScore=aftDynamicFlight.getScore()+Configuration.getCancelFlightParam()*preDynamicFlight.getImportRatio()*0.4;
		}
	}
	
	public void resetTailFrag(FlightFrag tailFrag, DynamicFlight firstDynamicFlight, DynamicFlight lastDynamicFlight, double costScore) {
		connectOperation=ConnectOperation.OP_NONEOP;
		flightFrag=tailFrag;
		lastFlightIdIndex=lastDynamicFlight.getFlightIdIndex();
		airplaneIdIndex=firstDynamicFlight.getAirplaneIdIndex();
		airplaneTypeIndex=firstDynamicFlight.getAirplaneTypeIndex();
		startAirportIndex=firstDynamicFlight.getStartAirportIndex();
		endAirportIndex=lastDynamicFlight.getEndAirportIndex();
		startDateTime=firstDynamicFlight.getStartDateTime();
		endDateTime=lastDynamicFlight.getEndDateTime();
		earnScore=Configuration.getCancelFlightParam()*tailFrag.getTotalImportRatio();
		this.costScore=costScore;
	}
	
	public String printDeputyFrag() {
		String lastFlightId="10000";
		if(lastFlightIdIndex<InfoInterpreter.flightIdArray.size())
			lastFlightId=InfoInterpreter.flightIdArray.get(lastFlightIdIndex);
		String str = lastFlightId + ","
				+ connectOperation + ","
                + InfoInterpreter.airportNameArray.get(startAirportIndex) + ","
                + InfoInterpreter.airportNameArray.get(endAirportIndex) + ","
                + InfoInterpreter.timeStampToString(startDateTime) + ","
                + InfoInterpreter.timeStampToString(endDateTime) + ","
                + InfoInterpreter.airplaneIdArray.get(airplaneIdIndex) + ","
                + InfoInterpreter.airplaneTypeArray.get(airplaneTypeIndex) + ","
            		+ earnScore + ","
            		+ costScore + "\n";
        return str;
	}
	
}

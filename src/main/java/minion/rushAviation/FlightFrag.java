package minion.rushAviation;

import javax.imageio.event.IIOReadWarningListener;

/**
 * Created by minion rush on 17/8/16.
 */
public class FlightFrag{

	public static enum FragType{FRAG_HEAD,FRAG_TAIL,FRAG_BODY_SINGLE,FRAG_BODY_CONNECT,FRAG_EMPTY};

	private FragType fragType;

	private int flightNumber=0;
	
	private double totalImportRatio=0;
	
	private double totalScore=0;
	
	private DynamicFlight firstDynamicFlight=null;
	
	private DynamicFlight lastDynamicFlight=null;

	private boolean canStraighten=false;
	
	private boolean isSearched=false;

	//构造函数——初始化构造
	public FlightFrag() {
		
	}
	
	//构造函数——空头链构造
	public FlightFrag(DynamicFlight firstDynamicFlight) {
		this.fragType=FragType.FRAG_HEAD;
		this.firstDynamicFlight=firstDynamicFlight;
	}
	
	//构造函数——正常链构造
	public FlightFrag(FragType fragType, int flightNumber, double totalImportRatio, double totalScore,
						DynamicFlight firstDynamicFlight, DynamicFlight lastDynamicFlight) {
		this.fragType=fragType;
		this.flightNumber=flightNumber;
		this.totalImportRatio=totalImportRatio;
		this.totalScore=totalScore;
		this.firstDynamicFlight=firstDynamicFlight;
		this.lastDynamicFlight=lastDynamicFlight;
	}
	
	public void copyFrom(FlightFrag flightFrag) {
		this.fragType=flightFrag.fragType;
		this.flightNumber=flightFrag.flightNumber;
		this.totalImportRatio=flightFrag.totalImportRatio;
		this.totalScore=flightFrag.totalScore;
		this.isSearched=flightFrag.isSearched;
		this.canStraighten=flightFrag.canStraighten;
	}

	public boolean isHeadFrag() {
		return fragType==FragType.FRAG_HEAD;
	}
	
	public boolean isTailFrag() {
		return fragType==fragType.FRAG_TAIL;
	}
	
	public boolean isBodySingleFrag() {
		return fragType==FragType.FRAG_BODY_SINGLE;
	}
	
	public boolean isBodyConnectFrag() {
		return fragType==FragType.FRAG_BODY_CONNECT;
	}
	
	public boolean isSearched() {
		return isSearched;
	}

	public void setIsSearched(boolean isSearched) {
		this.isSearched=isSearched;
	}
	
	public void setCanStraighten(boolean canStraighten) {
		this.canStraighten=canStraighten;
	}
	
	public void setTotalScore(double totalScore) {
		this.totalScore=totalScore;
	}
	
	public boolean isEmptyFlightFrag() {
		return flightNumber==0;
	}
	
	public DynamicFlight getFirstDynamicFlight() {
		return firstDynamicFlight;
	}
	
	public DynamicFlight getLastDynamicFlight() {
		return lastDynamicFlight;
	}
	
	public double getTotalImportRatio() {
		return totalImportRatio;
	}
	
	public double getTotalScore() {
		return totalScore;
	}
	
	public boolean getCanStraighten() {
		return canStraighten;
	}
	
	public void shiftHeadLeft() {
		
	}
	
	public void shiftHeadRight() {
		
	}
	
	public void shiftTailRight() {
		
	}
	
	public void shiftTailLeft() {
		
	}

	/**
	 * 打印碎片信息
	 */
	public String printFlightFrag(){
		int airplaneIdIndex=firstDynamicFlight.getAirplaneIdIndex();
		int airplaneTypeIndex=firstDynamicFlight.getAirplaneTypeIndex();
		int startAirportIndex=firstDynamicFlight.getStartAirportIndex();
		long startDateTime=firstDynamicFlight.getStartDateTime();
		int endAirportIndex=-1;
		long endDateTime=-1;
		if(flightNumber==0) {
			endAirportIndex=firstDynamicFlight.getStartAirportIndex();
			endDateTime=0;
		}else {
			endAirportIndex=lastDynamicFlight.getEndAirportIndex();
			endDateTime=lastDynamicFlight.getEndDateTime();
		}
		String str = InfoInterpreter.airplaneTypeArray.get(airplaneTypeIndex) + ","
				+ InfoInterpreter.airplaneIdArray.get(airplaneIdIndex) + ","
				+ fragType + ","
				+ (canStraighten?1:0) + ","
				+ InfoInterpreter.airportNameArray.get(startAirportIndex) + ","
				+ InfoInterpreter.airportNameArray.get(endAirportIndex) + ","
				+ InfoInterpreter.timeStampToString(startDateTime) + ","
				+ InfoInterpreter.timeStampToString(endDateTime) + ","
				+ flightNumber + ","
				+ totalImportRatio + ","
				+ totalScore + ","
				+ (isSearched?"sd":"nsd")+"\n";
		return str;
	}

}

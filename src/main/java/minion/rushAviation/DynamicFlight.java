package minion.rushAviation;

import javax.print.attribute.standard.RequestingUserName;

import org.apache.poi.util.Configurator;
import org.apache.xmlbeans.impl.xb.xmlconfig.ConfigDocument.Config;

/**
 * Created by minion rush on 17/8/16.
 */
public class DynamicFlight{
	
	//原始航班数据
	private OriginFlight originFlight;
	//同航线前置航班
	private DynamicFlight preDynamicFlight=null;
	//同航线后置航班
	private DynamicFlight nextDynamicFlight=null;
    //航班ID
    private int flightIdIndex;
    //起飞机场
    private int startAirportIndex;
    //降落机场
    private int endAirportIndex;
    //起飞时间
    private long startDateTime;
    //降落时间
    private long endDateTime;
    //飞机ID
    private int airplaneIdIndex;
    //机型
    private int airplaneTypeIndex;
    
    //是否取消
    private boolean isCancel=false;
    //是否拉直
    private boolean isStraighten=false;
    //是否调机
    private boolean isEmptyFlight=false;
    
    //分数
    private double adjustScore=0;
    private double cancelScore=0;
    private double changePlaneTypeScore=0;
    private double changePlaneScore=0;
    private double straightenScore=0;
    private double aheadScore=0;
    private double delayScore=0;
    
    private double importRatio=0;
    
    private boolean isOriginStartInCapacity=false;
    private boolean isOriginEndInCapacity=false;
    private boolean isOriginStartInScene=false;
    private boolean isOriginEndInScene=false;
    private boolean canAhead=false;
    private boolean canDelay=false;
    
    //构造函数——空构造函数
    public DynamicFlight() {
    	
    }
    
    //构造函数——构造已有航班
    public DynamicFlight(OriginFlight originFlight) {
    	this.originFlight=originFlight;
        this.flightIdIndex=originFlight.getFlightIdIndex();
        this.startAirportIndex=originFlight.getStartAirportIndex();
        this.endAirportIndex=originFlight.getEndAirportIndex();
        this.startDateTime=originFlight.getStartDateTime();
        this.endDateTime=originFlight.getEndDateTime();
        this.airplaneIdIndex=originFlight.getAirplaneIdIndex();
        this.airplaneTypeIndex=originFlight.getAirplaneTypeIndex();
        this.importRatio=originFlight.getImportRatio();
	}
    
    //构造函数——构造调机航班
    public DynamicFlight(int emptyFlightIdIndex) {
        flightIdIndex=emptyFlightIdIndex;
        isEmptyFlight=true;
	}
    
    public void copyFrom(DynamicFlight dynamicFlight) {
    		this.originFlight=dynamicFlight.originFlight;
        this.flightIdIndex=dynamicFlight.flightIdIndex;
        this.startAirportIndex=dynamicFlight.startAirportIndex;
        this.endAirportIndex=dynamicFlight.endAirportIndex;
        this.startDateTime=dynamicFlight.startDateTime;
        this.endDateTime=dynamicFlight.endDateTime;
        this.airplaneIdIndex=dynamicFlight.airplaneIdIndex;
        this.airplaneTypeIndex=dynamicFlight.airplaneTypeIndex;
        this.isCancel=dynamicFlight.isCancel;
        this.isStraighten=dynamicFlight.isStraighten;
        this.isEmptyFlight=dynamicFlight.isEmptyFlight;
        this.adjustScore=dynamicFlight.adjustScore;
        this.cancelScore=dynamicFlight.cancelScore;
        this.changePlaneTypeScore=dynamicFlight.changePlaneTypeScore;
        this.changePlaneScore=dynamicFlight.changePlaneScore;
        this.straightenScore=dynamicFlight.straightenScore;
        this.aheadScore=dynamicFlight.aheadScore;
        this.delayScore=dynamicFlight.delayScore;
        this.importRatio=dynamicFlight.importRatio;
        this.isOriginStartInCapacity=dynamicFlight.isOriginStartInCapacity;
        this.isOriginEndInCapacity=dynamicFlight.isOriginEndInCapacity;
        this.isOriginStartInScene=dynamicFlight.isOriginStartInScene;
        this.isOriginEndInScene=dynamicFlight.isOriginEndInScene;
        this.canAhead=dynamicFlight.canAhead;
        this.canDelay=dynamicFlight.canDelay;
    }
    
    public void setPreDynamicFlight(DynamicFlight preDynamicFlight) {
    	this.preDynamicFlight=preDynamicFlight;
    }
    
    public void setNextDynamicFlight(DynamicFlight nextDynamicFlight) {
    	this.nextDynamicFlight=nextDynamicFlight;
    }
    
    public void setEmptyFlight(int startAirportIndex, int endAirportIndex, long startDateTime, 
			long endDateTime, int airplaneIdIndex, int airplaneTypeIndex) {
    	this.startAirportIndex=startAirportIndex;
        this.endAirportIndex=endAirportIndex;
        this.startDateTime=startDateTime;
        this.endDateTime=endDateTime;
        this.airplaneIdIndex=airplaneIdIndex;
        this.airplaneTypeIndex=airplaneTypeIndex;
    }
    
    public void setCanAhead(boolean canAhead) {
    	this.canAhead=canAhead;
    }
    
    public void setCanDelay(boolean canDelay) {
    	this.canDelay=canDelay;
    }
    
    public boolean getCanAhead() {
    	return canAhead;
    }
    
    public boolean getCanDelay() {
    	return canDelay;
    }
    
    public boolean getCanAdjustTime() {
    	return canAhead||canDelay;
    }
    
    public void resetStartDateTime() {
    	long travelTime=endDateTime-startDateTime;
    	startDateTime=originFlight.getStartDateTime();
    	endDateTime=startDateTime+travelTime;
    	aheadScore=delayScore=0;
    }
    
    public void setIsOriginStartInCapacity(boolean isOriginStartInCapacity) {
    	this.isOriginStartInCapacity=isOriginStartInCapacity;
    }
    
    public void setIsOriginEndInCapacity(boolean isOriginEndInCapacity) {
    	this.isOriginEndInCapacity=isOriginEndInCapacity;
    }
    
    public void setIsOriginStartInScene(boolean isOriginStartInScene) {
    	this.isOriginStartInScene=isOriginStartInScene;
    }
    
    public void setIsOriginEndInScene(boolean isOriginEndInScene) {
    	this.isOriginEndInScene=isOriginEndInScene;
    }
    
    public boolean isDomestic() {
    	return originFlight.isDomestic();
    }
    
    public boolean isOriginStartInScene() {
    	return isOriginStartInScene;
    }
    
    public boolean isOriginEndInScene() {
    	return isOriginEndInScene;
    }
    
    public boolean isOriginStartInCapacity() {
    	return isOriginStartInCapacity;
    }
    
    public boolean isOriginEndInCapacity() {
    	return isOriginEndInCapacity;
    }
    
    public boolean isAhead() {
    	return this.startDateTime<originFlight.getStartDateTime();
    }
    
    public boolean isDelay() {
    	return this.startDateTime>originFlight.getEndDateTime();
    }
    
    public boolean isCancel() {
    	return isCancel&&!isStraighten;
    }
    
    public DynamicFlight getPreDynamicFlight() {
    	return preDynamicFlight;
    }
    
    public DynamicFlight getNextDynamicFlight() {
    	return nextDynamicFlight;
    }
    
    public long getOriginStartDateTime() {
    	return originFlight.getStartDateTime();
    }
    
    public long getOriginEndDateTime() {
    	return originFlight.getEndDateTime();
    }
    
    public long getTravelTime() {
    	return endDateTime-startDateTime;
    }
    
    public int getFlightIdIndex() {
    	return flightIdIndex;
    }
    
    //提前航班
    public boolean aheadFlight(long aheadTime) {
    	if(originFlight.isDomestic()) {
    		startDateTime-=aheadTime;
        	endDateTime-=aheadTime;
        	aheadScore+=(importRatio*aheadTime*Configuration.getAheadFlightParam()/3600000);
        	double originStartDateTime=originFlight.getStartDateTime();
    		return originStartDateTime-startDateTime<=Configuration.maxAheadTime;
    	}else {
    		return false;
    	}
    }
    
    //延后航班
    public boolean delayFlight(long delayTime) {
    	//remark: empty flight might delay as well
    	startDateTime+=delayTime;
    	endDateTime+=delayTime;
    	if(isEmptyFlight) {
    		return true;
    	}else {
    		delayScore+=(importRatio*delayTime*Configuration.getDelayFlightParam()/3600000);
        	double originStartDateTime=originFlight.getStartDateTime();
        	if(originFlight.isDomestic()) {
        		return startDateTime-originStartDateTime<=Configuration.maxDomesticDelayTime;
        	}else {
        		return startDateTime-originStartDateTime<=Configuration.maxAbroadDelayTime;
        	}	
    	}
    }
    
    //取消航班
    public void cancelFlight() {
    	isCancel=true;
    	cancelScore=importRatio*Configuration.getCancelFlightParam();
    }
    
    //拉直第1个联程航班
    public void setStraightenPrePart(long travelTime, int endAirportIndex, double nextImportRatio) {
    	isStraighten=true;
    	endDateTime=startDateTime+travelTime;
    	this.endAirportIndex=endAirportIndex;
    	importRatio+=nextImportRatio;
    	straightenScore=importRatio*Configuration.getConnectFlightStraightenParam();
    }
    
    //拉直第2个联程航班
    public void setStraightenAftPart() {
    	isCancel=true;
    	isStraighten=true;
    }
    
    //换机
    public void changePlane(int airplaneIdIndex, int airplaneTypeIndex) {
    	if(this.airplaneIdIndex!=airplaneIdIndex) {
    		long originStartDateTime=originFlight.getStartDateTime();
    		//changePlaneScore=Configuration.getSwapFlightParam(originStartDateTime);
    		changePlaneScore=importRatio*Configuration.getSwapFlightParam(originStartDateTime);
    	}
    	//changePlaneTypeScore=Configuration.getFlightTypeChangeParam(this.airplaneTypeIndex, airplaneTypeIndex);
    	changePlaneTypeScore=importRatio*Configuration.getFlightTypeChangeParam(this.airplaneTypeIndex, airplaneTypeIndex);
    	this.airplaneIdIndex=airplaneIdIndex;
    	this.airplaneTypeIndex=airplaneTypeIndex;
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
    
    public double getImportRatio() {
    	return importRatio;
    }
    
    public double getAdjustScore() {
    	return adjustScore;
    }
    
    public double getCancelScore() {
    	return cancelScore;
    }
    
    public double getChangePlaneTypeScore() {
    	return changePlaneTypeScore;
    }
    
    public double getChangePlaneScore() {
    	return changePlaneScore;
    }
    
    public double getStraightenScore() {
    	return straightenScore;
    }
    
    public double getAheadScore() {
    	return aheadScore;
    }
    
    public double getDelayScore() {
    	return delayScore;
    }
    
    public double getScore() {
    	double score=adjustScore+aheadScore+delayScore+changePlaneTypeScore+changePlaneScore+cancelScore+straightenScore;
    	return score;
    }
    
    public int getAirplaneIdIndex() {
    	return airplaneIdIndex;
    }
    
    public int getAirplaneTypeIndex() {
    	return airplaneTypeIndex;
    }
    
    public boolean isConnected() {
    	return originFlight.isConnected();
    }
    
    public boolean isConnectPrePart() {
    	return originFlight.isConnected()&&originFlight.isConnectedPrePart();
    }
    
    public boolean isConnectAftPart() {
    	return originFlight.isConnected()&&!originFlight.isConnectedPrePart();
    }
    
    /**
     * 打印动态航班结果
     */
    public String printDynamicFlight(){
    	int connectedFlightIdIndex=originFlight.getConnectedFlightIdIndex();
    	String connectedFlightId="-1";
    	if(connectedFlightIdIndex>=0) {
    		connectedFlightId=InfoInterpreter.flightIdArray.get(connectedFlightIdIndex);
    	}
    	double score=adjustScore+aheadScore+delayScore+changePlaneTypeScore+changePlaneScore+cancelScore+straightenScore;
        String str = InfoInterpreter.flightIdArray.get(flightIdIndex) + ","
        		+ (originFlight.isDomestic()?1:0) + ","
        		+ connectedFlightId + ","
                + InfoInterpreter.airportNameArray.get(startAirportIndex) + ","
                + InfoInterpreter.airportNameArray.get(endAirportIndex) + ","
                + InfoInterpreter.timeStampToString(startDateTime) + ","
                + InfoInterpreter.timeStampToString(endDateTime) + ","
                + InfoInterpreter.airplaneIdArray.get(airplaneIdIndex) + ","
                + (isOriginStartInCapacity?1:0) + ","
                + (isOriginEndInCapacity?1:0) + ","
                + (isOriginStartInScene?1:0) + ","
                + (isOriginEndInScene?1:0) + ",,"
                + (canAhead?1:0) + ","
                + (canDelay?1:0) + ",,"
            		+ importRatio + ","
            		+ score + ",,"
                + (isCancel?1:0) + ","
            		+ (isStraighten?1:0) + ","
            		+ (isEmptyFlight?1:0) + ","
            		+ 0 + "\n";
        return str;
    }
    
    /**
     * 打印求解结果
     */
    public String printResultFlight(){
        String str = InfoInterpreter.flightIdArray.get(flightIdIndex) + ","
                + InfoInterpreter.airportNameArray.get(startAirportIndex) + ","
                + InfoInterpreter.airportNameArray.get(endAirportIndex) + ","
                + InfoInterpreter.timeStampToString(startDateTime) + ","
                + InfoInterpreter.timeStampToString(endDateTime) + ","
                + InfoInterpreter.airplaneIdArray.get(airplaneIdIndex) + ","
                + (isCancel?1:0) + ","
            		+ (isStraighten?1:0) + ","
            		+ (isEmptyFlight?1:0) + ","
            		+ 0 + ","
            		+ " " + "\n";
        return str;
    }
    
}

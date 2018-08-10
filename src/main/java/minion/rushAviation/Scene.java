package minion.rushAviation;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Created by minion rush on 17/8/16.
 */
public class Scene {
    //开始时间
    private long startDateTime;
    //介绍时间
    private long endDateTime;
    //故障类型 (0：降落，1：飞行，2：停机)
    private int type;
    //机场
    private int airportIndex;
    //停机数
    private int stopAirplaneNum;
    //余下停机数
    private int remainStopNum;
    
    public void resetRemainStopNum() {
    	remainStopNum=stopAirplaneNum;
    }
    
    public boolean isEndSceneType() {
    	if(type==0) {
    		return true;
    	}
    	return false;
    }
    
    public boolean isStartSceneType() {
    	if(type==1) {
    		return true;
    	}
    	return false;
    }
    
    public boolean isStopSceneType() {
    	if(type==2) {
    		return true;
    	}
    	return false;
    }
    
    public long getStartDateTime() {
        return startDateTime;
    }

    public long getEndDateTime() {
        return endDateTime;
    }

    public int getAirportIndex() {
        return airportIndex;
    }

    public Scene(Row row){
        if(row.getPhysicalNumberOfCells() != 7){
            throw new RuntimeException("故障信息的数据列数错误，不等于7项！");
        }
        DataFormatter df = new DataFormatter();
        startDateTime = row.getCell(0).getDateCellValue().getTime();
        endDateTime = row.getCell(1).getDateCellValue().getTime();
        type = typeTransfer(df.formatCellValue(row.getCell(2)));
        airportIndex = InfoInterpreter.airportNameMap.get(df.formatCellValue(row.getCell(3)));
        remainStopNum = stopAirplaneNum = Integer.parseInt(df.formatCellValue(row.getCell(6)).isEmpty() ? "0" : df.formatCellValue(row.getCell(6)));
    }

    private int typeTransfer(String limitType){
        if("降落".equals(limitType)){
            return 0;
        }
        else if("起飞".equals(limitType)){
            return 1;
        }
        else if("停机".equals(limitType)){
            return 2;
        }
        else {
            throw new RuntimeException("出现未知的场景类型错误！");
        }
    }
    
    private String typeString() {
    		if(type==0) {
    			return "降落";
    		}else if(type==1){
    			return "起飞";
    		}else if(type==2){
    			return "停机";
    		}
    		else{
    			throw new RuntimeException("出现未知的场景类型错误！");
    		}
    }
    
    /**
     * 根据传进来的数据，判断是否落在受影响的场景内
     * @param flightId       //暂时没有航班故障做处理
     * @param airplaneId     //暂时没有飞机故障做处理
     * @param startAirport
     * @param endAirport
     * @param startTime
     * @param endTime
     * @return
     */
    public boolean isInScene(int startAirportIndex, int endAirportIndex, long startTime, long endTime){
        if(type == 0){    //降落判断
        		if(airportIndex==endAirportIndex && (endTime>startDateTime && endTime<endDateTime))
                return true;
        }
        else if(type == 1){    //起飞判断
        		if(airportIndex==startAirportIndex && (startTime>startDateTime && startTime<endDateTime))
                return true;
        }
        return false;
    }
    
    /**
     * 根据传进来的数据，判断起飞是否落在受影响的场景内
     * @param flightId       //暂时没有航班故障做处理
     * @param airplaneId     //暂时没有飞机故障做处理
     * @param startAirport
     * @param startTime
     * @return
     */
    public boolean isStartInScene(int startAirportIndex, long startTime){
        if(type == 1){    //起飞故障判断
            if(airportIndex==startAirportIndex && (startTime>startDateTime && startTime<endDateTime))
                return true;
        }
        return false;
    }

    /**
     * 根据传进来的数据，判断降落是否落在受影响的场景内
     * @param flightId       //暂时没有航班故障做处理
     * @param airplaneId     //暂时没有飞机故障做处理
     * @param endAirport
     * @param endTime
     * @return
     */
    public boolean isEndInScene(int endAirportIndex, long endTime){
       if(type == 0){    //降落故障判断
           if(airportIndex==endAirportIndex && (endTime>startDateTime && endTime<endDateTime))
               return true;
        }
        return false;
    }

    /**
     * 根据传进来的数据，判断停机时间段是否落在受影响的场景内
     * @param flightId
     * @param airplaneId
     * @param airport
     * @param startTime
     * @param endTime
     * @return
     */
    public boolean isStopInScene(int airportIndex, long startTime, long endTime){
        if(type == 2){    //机场停机判断
            if(this.airportIndex==airportIndex && (endTime>=endDateTime && startTime<=startDateTime)) {
            	return true;
            }
        }
        return false;
    }
    
    public boolean canStopInScene(int airportIndex, long startTime, long endTime){
        if(type == 2){    //机场停机判断
            if(this.airportIndex==airportIndex && (endTime>=endDateTime && startTime<=startDateTime)) {
            	return remainStopNum>0;
            }
        }
        return true;
    }
    
    public void paybackRemainStopNum(int airportIndex, long startTime, long endTime) {
    	if(type == 2){    //机场停机判断
            if(this.airportIndex==airportIndex && (endTime>=endDateTime && startTime<=startDateTime)) {
            	if(remainStopNum<stopAirplaneNum) {
            		++remainStopNum;
            	}else {
            		throw new RuntimeException("停机数资源归还异常！");
            	}
            }
        }
    }
    
    public void consumeRemainStopNum(int airportIndex, long startTime, long endTime){
        if(type == 2){    //机场停机判断
            if(this.airportIndex==airportIndex && (endTime>=endDateTime && startTime<=startDateTime)) {
            	if(remainStopNum>0) {
            		--remainStopNum;
            	}else {
            		throw new RuntimeException("停机数资源竞争异常！");
            	}
            }
        }
    }
    
    public String printScene(){
    	String str = InfoInterpreter.timeStampToString(startDateTime) + ","
    				+ InfoInterpreter.timeStampToString(endDateTime) + ","
    	    			+ typeString() + ","
    	    			+ InfoInterpreter.airportNameArray.get(airportIndex) + ","
    	    	    	+ stopAirplaneNum + "\n";
    return str;
}
}

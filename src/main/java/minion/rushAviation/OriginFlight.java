package minion.rushAviation;


import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Created by minion rush on 17/8/16.
 */
public class OriginFlight implements Comparable{
	
	//原始前置航班
	private OriginFlight preOriginFlight=null;
	//原始后置航班
	private OriginFlight nextOriginFlight=null;
    //航班ID
    private int flightIdIndex;
    //日期
    private long date;
    //国际/国内
    private boolean isDomestic;
    //航班号
    private int flightNoIndex;
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
    //旅客数
    private int passengerNum;
    //联程旅客数
    private int connectPassengerNum;
    //座位数
    private int seatNum;
    
    //重要系数
    private float importRatio;

    //是否是联程航班
    private boolean isConnected = false;
    //联程航班位置
    private boolean isConnectedPrePart;
    //联程航班后置航班
    private int connectedFlightIdIndex=-1;
    

    public void setPreOriginFlight(OriginFlight preOriginFlight) {
    	this.preOriginFlight=preOriginFlight;
    }
    
    public void setNextOriginFlight(OriginFlight nextOriginFlight) {
    	this.nextOriginFlight=nextOriginFlight;
    }
    
    public OriginFlight getPreOriginFlight() {
    	return preOriginFlight;
    }
    
    public OriginFlight getNextOriginFlight() {
    	return nextOriginFlight;
    }
    
    public void setConnected(int connectedFlightIdIndex, boolean loc) {
        isConnected = true;
        this.connectedFlightIdIndex = connectedFlightIdIndex;
        isConnectedPrePart=loc;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public int getConnectedFlightIdIndex() {
        return connectedFlightIdIndex;
    }

    public int getFlightIdIndex() {
        return flightIdIndex;
    }

    public long getDate() {
        return date;
    }

    public boolean isDomestic() {
        return isDomestic;
    }

    public int getFlightNoIndex() {
        return flightNoIndex;
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

    public int getAirplaneIdIndex() {
        return airplaneIdIndex;
    }

    public int getAirplaneTypeIndex() {
        return airplaneTypeIndex;
    }

    public float getImportRatio() {
        return importRatio;
    }
    
    public boolean isConnectedPrePart() {
    	return isConnectedPrePart;
    }

    //构造函数
    public OriginFlight(Row row){
        if(row.getPhysicalNumberOfCells() != 14){
            throw new RuntimeException("航班信息的数据列数错误，不等于14项！");
        }
        //临时变量
        Integer index;
        String tpstring;
        DataFormatter df = new DataFormatter();
        //航班ID
        tpstring=df.formatCellValue(row.getCell(0));
        index=InfoInterpreter.flightIdMap.get(tpstring);
        if(index==null) {
        	index=InfoInterpreter.flightIdArray.size();
        	InfoInterpreter.flightIdArray.add(tpstring);
        	InfoInterpreter.flightIdMap.put(tpstring, index);
        }
        flightIdIndex=index;
        //日期
        date = row.getCell(1).getDateCellValue().getTime();
        //国际/国内
        isDomestic = df.formatCellValue(row.getCell(2)).equals("国内") ? true : false;
        //航班号
        tpstring=df.formatCellValue(row.getCell(3));
        index=InfoInterpreter.flightNoMap.get(tpstring);
        if(index==null) {
        	index=InfoInterpreter.flightNoArray.size();
        	InfoInterpreter.flightNoArray.add(tpstring);
        	InfoInterpreter.flightNoMap.put(tpstring, index);
        }
        flightNoIndex=index;
        //起飞机场
        tpstring=df.formatCellValue(row.getCell(4));
        index=InfoInterpreter.airportNameMap.get(tpstring);
        if(index==null) {
        	index=InfoInterpreter.airportNameArray.size();
        	InfoInterpreter.airportNameArray.add(tpstring);
        	InfoInterpreter.airportNameMap.put(tpstring, index);
        }
        startAirportIndex=index;
        //降落机场
        tpstring=df.formatCellValue(row.getCell(5));
        index=InfoInterpreter.airportNameMap.get(tpstring);
        if(index==null) {
        	index=InfoInterpreter.airportNameArray.size();
        	InfoInterpreter.airportNameArray.add(tpstring);
        	InfoInterpreter.airportNameMap.put(tpstring, index);
        }
        endAirportIndex=index;
        //起飞时间
        startDateTime = row.getCell(6).getDateCellValue().getTime();
        //降落时间
        endDateTime = row.getCell(7).getDateCellValue().getTime();
        //飞机ID
        tpstring=df.formatCellValue(row.getCell(8));
        index=InfoInterpreter.airplaneIdMap.get(tpstring);
        if(index==null) {
        	index=InfoInterpreter.airplaneIdArray.size();
        	InfoInterpreter.airplaneIdArray.add(tpstring);
        	InfoInterpreter.airplaneIdMap.put(tpstring, index);
        }
        airplaneIdIndex=index;
        //机型
        tpstring=df.formatCellValue(row.getCell(9));
        index=InfoInterpreter.airplaneTypeMap.get(tpstring);
        if(index==null) {
        	index=InfoInterpreter.airplaneTypeArray.size();
        	InfoInterpreter.airplaneTypeArray.add(tpstring);
        	InfoInterpreter.airplaneTypeMap.put(tpstring, index);
        }
        airplaneTypeIndex=index;
        passengerNum = Integer.parseInt(df.formatCellValue(row.getCell(10)));
        connectPassengerNum = Integer.parseInt(df.formatCellValue(row.getCell(11)));
        seatNum = Integer.parseInt(df.formatCellValue(row.getCell(12)));
        importRatio = Float.parseFloat(df.formatCellValue(row.getCell(13)));
    }

    @Override
    public int compareTo(Object o) {
        OriginFlight other = (OriginFlight) o;
        if (this.airplaneIdIndex==other.airplaneIdIndex) {
            if (this.startDateTime>other.startDateTime) {
                return 1;
            } else if (this.startDateTime<other.startDateTime) {
                return -1;
            } else {
                return 0;
            }
        } else {
	        	if(this.airplaneIdIndex>other.airplaneIdIndex) {
	        		return 1;
	        	}else{
	        		return -1;
	        	}
        }
    }

    /**
     * 打印航班信息
     */
    public String printFlight(){
        String str = InfoInterpreter.flightIdArray.get(flightIdIndex) + ","
        			+ InfoInterpreter.timeStampToString(date) + ","
        			+ (isDomestic?"0":"1")+","
        			+ (isConnected?InfoInterpreter.flightIdArray.get(connectedFlightIdIndex):"-1") + ","
                + InfoInterpreter.airportNameArray.get(startAirportIndex) + ","
                + InfoInterpreter.airportNameArray.get(endAirportIndex) + ","
                + InfoInterpreter.timeStampToString(startDateTime) + ","
                + InfoInterpreter.timeStampToString(endDateTime) + ","
                + InfoInterpreter.airplaneIdArray.get(airplaneIdIndex) + ","
                + InfoInterpreter.airplaneTypeArray.get(airplaneTypeIndex) + ","
                + passengerNum + ","
                + connectPassengerNum + ","
                + seatNum + ","
                + importRatio + "\n";
        return str;
    }
}

package minion.rushAviation;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Created by minion rush on 17/8/16.
 */
public class AirplaneLimitation implements Comparable{
    //起飞机场
    private int startAirportIndex;
    //降落机场
    private int endAirportIndex;
    //飞机ID
    private int airplaneIdIndex;
    //辅助排序查找参数
    private int searchNumber;

    public int getStartAirportIndex() {
        return startAirportIndex;
    }

    public int getEndAirportIndex() {
        return endAirportIndex;
    }

    public int getAirplaneIdIndex() {
        return airplaneIdIndex;
    }

    public AirplaneLimitation(Row row){
        if(row.getPhysicalNumberOfCells() != 3){
            throw new RuntimeException("航线-飞机限制信息的数据列数错误，不等于3项！");
        }
        DataFormatter df = new DataFormatter();
        startAirportIndex = InfoInterpreter.airportNameMap.get(df.formatCellValue(row.getCell(0)));
        endAirportIndex = InfoInterpreter.airportNameMap.get(df.formatCellValue(row.getCell(1)));
        airplaneIdIndex = InfoInterpreter.airplaneIdMap.get(df.formatCellValue(row.getCell(2)));
        int airportnumber=InfoInterpreter.airportNameArray.size();
        searchNumber=startAirportIndex*airportnumber+endAirportIndex;
    }
    
    public int compareSearchNumber(int searchNumber) {
    		if(this.searchNumber>searchNumber) {
    			return 1;
    		}
    		else if(this.searchNumber<searchNumber) {
    			return -1;
    		}else {
    			return 0;
    		}
    }
    
    @Override
    public int compareTo(Object o) {
        AirplaneLimitation other = (AirplaneLimitation) o;
        if (this.searchNumber>other.searchNumber) {
            return 1;
        } else if(this.searchNumber<other.searchNumber){
        		return -1;
        } else {
        		return 0;
        }
    }
    
    public String printAirplaneLimitation(){
	    	String str = InfoInterpreter.airportNameArray.get(startAirportIndex) + ","
	    				+ InfoInterpreter.airportNameArray.get(endAirportIndex) + ","
	    				+ InfoInterpreter.airplaneIdArray.get(airplaneIdIndex) + "\n";
	    return str;
    }
}

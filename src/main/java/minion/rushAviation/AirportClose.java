package minion.rushAviation;


import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Created by minion rush on 17/8/16.
 */
public class AirportClose {
    //机场
    private int airportIndex;
    //关闭时间
    private long beginCloseTime;
    //开放时间
    private long endCloseTime;
    //生效日期
    private long beginDate;
    //失效日期
    private long endDate;

    public int getAirportIndex() {
        return airportIndex;
    }

    public long getBeginCloseTime() {
        return beginCloseTime;
    }
    
    public long getBeginDateTime(long time) {
    	long date=time-(time+28800000)%86400000;
    	return date+beginCloseTime;
    }

    public long getEndCloseTime() {
        return endCloseTime;
    }
    
    public long getStartDateTime(long time) {
    	long date=time-(time+28800000)%86400000;
    	return date+beginCloseTime;
    }
    
    public long getEndDateTime(long time) {
    	long date=time-(time+28800000)%86400000;
    	return date+endCloseTime;
    }

    public long getBeginDate() {
        return beginDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public AirportClose(Row row){
        if(row.getPhysicalNumberOfCells() != 5){
            throw new RuntimeException("机场关闭限制信息的数据列数错误，不等于5项！");
        }
        DataFormatter df = new DataFormatter();
        airportIndex =  InfoInterpreter.airportNameMap.get(df.formatCellValue(row.getCell(0)));
        beginCloseTime = processTimeStr( df.formatCellValue(row.getCell(1)));
        endCloseTime = processTimeStr( df.formatCellValue(row.getCell(2)));
        beginDate = row.getCell(3).getDateCellValue().getTime();
        endDate = row.getCell(4).getDateCellValue().getTime();
    }

    //时间字符串（0:00, 00:00）的处理
    private long processTimeStr(String timeStr){
        if(timeStr.length() == 4){
            int hour = Integer.parseInt(timeStr.substring(0, 1));
            int minute = Integer.parseInt(timeStr.substring(2, 4));
            return (hour * 60 + minute) * 60000;
        }
        else if(timeStr.length() == 5) {
            int hour = Integer.parseInt(timeStr.substring(0, 2));
            int minute = Integer.parseInt(timeStr.substring(3, 5));
            return (hour * 60 + minute) * 60000;
        }
        else {
            throw new RuntimeException("机场关闭限制信息中的时间字符串格式不满足格式要求(0:00, 00:00)！");
        }
    }

    //判断时间戳是否落在关闭时间窗内
    public boolean isClosed(long time){
    	long date=time-(time+28800000)%86400000;
        if(date<beginDate || date>=endDate){
            return false;
        }
        long start = date + beginCloseTime;
        long end = date + endCloseTime;
        if(time > start && time < end){
            return true;
        }
        return false;
    }
    
    public String printAirportClose(){
	    	String str = InfoInterpreter.airportNameArray.get(airportIndex) + ","
	    				+ InfoInterpreter.timeStampToString(beginCloseTime-28800000) + ","
	    	    			+ InfoInterpreter.timeStampToString(endCloseTime-28800000) + ","
	    	    			+ InfoInterpreter.timeStampToString(beginDate) + ","
	    	    	    	+ InfoInterpreter.timeStampToString(endDate) + "\n";
	    return str;
	}
}

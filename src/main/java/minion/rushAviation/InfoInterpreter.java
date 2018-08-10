package minion.rushAviation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by minion rush on 17/8/16.
 */
public class InfoInterpreter {
	
	//mapping vectors
	public static ArrayList<String> flightIdArray=new ArrayList<>();
	public static ArrayList<String> flightNoArray=new ArrayList<>();
	public static ArrayList<String> airportNameArray=new ArrayList<>();
	public static ArrayList<String> airplaneIdArray=new ArrayList<>();
	public static ArrayList<String> airplaneTypeArray=new ArrayList<>();
	//mapping maps
	public static Map<String, Integer> flightIdMap=new HashMap<>();
	public static Map<String, Integer> flightNoMap=new HashMap<>();
	public static Map<String, Integer> airportNameMap=new HashMap<>();
	public static Map<String, Integer> airplaneIdMap=new HashMap<>();
	public static Map<String, Integer> airplaneTypeMap=new HashMap<>();
	
	//time operations
	private static final String TIME_PATTERN = "yyyy/MM/dd HH:mm";
    private static final String DAY_PATTERN = "yyyy/MM/dd";

    /**
     * 将时间字符串转化为Date类
     * @param timeStr
     * @return
     */
    public static Date timeStringToDate(String timeStr) {
        SimpleDateFormat sf = new SimpleDateFormat(TIME_PATTERN);
        Date date = null;
        try {
            date = sf.parse(timeStr);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 将时间戳转化为时间字符串
     * @param time
     * @return
     */
    public static String timeStampToString(long time) {
        SimpleDateFormat sf = new SimpleDateFormat(TIME_PATTERN);
        Date date = new Date(time);
        return sf.format(date);
    }

    /**
     * 将时间戳转化为对应的日期所对应的时间戳
     * @param timeStr
     * @return
     */
    public static Date timeStampToDate(String timeStr) {
        SimpleDateFormat sf = new SimpleDateFormat(DAY_PATTERN);
        Date date = null;
        try {
            date = sf.parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}

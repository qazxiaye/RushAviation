package minion.rushAviation;

public class BrokenTimeWindow {
    //航班打散的起始时间
    private long startTime;
    //航班打散的结束时间
    private long endTime;

    public BrokenTimeWindow(long startTime, long endTime){
        this.startTime = startTime;
        this.endTime = endTime;
        if(startTime > endTime){
            throw new RuntimeException("打散时间窗数据有误！");
        }
    }

    //判断传进来的时间是否在打散时间窗口之内
    public boolean isInBrokenTimeWindow(long time){
        if(time <= startTime || time >= endTime)
            return false;
        return true;
    }
}

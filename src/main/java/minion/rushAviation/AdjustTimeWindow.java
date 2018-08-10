package minion.rushAviation;

/**
 * Created by minion rush on 17/8/16.
 */
public class AdjustTimeWindow {
    //恢复调整的起始时间
    private long startTime;
    //恢复调整的结束时间
    private long endTime;

    public AdjustTimeWindow(long startTime, long endTime){
        this.startTime = startTime;
        this.endTime = endTime;
        if(!(startTime < endTime)){
            throw new RuntimeException("恢复时间窗数据有误！");
        }
    }

    //判断传进来的时间是否在调整时间窗口之内
    public boolean isInAdjustTimeWindow(long time){
        if(time < startTime || time > endTime)
            return false;
        return true;
    }
}

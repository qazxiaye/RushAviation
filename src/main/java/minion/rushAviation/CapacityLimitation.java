package minion.rushAviation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by minion rush on 17/8/16.
 */
public class CapacityLimitation {
    //台风前容量控制的起始时间
    private long startBeforeTime;
    //台风前容量控制的结束时间
    private long endBeforeTime;
    //台风后容量控制的起始时间
    private long startAfterTime;
    //台风后容量控制的结束时间
    private long endAfterTime;
    //台风前容量资源
    private int[] beforeCapacity;
    //台风后容量资源
    private int[] afterCapacity;

    public CapacityLimitation(long startBeforeTime, long endBeforeTime, long startAfterTime, long endAfterTime){
        this.startBeforeTime = startBeforeTime;
        this.endBeforeTime = endBeforeTime;
        this.startAfterTime = startAfterTime;
        this.endAfterTime = endAfterTime;
        if(!(startBeforeTime < endBeforeTime && endBeforeTime < startAfterTime && startAfterTime < endAfterTime)){
            throw new RuntimeException("单位时间容量限制时间窗数据有误！");
        }
        generateCapacity();
    }
    
    public void resetCapacity() {
    	for(int index=0, len=beforeCapacity.length;index<len;++index) {
    		beforeCapacity[index]=2;
    	}
    	for(int index=0, len=afterCapacity.length;index<len;++index) {
    		afterCapacity[index]=2;
    	}
    }
    
    public boolean isInCapacityLimitation(long time) {
    	if((time >= startBeforeTime && time <= endBeforeTime) || (time >= startAfterTime && time <= endAfterTime)){
            return  true;
        }
        return false;
    }

    public boolean canInCapacityLimitation(long time) {
    	int beforeloc=getBeforeLocIndex(time);
    	int afterloc=getAfterLocIndex(time);
    	
    	if(beforeloc>=0) {
    		return beforeCapacity[beforeloc]>0;
    	}
    	
    	if(afterloc>=0) {
    		return afterCapacity[afterloc]>0;
    	}
    	
    	return true;
    }
    
    public void paybackCapacityLimitation(long time) {
    	int beforeloc=getBeforeLocIndex(time);
    	int afterloc=getAfterLocIndex(time);
    	
    	if(beforeloc>=0) {
    		if(beforeCapacity[beforeloc]<2) {
    			++beforeCapacity[beforeloc];
    		}else {
    			throw new RuntimeException("容量资源归还异常！");
    		}
    	}
    	
    	if(afterloc>=0) {
    		if(afterCapacity[afterloc]<2) {
    			++afterCapacity[afterloc];
    		}else {
    			throw new RuntimeException("容量资源归还异常！");
    		}
    	}
    }
    
    public void consumeCapacityLimitation(long time) {
    	int beforeloc=getBeforeLocIndex(time);
    	int afterloc=getAfterLocIndex(time);
    	
    	if(beforeloc>=0) {
    		if(beforeCapacity[beforeloc]>0) {
    			--beforeCapacity[beforeloc];
    		}else {
    			throw new RuntimeException("容量资源竞争异常！");
    		}
    	}
    	
    	if(afterloc>=0) {
    		if(afterCapacity[afterloc]>0) {
    			--afterCapacity[afterloc];
    		}else {
    			throw new RuntimeException("容量资源竞争异常！");
    		}
    	}
    }
    
    //按每5分钟，划分时间片段
    private void generateCapacity(){
    	int beforeCapacityLength=(int)((endBeforeTime-startBeforeTime)/300000)+1;
    	beforeCapacity=new int[beforeCapacityLength];
    	for(int index=0;index<beforeCapacityLength;++index) {
    		beforeCapacity[index]=2;
    	}
    	int afterCapacityLength=(int)((endAfterTime-startAfterTime)/300000)+1;
    	afterCapacity=new int[afterCapacityLength];
    	for(int index=0;index<afterCapacityLength;++index) {
    		afterCapacity[index]=2;
    	}
    }

    //获取当前时间对应的location
    private int getBeforeLocIndex(long time){
        if((time >= startBeforeTime && time <= endBeforeTime)){
            return  (int)((time - startBeforeTime) / 300000);
        }else {
            return -1;
        }
    }
    
    private int getAfterLocIndex(long time){
        if((time >= startAfterTime && time <= endAfterTime)) {	
        	return  (int)((time - startAfterTime) / 300000);
        }
        else {
            return -1;
        }
    }
    
    public void printCapacityLimitation() {
    	System.out.println("before capacity");
    	for(int index=0, len=beforeCapacity.length;index<len;++index) {
    		System.out.println(beforeCapacity[index]);
    	}
    	
    	System.out.println("aft capacity");
    	for(int index=0, len=afterCapacity.length;index<len;++index) {
    		System.out.println(afterCapacity[index]);
    	}
    }

}

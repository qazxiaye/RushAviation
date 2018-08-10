package minion.rushAviation;

import org.apache.poi.ss.formula.functions.Index;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.impl.values.JavaBase64Holder;
import org.apache.xmlbeans.impl.xb.xsdschema.Keybase;
import org.w3c.dom.DOMImplementationList;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.DoubleToLongFunction;
import java.util.function.LongToDoubleFunction;

import javax.sound.midi.MidiDevice.Info;
import javax.swing.plaf.synth.SynthSpinnerUI;
import javax.swing.plaf.synth.SynthStyle;

/**
 * Created by minion rush on 17/8/16.
 */
public class ResultSolver{

	//定义输入数据结构
	private ArrayList<OriginFlight> originFlightArray=new ArrayList<>();  //flightIdIndex based
	private ArrayList<ArrayList<OriginFlight>> originAirLineArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<ArrayList<AirplaneLimitation>> airplaneLimitationArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<ArrayList<AirportClose>> airportCloseArray=new ArrayList<>();  //airportNameIndex based
	private long[][][] travelTimeCollection;  //airplaneTypeIndex, startAirportNameIndex, endAirportNameIndex based
	private boolean[] domesticAirportRecord;  //airportNameIndex based
	private AdjustTimeWindow adjustTimeWindow;
	private ArrayList<Scene> startSceneArray=new ArrayList<>();  //airportNameIndex based
	private ArrayList<Scene> endSceneArray=new ArrayList<>();  //airportNameIndex based
	
	private ArrayList<Scene> stopSceneArray=new ArrayList<>();  //airportNameIndex based, dynamic
	private ArrayList<CapacityLimitation> capacityLimitationArray=new ArrayList<>();  //airportNameIndex based, dynamic
	
	//定义求解数据结构
	private ArrayList<ArrayList<Integer>> airplaneTypeGroupArray=new ArrayList<>();  //airplaneTypeIndex based
	private BrokenTimeWindow brokenTimeWindow;  //self-defined
	private double airlineMaxCostScore=1000;  //self-defined
	private DynamicFlight globalDeputyDynamicFlight1;
	private DynamicFlight globalDeputyDynamicFlight2;
	
	private ArrayList<DynamicFlight> initFlightArray=new ArrayList<>();  //flightIdIndex based
	private ArrayList<ArrayList<DynamicFlight>> initAirLineArray=new ArrayList<>();  //airplaneIdIndex based
	
	private ArrayList<FlightFrag> initHeadFragArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<FlightFrag> initTailFragArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<ArrayList<FlightFrag>> initBodyFragArray=new ArrayList<>();  //airplaneIdIndex based
	
	private ArrayList<ArrayList<Integer>> initHeadMatchArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<ArrayList<Integer>> initNormalTypeGroupArray=new ArrayList<>();  //airplaneTypeIndex based
	private ArrayList<ArrayList<Integer>> initBrokenTypeGroupArray=new ArrayList<>();  //airplaneTypeIndex based
	
	
	private ArrayList<DynamicFlight> iteratorFlightArray=new ArrayList<>();  //flightIdIndex based
	private ArrayList<ArrayList<DynamicFlight>> iteratorAirLineArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<DynamicFlight> iteratorEmptyFlightArray=new ArrayList<>();
	private int iteratorEmptyFlightNumber=0;

	private ArrayList<FlightFrag> iteratorHeadFragArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<FlightFrag> iteratorTailFragArray=new ArrayList<>();  //airplaneIdIndex based
	private ArrayList<ArrayList<FlightFrag>> iteratorBodyFragArray=new ArrayList<>();  //airplaneIdIndex based
	
	private int[] iteratorMatchArray;
	
	private ArrayList<ArrayList<FlightFrag>> iteratorDfsBodyFragArray=new ArrayList<>();  //airportNameIndex based
	private enum IteratorDfsMode{DFSMODE_GREEDY, DFSMODE_GENTLE};
	private IteratorDfsMode iteratorDfsMode=IteratorDfsMode.DFSMODE_GENTLE;
	
	private ArrayList<DeputyFrag> iteratorDfsRunDfragArray=new ArrayList<>();
	private int iteratorDfsRunDfragNumber=0;
	
	private ArrayList<DeputyFrag> iteratorDfsRecDfragArray=new ArrayList<>();
	private int iteratorDfsRecDfragNumber=0;
	private double iteratorDfsRecScore=0;
	private double iteratorDfsRecEarnScore=0;
	private double iteratorDfsRecCostScore=0;
	
	private int iteratorDfsRecResultNumber=0;
	private FlightFrag iteratorDfsHeadFrag=null;
	private int iteratorAirplaneIdIndex=-1;
	private int iteratorAirplaneTypeIndex=-1;
	private FlightFrag iteratorDfsTailFrag=null;
	private int iteratorDfsTailSPortIndex=-1;
	
	private ArrayList<FlightFrag> iteratorNewBodyFragArray=new ArrayList<>();
	int iteratorNewBodyFragNumber=0;
	private ArrayList<FlightFrag> iteratorEmptyFragArray=new ArrayList<>();
	int iteratorEmptyFragNumber=0;
	
	private ArrayList<ArrayList<DeputyFrag>> iteratorResultDfragArray=new ArrayList<>();
	private int[] iteratorResultDfragNumberArray;
	private double[] iteratorResultScoreArray;
	private double[] iteratorResultEarnScoreArray;
	private double[] iteratorResultCostScoreArray;
	
	private enum HeadTailStatus{STATUS_BROKEN, STATUS_FIXED, STATUS_WAIT};
	private HeadTailStatus[] iteratorHeadAirlineStatus;
	
	private int[] resultMatchArray;
	double resultTotalScore=java.lang.Double.MAX_VALUE;
	
	public ResultSolver(InputStream inputStream){
		//读取输入数据
		readInputData(inputStream);
		//设置联程航班标识
		for(int index=0, len=InfoInterpreter.airplaneTypeArray.size();index<len;++index) {
			ArrayList<Integer> airplaneTypeGroupList=new ArrayList<>();
			airplaneTypeGroupArray.add(airplaneTypeGroupList);
		}
		for(int index=0, len=originAirLineArray.size();index<len;++index) {
			//preprocessing
			int planeidindex=index;
			ArrayList<OriginFlight> flightlist=originAirLineArray.get(planeidindex);
			int airplaneTypeIndex=flightlist.get(0).getAirplaneTypeIndex();
			airplaneTypeGroupArray.get(airplaneTypeIndex).add(planeidindex);
			Collections.sort(flightlist);
			//flight interval
			int vertexnumber=flightlist.size();
			for(int indexj=1;indexj<vertexnumber;++indexj) {
				OriginFlight preflight=flightlist.get(indexj-1);
				OriginFlight flight=flightlist.get(indexj);
				int preflightnoindex=preflight.getFlightNoIndex();
				int flightnoindex=flight.getFlightNoIndex();
				int preflightidindex=preflight.getFlightIdIndex();
				int flightidindex=flight.getFlightIdIndex();
				if(preflightnoindex==flightnoindex) {
					preflight.setConnected(flightidindex, true);
					flight.setConnected(preflightidindex, false);
				}
			}
			//end airport
			int lastindex=flightlist.size()-1;
		}
	}

	public void runSolver(){

		setupSolveStructure();

		initAirlineFrag();
		
		initAirlineMatch();

		long startTime=System.currentTimeMillis();

		resetIteratorStructure();

		iteratorAirlines();

		resultAirlines();

		long endTime=System.currentTimeMillis();

		System.out.println("求解消耗时间："+(endTime-startTime)+"ms");

	}

	public double generateResult(String resultDataFilePath){
		System.out.println("generateResult\n");
		
		double resultTotalAdjustScore=0;
	    double resultTotalCancelScore=0;
	    double resultTotalChangePlaneTypeScore=0;
	    double resultTotalChangePlaneScore=0;
	    double resultTotalStraightenScore=0;
	    double resultTotalAheadScore=0;
	    double resultTotalDelayScore=0;
	    double reusltTotalScore=0;
	    
		try {
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(resultDataFilePath));
			String resultDetailPath="data/mycsv/test_resultdetail.csv";
			BufferedWriter resultDetailWriter=new BufferedWriter(new FileWriter(resultDetailPath));
			
			for(int index=0, len=iteratorFlightArray.size();index<len;++index) {
				DynamicFlight dynamicFlight=iteratorFlightArray.get(index);
				resultWriter.write(dynamicFlight.printResultFlight());
				resultDetailWriter.write(dynamicFlight.printDynamicFlight());
				resultTotalAdjustScore+=dynamicFlight.getAdjustScore();
				resultTotalCancelScore+=dynamicFlight.getCancelScore();
				resultTotalChangePlaneTypeScore+=dynamicFlight.getChangePlaneTypeScore();
				resultTotalChangePlaneScore+=dynamicFlight.getChangePlaneScore();
				resultTotalStraightenScore+=dynamicFlight.getStraightenScore();
				resultTotalAheadScore+=dynamicFlight.getAheadScore();
				resultTotalDelayScore+=dynamicFlight.getDelayScore();
			}
			resultTotalScore=resultTotalAdjustScore+resultTotalCancelScore+resultTotalChangePlaneTypeScore
								+resultTotalChangePlaneScore+resultTotalStraightenScore+resultTotalAheadScore
								+resultTotalDelayScore;
			/*
			for(int index=0, len=resultEmptyFlightNumber;index<len;++index) {
				if(!resultEmptyFlightArray.get(index).isCancel()){
					resultWriter.write(resultEmptyFlightArray.get(index).printResultFlight());
					resultDetailWriter.write(resultEmptyFlightArray.get(index).printDynamicFlight());
				}
			}
			*/
			
			System.out.println("resultTotalAdjustScore: "+resultTotalAdjustScore);
			System.out.println("resultTotalCancelScore: "+resultTotalCancelScore);
			System.out.println("resultTotalChangePlaneTypeScore: "+resultTotalChangePlaneTypeScore);
			System.out.println("resultTotalChangePlaneScore: "+resultTotalChangePlaneScore);
			System.out.println("resultTotalStraightenScore: "+resultTotalStraightenScore);
			System.out.println("resultTotalAheadScore: "+resultTotalAheadScore);
			System.out.println("resultTotalDelayScore: "+resultTotalDelayScore);
			
			resultWriter.close();
			resultDetailWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("generateResult done\n\n");
		return resultTotalScore;
	}
	
	private void setupSolveStructure() {
		
		Configuration.setupTypeChangeParam();
		//设置恢复窗口限制
        long startAdjustTime = InfoInterpreter.timeStringToDate("2017/05/06 06:00").getTime();
        long endAdjustTime = InfoInterpreter.timeStringToDate("2017/05/09 00:00").getTime();
        adjustTimeWindow = new AdjustTimeWindow(startAdjustTime, endAdjustTime);
        //设置单位时间容量限制
        long startBeforeTime = InfoInterpreter.timeStringToDate("2017/05/06 15:00").getTime();
        long endBeforeTime = InfoInterpreter.timeStringToDate("2017/05/06 16:00").getTime();
        long startAfterTime = InfoInterpreter.timeStringToDate("2017/05/07 17:00").getTime();
        long endAfterTime = InfoInterpreter.timeStringToDate("2017/05/07 19:00").getTime();
        CapacityLimitation capacityLimitation=null;
        for(int index=0, len=InfoInterpreter.airportNameArray.size();index<len;++index) {
        	capacityLimitation=null;
        	Scene startScene=startSceneArray.get(index);
        	if(startScene!=null) {
        		capacityLimitation = new CapacityLimitation(startBeforeTime, endBeforeTime, startAfterTime, endAfterTime);
        	}
        	capacityLimitationArray.add(capacityLimitation);  //element could be null
        }
        
        //设置航班打散窗口限制
        long startBrokenTime = InfoInterpreter.timeStringToDate("2017/05/06 14:00").getTime();
        long endBrokenTime = InfoInterpreter.timeStringToDate("2017/05/08 08:00").getTime();
        brokenTimeWindow = new BrokenTimeWindow(startBrokenTime, endBrokenTime);
		
        globalDeputyDynamicFlight1=new DynamicFlight();
        globalDeputyDynamicFlight2=new DynamicFlight();
        
		DynamicFlight tempDynamicFlight=null;
		for(int index=0, len=originFlightArray.size();index<len;++index) {
			tempDynamicFlight=new DynamicFlight(originFlightArray.get(index));
			initFlightArray.add(tempDynamicFlight);
			tempDynamicFlight=new DynamicFlight(originFlightArray.get(index));
			iteratorFlightArray.add(tempDynamicFlight);
		}
		
		int tempFlightIdIndex;
		for(int indexi=0, leni=originAirLineArray.size();indexi<leni;++indexi) {
			ArrayList<OriginFlight> originAirLineList=originAirLineArray.get(indexi);
			ArrayList<DynamicFlight> initAirLineList=new ArrayList<>();
			ArrayList<DynamicFlight> iteratorAirLineList=new ArrayList<>();
			for(int indexj=0, lenj=originAirLineList.size();indexj<lenj;++indexj) {
				tempFlightIdIndex=originAirLineList.get(indexj).getFlightIdIndex();
				initAirLineList.add(initFlightArray.get(tempFlightIdIndex));
				iteratorAirLineList.add(iteratorFlightArray.get(tempFlightIdIndex));
			}
			OriginFlight preOriginFlight=originAirLineList.get(0);
			for(int indexj=1, lenj=originAirLineList.size();indexj<lenj;++indexj) {
				OriginFlight originFlight=originAirLineList.get(indexj);
				preOriginFlight.setNextOriginFlight(originFlight);
				originFlight.setPreOriginFlight(preOriginFlight);
				preOriginFlight=originFlight;
			}
			DynamicFlight preDynamicFlight=initAirLineList.get(0);
			for(int indexj=1, lenj=initAirLineList.size();indexj<lenj;++indexj) {
				DynamicFlight dynamicFlight=initAirLineList.get(indexj);
				preDynamicFlight.setNextDynamicFlight(dynamicFlight);
				dynamicFlight.setPreDynamicFlight(preDynamicFlight);
				preDynamicFlight=dynamicFlight;
			}
			preDynamicFlight=iteratorAirLineList.get(0);
			for(int indexj=1, lenj=iteratorAirLineList.size();indexj<lenj;++indexj) {
				DynamicFlight dynamicFlight=iteratorAirLineList.get(indexj);
				preDynamicFlight.setNextDynamicFlight(dynamicFlight);
				dynamicFlight.setPreDynamicFlight(preDynamicFlight);
				preDynamicFlight=dynamicFlight;
			}
			
			ArrayList<Integer> initHeadMatchList=new ArrayList<>();
			initHeadMatchArray.add(initHeadMatchList);
			ArrayList<FlightFrag> initBodyFragList=new ArrayList<>();
			initBodyFragArray.add(initBodyFragList);
			ArrayList<FlightFrag> iteratorBodyFragList=new ArrayList<>();
			iteratorBodyFragArray.add(iteratorBodyFragList);
			initAirLineArray.add(initAirLineList);
			iteratorAirLineArray.add(iteratorAirLineList);
		}
		
		for(int index=0, len=airplaneTypeGroupArray.size();index<len;++index) {
			ArrayList<Integer> initNormalTypeGroupList=new ArrayList<>();
			initNormalTypeGroupArray.add(initNormalTypeGroupList);
			ArrayList<Integer> initBrokenTypeGroupList=new ArrayList<>();
			initBrokenTypeGroupArray.add(initBrokenTypeGroupList);
		}
		
		for(int index=0, len=airportCloseArray.size();index<len;++index) {
			ArrayList<FlightFrag> iteratorDfsBodyFragList=new ArrayList<>();
			iteratorDfsBodyFragArray.add(iteratorDfsBodyFragList);
		}
		
		DynamicFlight tempEmptyFlight=null;
		FlightFrag tempNewBodyFrag=null;
		for(int index=9001;index<9200;++index) {
			String tpstring=String.valueOf(index);
			Integer tpindex=InfoInterpreter.flightIdMap.get(tpstring);
			if(tpindex==null) {
				tpindex=InfoInterpreter.flightIdArray.size();
				InfoInterpreter.flightIdArray.add(tpstring);
				InfoInterpreter.flightIdMap.put(tpstring, tpindex);
			}
			tempEmptyFlight=new DynamicFlight(tpindex);
			iteratorEmptyFlightArray.add(tempEmptyFlight);
			tempNewBodyFrag=new FlightFrag();
			iteratorEmptyFragArray.add(tempNewBodyFrag);
			tempNewBodyFrag=new FlightFrag();
			iteratorNewBodyFragArray.add(tempNewBodyFrag);
		}
		
		DeputyFrag tempDeputyFrag=null;
		for(int index=0;index<20;++index) {
			tempDeputyFrag=new DeputyFrag();
			iteratorDfsRunDfragArray.add(tempDeputyFrag);
			tempDeputyFrag=new DeputyFrag();
			iteratorDfsRecDfragArray.add(tempDeputyFrag);
		}
		
		iteratorHeadAirlineStatus=new HeadTailStatus[originAirLineArray.size()];
		
		iteratorMatchArray=new int[originAirLineArray.size()];
		for(int index=0, len=iteratorMatchArray.length;index<len;++index) {
			iteratorMatchArray[index]=-1;
		}
		
		DeputyFrag deputyFrag=null;
		for(int index=0,len=initAirLineArray.size();index<len;++index) {
			ArrayList<DeputyFrag> iteratorResultDfragList=new ArrayList<>();
			for(int indexi=0,leni=20;indexi<leni;++indexi) {
				deputyFrag=new DeputyFrag();
				iteratorResultDfragList.add(deputyFrag);
			}
			iteratorResultDfragArray.add(iteratorResultDfragList);
		}
		iteratorResultDfragNumberArray=new int[initAirLineArray.size()];
		iteratorResultScoreArray=new double[initAirLineArray.size()];
		iteratorResultEarnScoreArray=new double[initAirLineArray.size()];
		iteratorResultCostScoreArray=new double[initAirLineArray.size()];
		
		resultMatchArray=new int[originAirLineArray.size()];
		for(int index=0, len=resultMatchArray.length;index<len;++index) {
			resultMatchArray[index]=-1;
		}
		resultTotalScore=java.lang.Double.MAX_VALUE;
	}

	private void initAirlineFrag() {
		System.out.println("initAirlines\n");
		
		try{
			String initflightPath = "data/mycsv/test_initflight.csv";
			BufferedWriter initflightWriter = new BufferedWriter(new FileWriter(initflightPath));
			String initflightheadPath = "data/mycsv/test_initflighthead.csv";
			BufferedWriter initflightheadWriter = new BufferedWriter(new FileWriter(initflightheadPath));
			String initflightbodyPath = "data/mycsv/test_initflightbody.csv";
			BufferedWriter initflightbodyWriter = new BufferedWriter(new FileWriter(initflightbodyPath));
			String initflighttailPath = "data/mycsv/test_initflighttail.csv";
			BufferedWriter initflighttailWriter = new BufferedWriter(new FileWriter(initflighttailPath));
			String initflightcancelPath = "data/mycsv/test_initflightcancel.csv";
			BufferedWriter initflightcancelWriter = new BufferedWriter(new FileWriter(initflightcancelPath));
			
			String initfragheadPath = "data/mycsv/test_initfraghead.csv";
			BufferedWriter initfragheadWriter = new BufferedWriter(new FileWriter(initfragheadPath));
			String initfragbodyPath = "data/mycsv/test_initfragbody.csv";
			BufferedWriter initfragbodyWriter = new BufferedWriter(new FileWriter(initfragbodyPath));
			String initfragtailPath = "data/mycsv/test_initfragtail.csv";
			BufferedWriter initfragtailWriter = new BufferedWriter(new FileWriter(initfragtailPath));
			
			int normalAirLineCount=0;
			
			int totalFlightCount=0;
			int headFlightCount=0;
			int tailFlightCount=0;
			int bodyFlightCount=0;
			int bodyFlightBrokenCount=0;
			int bodyFlightNormalCount=0;
			int cancelFlightCount=0;
			
			int startCapacityFlightCount=0;
			int endCapacityFlightCount=0;
			int startSceneFlightCount=0;
			int endSceneFlightCount=0;
			int canAheadFlightCount=0;
			int canDelayFlightCount=0;

			int bodyFragCount=0;
			int bodyFragBrokenCount=0;
			int bodyFragNormalCount=0;
			int singleFragCount=0;
			int singleFragBrokenCount=0;
			int singleFragNormalCount=0;
			int connectFragCount=0;
			int connectFragBrokenCount=0;
			int connectFragNormalCount=0;
			int canStaightenFragCount=0;
			
			for(int index=0, len=initAirLineArray.size();index<len;++index) {
				int airplaneIdIndex=index;
				int firstBrokenIndex=-1;
				int lastBrokenIndex=-1;
				ArrayList<DynamicFlight> initAirLineList=initAirLineArray.get(airplaneIdIndex);
				ArrayList<DynamicFlight> iteratorAirLineList=iteratorAirLineArray.get(airplaneIdIndex);
				int initAirLineListLength=initAirLineList.size();
				ArrayList<FlightFrag> initBodyFragList=initBodyFragArray.get(airplaneIdIndex);
				ArrayList<FlightFrag> iteratorBodyFragList=iteratorBodyFragArray.get(airplaneIdIndex);
				int airplaneTypeIndex=initAirLineList.get(0).getAirplaneTypeIndex();
				//locate firstBrokenIndex
				DynamicFlight preDynamicFlight=initAirLineList.get(0);
				DynamicFlight dynamicFlight=null;
				for(int indexi=1;indexi<initAirLineListLength;++indexi) {
					dynamicFlight=initAirLineList.get(indexi);
					Scene startScene=getStartScene(preDynamicFlight);
					Scene endScene=getEndScene(preDynamicFlight);
					Scene stopScene=getStopScene(preDynamicFlight, dynamicFlight);
					CapacityLimitation startCapacityLimitation=getInStartCapacityLimitation(preDynamicFlight);
					CapacityLimitation endCapacityLimitation=getInEndCapacityLimitation(preDynamicFlight);
					if(startScene!=null||endScene!=null||stopScene!=null||
						startCapacityLimitation!=null||endCapacityLimitation!=null) {
						firstBrokenIndex=indexi-1;
						break;
					}
					preDynamicFlight=dynamicFlight;
				}
				
				boolean isAirlineNormal=false;
				if(firstBrokenIndex<0) {
					//original normal airline
					isAirlineNormal=true;
					++normalAirLineCount;
					Integer normalAirlineIdIndex=airplaneIdIndex;
					ArrayList<Integer> initNormalTypeGroupList=initNormalTypeGroupArray.get(airplaneTypeIndex);
					initNormalTypeGroupList.add(normalAirlineIdIndex);
					//locate firstBrokenIndex
					for(int indexi=0;indexi<initAirLineListLength;++indexi) {
						dynamicFlight=initAirLineList.get(indexi);
						long startDateTime=dynamicFlight.getStartDateTime();
						if(brokenTimeWindow.isInBrokenTimeWindow(startDateTime)) {
							firstBrokenIndex=indexi;
							break;
						}
					}
					//locate lastBrokenIndex
					for(int indexi=initAirLineListLength-1;indexi>=firstBrokenIndex;--indexi) {
						dynamicFlight=initAirLineList.get(indexi);
						long endDateTime=dynamicFlight.getEndDateTime();
						if(brokenTimeWindow.isInBrokenTimeWindow(endDateTime)) {
							lastBrokenIndex=indexi;
							break;
						}
					}
					if(initAirLineList.get(firstBrokenIndex).isConnectAftPart()) {
						--firstBrokenIndex;
					}
					if(initAirLineList.get(lastBrokenIndex).isConnectPrePart()) {
						++lastBrokenIndex;
					}
					bodyFlightNormalCount+=lastBrokenIndex-firstBrokenIndex+1;
				}else {
					isAirlineNormal=false;
					//original broken airline
					Integer brokenAirlineIdIndex=airplaneIdIndex;
					ArrayList<Integer> initBrokenTypeGroupList=initBrokenTypeGroupArray.get(airplaneTypeIndex);
					initBrokenTypeGroupList.add(brokenAirlineIdIndex);
					//locate lastBrokenIndex
					dynamicFlight=initAirLineList.get(initAirLineListLength-1);
					preDynamicFlight=null;
					for(int indexi=initAirLineListLength-2;indexi>=0;--indexi) {
						preDynamicFlight=initAirLineList.get(indexi);
						Scene startScene=getStartScene(dynamicFlight);
						Scene endScene=getEndScene(dynamicFlight);
						Scene stopScene=getStopScene(preDynamicFlight, dynamicFlight);
						CapacityLimitation startCapacityLimitation=getInStartCapacityLimitation(dynamicFlight);
						CapacityLimitation endCapacityLimitation=getInEndCapacityLimitation(dynamicFlight);
						if(startScene!=null||endScene!=null||stopScene!=null||
								startCapacityLimitation!=null||endCapacityLimitation!=null) {
								lastBrokenIndex=indexi+1;
								break;
							}
						dynamicFlight=preDynamicFlight;
					}
					
					//expand broken body span
					int tempFirstBrokenIndex=0;
					for(int indexi=0;indexi<initAirLineListLength;++indexi) {
						dynamicFlight=initAirLineList.get(indexi);
						long startDateTime=dynamicFlight.getStartDateTime();
						if(brokenTimeWindow.isInBrokenTimeWindow(startDateTime)) {
							if(indexi<firstBrokenIndex) {
								firstBrokenIndex=indexi;
							}
							break;
						}
					}
					for(int indexi=initAirLineListLength-1;indexi>=firstBrokenIndex;--indexi) {
						dynamicFlight=initAirLineList.get(indexi);
						long endDateTime=dynamicFlight.getEndDateTime();
						if(brokenTimeWindow.isInBrokenTimeWindow(endDateTime)) {
							if(indexi>lastBrokenIndex) {
								lastBrokenIndex=indexi;
							}
							break;
						}
					}
					
					if(initAirLineList.get(firstBrokenIndex).isConnectAftPart()) {
						DynamicFlight aftConnectedFlight=initAirLineList.get(firstBrokenIndex);
						DynamicFlight preConnectedFlight=initAirLineList.get(firstBrokenIndex-1);
						long preStartDateTime=preConnectedFlight.getStartDateTime();
						if(!adjustTimeWindow.isInAdjustTimeWindow(preStartDateTime)) {
							//deal with adjust window problem
							Scene startScene=getStartScene(preConnectedFlight);
							boolean isAheadSuccess=false;
							if(startScene!=null) {
								//try ahead
								globalDeputyDynamicFlight1.copyFrom(aftConnectedFlight);
								int preFlightIdIndex=preConnectedFlight.getFlightIdIndex();
								int aftFlightIdIndex=aftConnectedFlight.getFlightIdIndex();
								long flightInterval=getFlightIntervalTime(preFlightIdIndex,aftFlightIdIndex);
								long earliestStartDateTime=preConnectedFlight.getEndDateTime()+flightInterval;
								isAheadSuccess=fitAheadDynamicFlight(globalDeputyDynamicFlight1,earliestStartDateTime);
							}
							if(isAheadSuccess) {
								aftConnectedFlight.copyFrom(globalDeputyDynamicFlight1);
							}else {
								//ahead failed, try delay
								globalDeputyDynamicFlight1.copyFrom(aftConnectedFlight);
								boolean isDelaySuccess=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
								if(isDelaySuccess) {
									aftConnectedFlight.copyFrom(globalDeputyDynamicFlight1);
								}else {
									throw new RuntimeException("恢复窗口与联程航班冲突，无解！");
								}
							}									
							++firstBrokenIndex;
						}else {
							--firstBrokenIndex;
						}
					}
					if(initAirLineList.get(lastBrokenIndex).isConnectPrePart()) {
						++lastBrokenIndex;
					}
					bodyFlightBrokenCount+=lastBrokenIndex-firstBrokenIndex+1;
				}
					
				DynamicFlight firstDynamicFlight=null;
				DynamicFlight lastDynamicFlight=null;
				double totalImportRatio=0;
				double totalScore=0;
				//convert head flights to head fragment
				FlightFrag headFlightFrag=null;
				firstDynamicFlight=initAirLineList.get(0);
				if(firstBrokenIndex>0) {
					totalImportRatio=0;
					totalScore=0;
					for(int indexi=0;indexi<firstBrokenIndex;++indexi) {
						totalImportRatio+=initAirLineList.get(indexi).getImportRatio();
						totalScore+=initAirLineList.get(indexi).getScore();
					}
					lastDynamicFlight=initAirLineList.get(firstBrokenIndex-1);
					//remark: last flight of head may occupy end capacity
					CapacityLimitation capacityLimitation=getInEndCapacityLimitation(lastDynamicFlight);
					if(capacityLimitation!=null) {
						lastDynamicFlight.setIsOriginEndInCapacity(true);
					}					
					headFlightFrag=new FlightFrag(FlightFrag.FragType.FRAG_HEAD,firstBrokenIndex,totalImportRatio,
													totalScore,firstDynamicFlight,lastDynamicFlight);
					initHeadFragArray.add(headFlightFrag);
					
					//construct iterator head fragment
					firstDynamicFlight=iteratorAirLineList.get(0);
					lastDynamicFlight=iteratorAirLineList.get(firstBrokenIndex-1);
					headFlightFrag=new FlightFrag(FlightFrag.FragType.FRAG_HEAD,firstBrokenIndex,totalImportRatio,
													totalScore,firstDynamicFlight,lastDynamicFlight);
					iteratorHeadFragArray.add(headFlightFrag);
				}else {
					headFlightFrag=new FlightFrag(firstDynamicFlight);
					initHeadFragArray.add(headFlightFrag);
					
					//construct iterator head fragment
					firstDynamicFlight=iteratorAirLineList.get(0);
					headFlightFrag=new FlightFrag(firstDynamicFlight);
					iteratorHeadFragArray.add(headFlightFrag);
				}
				
				boolean canAhead=false;
				boolean canDelay=false;
				for(int indexi=firstBrokenIndex;indexi<=lastBrokenIndex;++indexi) {
					//mark dynamic flight start/end capacity and scene
					dynamicFlight=initAirLineList.get(indexi);
					canAhead=false;
					Scene startScene=getStartScene(dynamicFlight);
					if(startScene!=null) {
						++startSceneFlightCount;
						dynamicFlight.setIsOriginStartInScene(true);
						globalDeputyDynamicFlight1.copyFrom(dynamicFlight);
						canAhead=fitAheadDynamicFlight(globalDeputyDynamicFlight1, 0);
					}
					Scene endScene=getEndScene(dynamicFlight);
					if(endScene!=null) {
						++endSceneFlightCount;
						dynamicFlight.setIsOriginEndInScene(true);
					}
					globalDeputyDynamicFlight1.copyFrom(dynamicFlight);
					canDelay=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
					dynamicFlight.setCanAhead(canAhead);
					dynamicFlight.setCanDelay(canDelay);
					if(canAhead) {
						++canAheadFlightCount;
					}
					if(canDelay) {
						++canDelayFlightCount;
					}
					CapacityLimitation startCapacityLimitation=getInStartCapacityLimitation(dynamicFlight);
					CapacityLimitation endCapacityLimitation=getInEndCapacityLimitation(dynamicFlight);
					if(startCapacityLimitation!=null) {
						++startCapacityFlightCount;
						dynamicFlight.setIsOriginStartInCapacity(true);
					}
					if(endCapacityLimitation != null) {
						++endCapacityFlightCount;
						dynamicFlight.setIsOriginEndInCapacity(true);
					}
				}
				for(int indexi=firstBrokenIndex;indexi<=lastBrokenIndex;++indexi) {
					//convert broken flights to broken fragments
					if(!initAirLineList.get(indexi).isConnected()) {
						dynamicFlight=initAirLineList.get(indexi);
						if(dynamicFlight.getCanAhead()||dynamicFlight.getCanDelay()) {
							++bodyFlightCount;
							++bodyFragCount;
							++singleFragCount;
							if(isAirlineNormal) {
								++bodyFragNormalCount;
							}else {
								++bodyFragBrokenCount;
							}
							if(isAirlineNormal) {
								++singleFragNormalCount;
							}else {
								++singleFragBrokenCount;
							}
							//generate body single fragment
							totalImportRatio=dynamicFlight.getImportRatio();
							totalScore=dynamicFlight.getScore();
							FlightFrag bodySingleFrag=new FlightFrag(FlightFrag.FragType.FRAG_BODY_SINGLE,1,totalImportRatio,
																		totalScore,dynamicFlight,dynamicFlight);
							initBodyFragList.add(bodySingleFrag);
							
							//construct iterator body single fragment
							dynamicFlight=iteratorAirLineList.get(indexi);
							bodySingleFrag=new FlightFrag(FlightFrag.FragType.FRAG_BODY_SINGLE,1,totalImportRatio,
															totalScore,dynamicFlight,dynamicFlight);
							iteratorBodyFragList.add(bodySingleFrag);
						}else {
							++cancelFlightCount;
							--bodyFlightBrokenCount;
							dynamicFlight.cancelFlight();
						}
					}
					else if(initAirLineList.get(indexi).isConnectPrePart()) {
						bodyFlightCount+=2;
						++bodyFragCount;
						++connectFragCount;
						if(isAirlineNormal) {
							++bodyFragNormalCount;
						}else {
							++bodyFragBrokenCount;
						}
						if(isAirlineNormal) {
							++connectFragNormalCount;
						}else {
							++connectFragBrokenCount;
						}
						//generate body connected fragment
						DynamicFlight preConnectedFlight=initAirLineList.get(indexi);
						DynamicFlight aftConnectedFlight=initAirLineList.get(indexi+1);
						totalImportRatio=preConnectedFlight.getImportRatio()+aftConnectedFlight.getImportRatio();
						totalScore=preConnectedFlight.getScore()+aftConnectedFlight.getScore();
						FlightFrag bodyConnectFrag=new FlightFrag(FlightFrag.FragType.FRAG_BODY_CONNECT,2,totalImportRatio,
																	totalScore,preConnectedFlight,aftConnectedFlight);
						if(preConnectedFlight.isDomestic()&&aftConnectedFlight.isDomestic()) {
							if(preConnectedFlight.isOriginEndInScene()||aftConnectedFlight.isOriginStartInScene()) {
								++canStaightenFragCount;
								bodyConnectFrag.setCanStraighten(true);
							}
						}
						initBodyFragList.add(bodyConnectFrag);
						//construct iterator body single fragment
						preConnectedFlight=iteratorAirLineList.get(indexi);
						aftConnectedFlight=iteratorAirLineList.get(indexi+1);
						bodyConnectFrag=new FlightFrag(FlightFrag.FragType.FRAG_BODY_CONNECT,2,totalImportRatio,
														totalScore,preConnectedFlight,aftConnectedFlight);
						iteratorBodyFragList.add(bodyConnectFrag);
					}
				}
				
				//convert tail flights to tail fragment
				firstDynamicFlight=initAirLineList.get(lastBrokenIndex+1);
				lastDynamicFlight=initAirLineList.get(initAirLineListLength-1);
				totalImportRatio=0;
				totalScore=0;
				for(int indexi=lastBrokenIndex+1;indexi<initAirLineListLength;++indexi) {
					totalImportRatio+=initAirLineList.get(indexi).getImportRatio();
					totalScore+=initAirLineList.get(indexi).getScore();
				}
				FlightFrag tailFlightFrag=new FlightFrag(FlightFrag.FragType.FRAG_TAIL,initAirLineListLength-lastBrokenIndex-1,
															totalImportRatio,totalScore,firstDynamicFlight,lastDynamicFlight);
				initTailFragArray.add(tailFlightFrag);
				
				//construct iterator tail fragment
				firstDynamicFlight=iteratorAirLineList.get(lastBrokenIndex+1);
				lastDynamicFlight=iteratorAirLineList.get(initAirLineListLength-1);
				tailFlightFrag=new FlightFrag(FlightFrag.FragType.FRAG_TAIL,initAirLineListLength-lastBrokenIndex-1,
												totalImportRatio,totalScore,firstDynamicFlight,lastDynamicFlight);
				iteratorTailFragArray.add(tailFlightFrag);
				
				for(int indexi=0, leni=initAirLineListLength;indexi<leni;++indexi) {
					dynamicFlight=initAirLineList.get(indexi);
					initflightWriter.write(dynamicFlight.printDynamicFlight());
					if(indexi<firstBrokenIndex) {
						initflightheadWriter.write(dynamicFlight.printDynamicFlight());
					}else if(indexi<=lastBrokenIndex) {
						if(dynamicFlight.isCancel()) {
							initflightcancelWriter.write(dynamicFlight.printDynamicFlight());
						}else {
							initflightbodyWriter.write(dynamicFlight.printDynamicFlight());
						}
					}else {
						initflighttailWriter.write(dynamicFlight.printDynamicFlight());
					}
				}
				totalFlightCount+=initAirLineListLength;
				headFlightCount+=firstBrokenIndex;
				tailFlightCount+=initAirLineListLength-lastBrokenIndex-1;
			}
			
			for(int index=0, len=initHeadFragArray.size();index<len;++index) {
				initfragheadWriter.write(initHeadFragArray.get(index).printFlightFrag());				
			}
			for(int index=0, len=initBodyFragArray.size();index<len;++index) {
				ArrayList<FlightFrag> initBodyFragList=initBodyFragArray.get(index);
				for(int indexi=0, leni=initBodyFragList.size();indexi<leni;++indexi) {
					initfragbodyWriter.write(initBodyFragList.get(indexi).printFlightFrag());
				}
			}
			for(int index=0, len=initTailFragArray.size();index<len;++index) {
				initfragtailWriter.write(initTailFragArray.get(index).printFlightFrag());
			}
			
			System.out.println("normalAirLineCount: "+normalAirLineCount+"\n");
			
			System.out.println("totalFlightCount: "+totalFlightCount);
			System.out.println("headFlightCount: "+headFlightCount);
			System.out.println("tailFlightCount: "+tailFlightCount);
			System.out.println("bodyFlightCount: "+bodyFlightCount);
			System.out.println("bodyFlightNormalCount: "+bodyFlightNormalCount);
			System.out.println("bodyFlightBrokenCount: "+bodyFlightBrokenCount);
			System.out.println("cancelFlightCount: "+cancelFlightCount+"\n");
			
			System.out.println("startCapacityFlightCount: "+startCapacityFlightCount);
			System.out.println("endCapacityFlightCount: "+endCapacityFlightCount);
			System.out.println("startSceneFlightCount: "+startSceneFlightCount);
			System.out.println("endSceneFlightCount: "+endSceneFlightCount);
			System.out.println("canAheadFlightCount: "+canAheadFlightCount);
			System.out.println("canDelayFlightCount: "+canDelayFlightCount+"\n");
			
			System.out.println("bodyFragCount: "+bodyFragCount);
			System.out.println("bodyFragNormalCount: "+bodyFragNormalCount);
			System.out.println("bodyFragBrokenCount: "+bodyFragBrokenCount);
			System.out.println("singleFragCount: "+singleFragCount);
			System.out.println("singleFragNormalCount: "+singleFragNormalCount);
			System.out.println("singleFragBrokenCount: "+singleFragBrokenCount);
			
			System.out.println("connectFragCount: "+connectFragCount);
			System.out.println("connectFragNormalCount: "+connectFragNormalCount);
			System.out.println("connectFragBrokenCount: "+connectFragBrokenCount);
			System.out.println("canStaightenFragCount: "+canStaightenFragCount+"\n");
			
			initflightWriter.close();
			initflightheadWriter.close();
			initflightbodyWriter.close();
			initflighttailWriter.close();
			initflightcancelWriter.close();
			
			initfragheadWriter.close();
			initfragbodyWriter.close();
			initfragtailWriter.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}	
		System.out.println("initAirlines done\n\n");
	}

	private void initAirlineMatch() {
		System.out.println("initAirlineMatch\n");
		
		for(int index=0, len=initNormalTypeGroupArray.size();index<len;++index) {
			int airplaneTypeIndex=index;
			ArrayList<Integer> initNormalTypeGroupList=initNormalTypeGroupArray.get(airplaneTypeIndex);
			for(int indexi=0, leni=initNormalTypeGroupList.size();indexi<leni;++indexi) {
				Integer headFragIndex=initNormalTypeGroupList.get(indexi);
				Integer tailFragIndex=headFragIndex;
				ArrayList<Integer> initHeadMatchList=initHeadMatchArray.get(headFragIndex);
				initHeadMatchList.add(tailFragIndex);
			}
		}
		
		for(int index=0 ,len=initBrokenTypeGroupArray.size();index<len;++index) {
			int airplaneTypeIndex=index;
			ArrayList<Integer> initBrokenTypeGroupList=initBrokenTypeGroupArray.get(airplaneTypeIndex);
			for(int indexi=0,leni=initBrokenTypeGroupList.size();indexi<leni;++indexi) {
				Integer headFragIndex=initBrokenTypeGroupList.get(indexi);
				ArrayList<Integer> initHeadMatchList=initHeadMatchArray.get(headFragIndex);
				for(int indexj=0,lenj=initBrokenTypeGroupList.size();indexj<lenj;++indexj) {
					Integer tailFragIndex=initBrokenTypeGroupList.get(indexj);
					if(headFragIndex==tailFragIndex) {
						//head matches with itself's tail
						initHeadMatchList.add(tailFragIndex);
					}else {
						//test if match or not
						boolean matchFlag=true;
						int airplaneIdIndex=headFragIndex;
						FlightFrag tailFrag=initTailFragArray.get(tailFragIndex);
						DynamicFlight dynamicFlight=tailFrag.getFirstDynamicFlight();
						while(dynamicFlight!=null) {
							int startAirportIndex=dynamicFlight.getStartAirportIndex();
							int endAirportIndex=dynamicFlight.getEndAirportIndex();
							if(queryIsLimitation(airplaneIdIndex, startAirportIndex, endAirportIndex)) {
								matchFlag=false;
								break;
							}
							dynamicFlight=dynamicFlight.getNextDynamicFlight();
						}
						if(matchFlag) {
							initHeadMatchList.add(tailFragIndex);
						}
					}
				}
			}
		}
		
		System.out.println("initAirlineMatch done\n\n");
	}
	
	private void resetIteratorStructure(){
		
		System.out.println("resetIteratorStructure\n");

		try{
			String iterflightPath = "data/mycsv/test_iterflight.csv";
			BufferedWriter iterflightWriter = new BufferedWriter(new FileWriter(iterflightPath));
			
			String iterfragheadPath = "data/mycsv/test_iterfraghead.csv";
			BufferedWriter iterfragheadWriter = new BufferedWriter(new FileWriter(iterfragheadPath));
			String iterfragbodyPath = "data/mycsv/test_iterfragbody.csv";
			BufferedWriter iterfragbodyWriter = new BufferedWriter(new FileWriter(iterfragbodyPath));
			String iterfragtailPath = "data/mycsv/test_iterfragtail.csv";
			BufferedWriter iterfragtailWriter = new BufferedWriter(new FileWriter(iterfragtailPath));
			
			for(int index=0, len=stopSceneArray.size();index<len;++index) {
				Scene stopScene=stopSceneArray.get(index);
				if(stopScene!=null) {
					stopScene.resetRemainStopNum();
				}
			}
			
			for(int index=0, len=capacityLimitationArray.size();index<len;++index) {
				CapacityLimitation capacityLimitation=capacityLimitationArray.get(index);
				if(capacityLimitation!=null) {
					capacityLimitation.resetCapacity();
				}
			}
			
			for(int index=0, len=iteratorFlightArray.size();index<len;++index) {
				iteratorFlightArray.get(index).copyFrom(initFlightArray.get(index));
				iterflightWriter.write(iteratorFlightArray.get(index).printDynamicFlight());
			}
			
			iteratorEmptyFlightNumber=0;
	
			for(int index=0, len=iteratorHeadFragArray.size();index<len;++index) {
				iteratorHeadFragArray.get(index).copyFrom(initHeadFragArray.get(index));
				iterfragheadWriter.write(iteratorHeadFragArray.get(index).printFlightFrag());
			}
			
			for(int index=0, len=iteratorBodyFragArray.size();index<len;++index) {
				ArrayList<FlightFrag> initBodyFragList=initBodyFragArray.get(index);
				ArrayList<FlightFrag> iteratorBodyFragList=iteratorBodyFragArray.get(index);
				for(int indexi=0, leni=iteratorBodyFragList.size();indexi<leni;++indexi) {
					iteratorBodyFragList.get(indexi).copyFrom(initBodyFragList.get(indexi));
					iterfragbodyWriter.write(iteratorBodyFragList.get(indexi).printFlightFrag());
				}
			}
			
			for(int index=0, len=iteratorTailFragArray.size();index<len;++index) {
				iteratorTailFragArray.get(index).copyFrom(initTailFragArray.get(index));
				iterfragtailWriter.write(iteratorTailFragArray.get(index).printFlightFrag());
			}
			
	 		for(int index=0, len=iteratorDfsBodyFragArray.size();index<len;++index) {
	 			iteratorDfsBodyFragArray.get(index).clear();
	 		}
	 		
	 		for(int index=0, len=iteratorResultDfragArray.size();index<len;++index) {
	 			iteratorResultDfragNumberArray[index]=0;
	 			iteratorResultScoreArray[index]=0;
	 			iteratorResultEarnScoreArray[index]=0;
	 			iteratorResultCostScoreArray[index]=java.lang.Double.MAX_VALUE;
	 		}
	 		
	 		iteratorEmptyFlightNumber=0;
	 		
			iteratorNewBodyFragNumber=0;
	
			iteratorEmptyFragNumber=0;
			
			for(int index=0, len=iteratorHeadAirlineStatus.length;index<len;++index) {
				iteratorHeadAirlineStatus[index]=HeadTailStatus.STATUS_BROKEN;
			}
			
			for(int index=0, len=iteratorMatchArray.length;index<len;++index) {
				iteratorMatchArray[index]=-1;
			}
			
			
			
			iterflightWriter.close();
			
			iterfragheadWriter.close();
			iterfragbodyWriter.close();
			iterfragtailWriter.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}	
		
		System.out.println("resetIteratorStructure done\n\n");
		
	}

	private void iteratorAirlines() {
		System.out.println("iteratorAirlines\n");
		
		try{
			
			//set self-self match
			for(int index=0, len=iteratorMatchArray.length;index<len;++index) {
				int headAirlineIndex=index;
				int tailAirlineIndex=index;
				iteratorMatchArray[headAirlineIndex]=tailAirlineIndex;
			}
			
			//head occupy capacity first
			for(int index=0, len=iteratorHeadFragArray.size();index<len;++index) {
				FlightFrag headFlightFrag=iteratorHeadFragArray.get(index);
				if(!headFlightFrag.isEmptyFlightFrag()) {
					DynamicFlight lastDynamicFlight=headFlightFrag.getLastDynamicFlight();
					if(lastDynamicFlight.isOriginEndInCapacity()) {
						int endAirportIndex=lastDynamicFlight.getEndAirportIndex();
						long endDateTime=lastDynamicFlight.getEndDateTime();
						CapacityLimitation capacityLimitation=capacityLimitationArray.get(endAirportIndex);
						capacityLimitation.consumeCapacityLimitation(endDateTime);
					}
				}
			}
			
			//put broken body fragments into iteratorDfsBodyFragArray
			int dfsSingleBrokenCount=0;
			int dfsConnectBrokenCount=0;
			int dfsConnectPreBrokenCount=0;
			int dfsConnectAftBrokenCount=0;
			for(int index=0, len=initBrokenTypeGroupArray.size();index<len;++index) {
				int airplaneTypeIndex=index;
				ArrayList<Integer> initBrokenTypeGroupList=initBrokenTypeGroupArray.get(airplaneTypeIndex);
				for(int indexi=0,leni=initBrokenTypeGroupList.size();indexi<leni;++indexi) {
					int airplaneIdIndex=initBrokenTypeGroupList.get(indexi);
					ArrayList<FlightFrag> iteratorBodyFragList=iteratorBodyFragArray.get(airplaneIdIndex);
					for(int indexj=0, lenj=iteratorBodyFragList.size();indexj<lenj;++indexj) {
						FlightFrag bodyFlightFrag=iteratorBodyFragList.get(indexj);
						DynamicFlight firstDynamicFlight=bodyFlightFrag.getFirstDynamicFlight();
						DynamicFlight lastDynamicFlight=bodyFlightFrag.getLastDynamicFlight();
						if(bodyFlightFrag.isBodySingleFrag()) {
							++dfsSingleBrokenCount;
							int startAirportIndex=firstDynamicFlight.getStartAirportIndex();
							ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(startAirportIndex);
							iteratorDfsBodyFragList.add(bodyFlightFrag);
						}else if(bodyFlightFrag.isBodyConnectFrag()) {
							++dfsConnectBrokenCount;
							if(firstDynamicFlight.getCanAhead()||firstDynamicFlight.getCanDelay()) {
								++dfsConnectPreBrokenCount;
								int startAirportIndex=firstDynamicFlight.getStartAirportIndex();
								ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(startAirportIndex);
								iteratorDfsBodyFragList.add(bodyFlightFrag);
							}
							if(lastDynamicFlight.getCanAhead()||lastDynamicFlight.getCanDelay()) {
								++dfsConnectAftBrokenCount;
								int startAirportIndex=lastDynamicFlight.getStartAirportIndex();
								ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(startAirportIndex);
								iteratorDfsBodyFragList.add(bodyFlightFrag);
							}
						}
					}
				}
	 		}
			System.out.println("dfsSingleBrokenCount: "+dfsSingleBrokenCount);
			System.out.println("dfsConnectBrokenCount: "+dfsConnectBrokenCount);
			System.out.println("dfsConnectPreBrokenCount: "+dfsConnectPreBrokenCount);
			System.out.println("dfsConnectAftBrokenCount: "+dfsConnectAftBrokenCount+"\n");
			
			//deal with initial normal airline, with self-served method
			int dfsSingleNormalCount=0;
			int dfsConnectNormalCount=0;
			int dfsConnectPreNormalCount=0;
			int dfsConnectAftNormalCount=0;
			for(int index=0, len=initNormalTypeGroupArray.size();index<len;++index) {
				int airplaneTypeIndex=index;
				ArrayList<Integer> initNormalTypeGroupList=initNormalTypeGroupArray.get(airplaneTypeIndex);
				for(int indexi=0,leni=initNormalTypeGroupList.size();indexi<leni;++indexi) {
					int normalAirlineIndex=initNormalTypeGroupList.get(indexi);
					iteratorHeadAirlineStatus[normalAirlineIndex]=HeadTailStatus.STATUS_FIXED;
					iteratorResultScoreArray[normalAirlineIndex]=0;
					iteratorResultEarnScoreArray[normalAirlineIndex]=0;
					iteratorResultCostScoreArray[normalAirlineIndex]=0;
					FlightFrag normalHeadFrag=iteratorHeadFragArray.get(normalAirlineIndex);
					ArrayList<FlightFrag> normalBodyFragList=iteratorBodyFragArray.get(normalAirlineIndex);
					FlightFrag normalTailFrag=iteratorTailFragArray.get(normalAirlineIndex);
					ArrayList<DeputyFrag> normalResultDfragList=iteratorResultDfragArray.get(normalAirlineIndex);
					//normal head fragment
					DeputyFrag headDeputyFrag=normalResultDfragList.get(iteratorResultDfragNumberArray[normalAirlineIndex]);
					++iteratorResultDfragNumberArray[normalAirlineIndex];
					headDeputyFrag.resetHeadFrag(normalHeadFrag);
					//normal body fragments
					for(int indexj=0,lenj=normalBodyFragList.size();indexj<lenj;++indexj) {
						FlightFrag normalBodyFrag=normalBodyFragList.get(indexj);
						DeputyFrag bodyDeputyFrag=normalResultDfragList.get(iteratorResultDfragNumberArray[normalAirlineIndex]);
						++iteratorResultDfragNumberArray[normalAirlineIndex];
						if(normalBodyFrag.isBodySingleFrag()) {
							++dfsSingleNormalCount;
							bodyDeputyFrag.resetBodySingleFrag(normalBodyFrag, normalBodyFrag.getFirstDynamicFlight());
						}else if(normalBodyFrag.isBodyConnectFrag()) {
							++dfsConnectNormalCount;
							++dfsConnectPreNormalCount;
							++dfsConnectAftNormalCount;
							bodyDeputyFrag.resetBodyConnectFrag(normalBodyFrag, normalBodyFrag.getFirstDynamicFlight(), 
									normalBodyFrag.getLastDynamicFlight(), DeputyFrag.ConnectOperation.OP_ALLPART);
						}
					}
					//normal tail fragment
					DeputyFrag	tailDeputyFrag=normalResultDfragList.get(iteratorResultDfragNumberArray[normalAirlineIndex]);
					++iteratorResultDfragNumberArray[normalAirlineIndex];
					tailDeputyFrag.resetTailFrag(normalTailFrag, normalTailFrag.getFirstDynamicFlight(), normalTailFrag.getLastDynamicFlight(), 0.0);
					normalResultDfragList.add(headDeputyFrag);
				}
			}
			System.out.println("dfsSingleNormalCount: "+dfsSingleNormalCount);
			System.out.println("dfsConnectNormalCount: "+dfsConnectNormalCount);
			System.out.println("dfsConnectPreNormalCount: "+dfsConnectPreNormalCount);
			System.out.println("dfsConnectAftNormalCount: "+dfsConnectAftNormalCount+"\n");
			
			//calculate total dfs body fragment number
			int dfsSingleTotalCount=dfsSingleBrokenCount+dfsSingleNormalCount;
			int dfsConnectTotalCount=dfsConnectBrokenCount+dfsConnectNormalCount;
			int dfsConnectPreTotalCount=dfsConnectPreBrokenCount+dfsConnectPreNormalCount;
			int dfsConnectAftTotalCount=dfsConnectAftBrokenCount+dfsConnectAftNormalCount;
			System.out.println("dfsSingleTotalCount: "+dfsSingleTotalCount);
			System.out.println("dfsConnectTotalCount: "+dfsConnectTotalCount);
			System.out.println("dfsConnectPreTotalCount: "+dfsConnectPreTotalCount);
			System.out.println("dfsConnectAftTotalCount: "+dfsConnectAftTotalCount+"\n");
			
			//deal with initial broken airline, with dfs gentle mode
			iteratorDfsMode=IteratorDfsMode.DFSMODE_GENTLE;
			int gentleUsedBodyCount=0;
			int gentleUsedBodySingleCount=0;
			int gentleUsedBodyConnectCount=0;
			double gentleTotalEarnScore=0;
			double gentleTotalCostScore=0;
			for(int index=initBrokenTypeGroupArray.size()-1;index>=0;--index) {
				int airplaneTypeIndex=index;
				ArrayList<Integer> initBrokenTypeGroupList=initBrokenTypeGroupArray.get(airplaneTypeIndex);
				iteratorAirplaneTypeIndex=airplaneTypeIndex;
				for(int indexi=0, leni=initBrokenTypeGroupList.size();indexi<leni;++indexi) {
					//solve head with exclusive tail here
					int headAirlineIndex=initBrokenTypeGroupList.get(indexi);
					int tailAirlineIndex=iteratorMatchArray[headAirlineIndex];
//					boolean printFlag=false;
//					if(InfoInterpreter.airplaneIdArray.get(headAirlineIndex).equals("118")) {
//						printFlag=true;
//					}
					//try dfs only first
					iteratorAirplaneIdIndex=headAirlineIndex;
					iteratorDfsHeadFrag=iteratorHeadFragArray.get(headAirlineIndex);
					iteratorDfsTailFrag=iteratorTailFragArray.get(tailAirlineIndex);
					iteratorDfsTailSPortIndex=iteratorDfsTailFrag.getFirstDynamicFlight().getStartAirportIndex();
					//change tail plane and type first
					DynamicFlight dynamicFlight=iteratorDfsTailFrag.getFirstDynamicFlight();
					double tailTotalScore=0;
					while(dynamicFlight!=null) {
						dynamicFlight.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
						tailTotalScore+=dynamicFlight.getScore();
						dynamicFlight=dynamicFlight.getNextDynamicFlight();
					}
					iteratorDfsTailFrag.setTotalScore(tailTotalScore);
					//try find most gentle result for this airline
					airlineMaxCostScore=0;
					for(int indexj=0;indexj<5;++indexj) {
						iteratorDfsRunDfragNumber=0;
						iteratorDfsRecResultNumber=0;
						iteratorDfsRecDfragNumber=0;
						iteratorDfsRecScore=java.lang.Double.MAX_VALUE;
						airlineMaxCostScore+=1000;
						iteratorDfsFixAirline(iteratorDfsHeadFrag, 0, 0);
						if(iteratorDfsRecDfragNumber>0) {
							break;
						}
					}
					//test if there is a best result
					if(iteratorDfsRecDfragNumber>0) {
						//dfs only success
						iteratorHeadAirlineStatus[iteratorAirplaneIdIndex]=HeadTailStatus.STATUS_FIXED;
//							System.out.println("airline success: "+InfoInterpreter.airplaneIdArray.get(iteratorAirplaneIdIndex)
//												+", earn score: "+iteratorDfsRecEarnScore+", cost score: "+iteratorDfsRecCostScore);
						//record score
						gentleTotalEarnScore+=iteratorDfsRecEarnScore;
						gentleTotalCostScore+=iteratorDfsRecCostScore;
						iteratorResultScoreArray[iteratorAirplaneIdIndex]=iteratorDfsRecScore;
						iteratorResultEarnScoreArray[iteratorAirplaneIdIndex]=iteratorDfsRecEarnScore;
						iteratorResultCostScoreArray[iteratorAirplaneIdIndex]=iteratorDfsRecCostScore;
						gentleUsedBodyCount+=iteratorDfsRecDfragNumber-2;
						//record result and remove used body fragments
						ArrayList<DeputyFrag> iteratorResultDfragList=iteratorResultDfragArray.get(iteratorAirplaneIdIndex);
						DeputyFrag preDeputyFrag=null;
						DeputyFrag deputyFrag=null;
						int iteratorDfsRecDfragLIndex=iteratorDfsRecDfragNumber-1;
						for(int indexj=0;indexj<iteratorDfsRecDfragNumber;++indexj) {
							//record airline result
							deputyFrag=iteratorResultDfragList.get(iteratorResultDfragNumberArray[iteratorAirplaneIdIndex]);
							++iteratorResultDfragNumberArray[iteratorAirplaneIdIndex];
							deputyFrag.copyFrom(iteratorDfsRecDfragArray.get(indexj));
							//remove used body flight fragments
							if(indexj>0&&indexj<iteratorDfsRecDfragLIndex) {		
								FlightFrag flightFrag=deputyFrag.getFlightFrag();
								int startAirportIndex=flightFrag.getFirstDynamicFlight().getStartAirportIndex();
								ArrayList<FlightFrag> iteratorBodyFragList=iteratorDfsBodyFragArray.get(startAirportIndex);	
								iteratorBodyFragList.remove(flightFrag);
								if(flightFrag.isBodyConnectFrag()) {
									++gentleUsedBodyConnectCount;
									int midAirportIndex=flightFrag.getFirstDynamicFlight().getEndAirportIndex();
									iteratorBodyFragList=iteratorDfsBodyFragArray.get(midAirportIndex);
									iteratorBodyFragList.remove(flightFrag);
								}else if(flightFrag.isBodySingleFrag()){
									++gentleUsedBodySingleCount;
								}
							}
						}
						//consume stop scene and capacity
						long preEndDateTime=iteratorDfsRecDfragArray.get(0).getEndDateTime();
						for(int indexj=1;indexj<iteratorDfsRecDfragNumber;++indexj) {
							deputyFrag=iteratorDfsRecDfragArray.get(indexj);
							int startAirportIndex=deputyFrag.getStartAirportIndex();
							int endAirportIndex=deputyFrag.getEndAirportIndex();
							long startDateTime=deputyFrag.getStartDateTime();
							long endDateTime=deputyFrag.getEndDateTime();
							Scene stopScene=stopSceneArray.get(startAirportIndex);
							if(stopScene!=null) {
								stopScene.consumeRemainStopNum(startAirportIndex, preEndDateTime, startDateTime);
							}
							preEndDateTime=endDateTime;
							CapacityLimitation startCapacityLimitation=capacityLimitationArray.get(startAirportIndex);
							if(startCapacityLimitation!=null) {
								startCapacityLimitation.consumeCapacityLimitation(startDateTime);
							}
							CapacityLimitation endCapacityLimitation=capacityLimitationArray.get(endAirportIndex);
							if(endCapacityLimitation!=null) {
								endCapacityLimitation.consumeCapacityLimitation(endDateTime);
							}
							//deal with body connect circumstance
							FlightFrag flightFrag=deputyFrag.getFlightFrag();
							if(flightFrag.isBodyConnectFrag()) {
								if(deputyFrag.getConnectOperation()==DeputyFrag.ConnectOperation.OP_ALLPART) {
									int midAirportIndex=flightFrag.getFirstDynamicFlight().getEndAirportIndex();
									long midEndDateTime=startDateTime+flightFrag.getFirstDynamicFlight().getTravelTime();
									long midStartDateTime=endDateTime-flightFrag.getLastDynamicFlight().getTravelTime();
									stopScene=stopSceneArray.get(midAirportIndex);
									if(stopScene!=null) {
										stopScene.consumeRemainStopNum(midAirportIndex, midEndDateTime, midStartDateTime);
									}
									CapacityLimitation midCapacityLimitation=capacityLimitationArray.get(midAirportIndex);
									if(midCapacityLimitation!=null) {
										midCapacityLimitation.consumeCapacityLimitation(midStartDateTime);
										midCapacityLimitation.consumeCapacityLimitation(midEndDateTime);
									}
								}
							}
						}
					}else {
						//dfs gentle failure
						iteratorHeadAirlineStatus[iteratorAirplaneIdIndex]=HeadTailStatus.STATUS_WAIT;
						System.out.println("airline failure: "+InfoInterpreter.airplaneIdArray.get(iteratorAirplaneIdIndex));
					}
				}
			}
			//count normal airline into score
			for(int index=0, len=initNormalTypeGroupArray.size();index<len;++index) {
				int airplaneTypeIndex=index;
				ArrayList<Integer> initNormalTypeGroupList=initNormalTypeGroupArray.get(airplaneTypeIndex);
				for(int indexi=0,leni=initNormalTypeGroupList.size();indexi<leni;++indexi) {
					int normalAirlineIndex=initNormalTypeGroupList.get(indexi);
					ArrayList<DeputyFrag> normalResultDfragList=iteratorResultDfragArray.get(normalAirlineIndex);
					for(int indexj=1, lenj=normalResultDfragList.size();indexj<lenj;++indexj) {
						gentleTotalEarnScore+=normalResultDfragList.get(indexj).getEarnScore();
						gentleTotalCostScore+=normalResultDfragList.get(indexj).getCostScore();
					}
				}
			}
			System.out.println("gentleTotalEarnScore: "+gentleTotalEarnScore);
			System.out.println("gentleTotalCostScore: "+gentleTotalCostScore);
			System.out.println("gentleUsedBodyCount: "+gentleUsedBodyCount);
			System.out.println("gentleUsedBodySingleCount: "+gentleUsedBodySingleCount);
			System.out.println("gentleUsedBodyConnectCount: "+gentleUsedBodyConnectCount+"\n");
			
			//deal with all airline, with dfs greedy mode
			iteratorDfsMode=IteratorDfsMode.DFSMODE_GREEDY;
			for(double iteratorAirlineMaxCostScore=1500;iteratorAirlineMaxCostScore<6000;iteratorAirlineMaxCostScore+=200) {
				airlineMaxCostScore=iteratorAirlineMaxCostScore;
				int greedyUsedBodyCount=0;
				int greedyUsedBodySingleCount=0;
				int greedyUsedBodyConnectCount=0;
				double greedyTotalEarnScore=0;
				double greedyTotalCostScore=0;
				for(int index=airplaneTypeGroupArray.size()-1;index>=0;--index) {
					int airplaneTypeIndex=index;
					ArrayList<Integer> airplaneTypeGroupList=airplaneTypeGroupArray.get(airplaneTypeIndex);
					iteratorAirplaneTypeIndex=airplaneTypeIndex;
					for(int indexi=0, leni=airplaneTypeGroupList.size();indexi<leni;++indexi) {
						//iterator head with exclusive tail here
						int headAirlineIndex=airplaneTypeGroupList.get(indexi);
//						boolean printFlag=false;
//						if(InfoInterpreter.airplaneIdArray.get(headAirlineIndex).equals("118")) {
//							printFlag=true;
//						}	
						if(iteratorResultCostScoreArray[headAirlineIndex]<iteratorAirlineMaxCostScore) {
							ArrayList<DeputyFrag> iteratorResultDfragList=iteratorResultDfragArray.get(headAirlineIndex);
							int iteratorResultDfragListLength=iteratorResultDfragNumberArray[headAirlineIndex];
							long preEndDateTime=iteratorResultDfragList.get(0).getEndDateTime();
							//pay back stop scene, capacity and body fragments
							for(int indexj=1;indexj<iteratorResultDfragListLength;++indexj) {
								DeputyFrag bodyDeputyFrag=iteratorResultDfragList.get(indexj);
								int startAirportIndex=bodyDeputyFrag.getStartAirportIndex();
								int endAirportIndex=bodyDeputyFrag.getEndAirportIndex();
								long startDateTime=bodyDeputyFrag.getStartDateTime();
								long endDateTime=bodyDeputyFrag.getEndDateTime();
								//pay back stop scene, capacity
								Scene stopScene=stopSceneArray.get(startAirportIndex);
								if(stopScene!=null) {
									stopScene.paybackRemainStopNum(startAirportIndex, preEndDateTime, startDateTime);
								}
								preEndDateTime=endDateTime;
								CapacityLimitation startCapacityLimitation=capacityLimitationArray.get(startAirportIndex);
								if(startCapacityLimitation!=null) {
									startCapacityLimitation.paybackCapacityLimitation(startDateTime);
								}
								CapacityLimitation endCapacityLimitation=capacityLimitationArray.get(endAirportIndex);
								if(endCapacityLimitation!=null) {
									endCapacityLimitation.paybackCapacityLimitation(endDateTime);
								}
								//pay back stop scene, capacity for connect allpart operation
								FlightFrag bodyFlightFrag=bodyDeputyFrag.getFlightFrag();
								if(bodyFlightFrag.isBodyConnectFrag()) {
									if(bodyDeputyFrag.getConnectOperation()==DeputyFrag.ConnectOperation.OP_ALLPART) {
										int midAirportIndex=bodyFlightFrag.getFirstDynamicFlight().getEndAirportIndex();
										long midEndDateTime=startDateTime+bodyFlightFrag.getFirstDynamicFlight().getTravelTime();
										long midStartDateTime=endDateTime-bodyFlightFrag.getLastDynamicFlight().getTravelTime();
										stopScene=stopSceneArray.get(midAirportIndex);
										if(stopScene!=null) {
											stopScene.paybackRemainStopNum(midAirportIndex, midEndDateTime, midStartDateTime);
										}
										CapacityLimitation midCapacityLimitation=capacityLimitationArray.get(midAirportIndex);
										if(midCapacityLimitation!=null) {
											midCapacityLimitation.paybackCapacityLimitation(midStartDateTime);
											midCapacityLimitation.paybackCapacityLimitation(midEndDateTime);
										}
									}
								}
								//put back body fragments
								DynamicFlight firstDynamicFlight=bodyFlightFrag.getFirstDynamicFlight();
								DynamicFlight lastDynamicFlight=bodyFlightFrag.getLastDynamicFlight();
								if(bodyFlightFrag.isBodySingleFrag()) {
									int bodyStartAirportIndex=firstDynamicFlight.getStartAirportIndex();
									ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(bodyStartAirportIndex);
									iteratorDfsBodyFragList.add(bodyFlightFrag);
								}else if(bodyFlightFrag.isBodyConnectFrag()) {
									if(firstDynamicFlight.getCanAhead()||firstDynamicFlight.getCanDelay()) {
										int preStartAirportIndex=firstDynamicFlight.getStartAirportIndex();
										ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(preStartAirportIndex);
										iteratorDfsBodyFragList.add(bodyFlightFrag);
									}
									if(lastDynamicFlight.getCanAhead()||lastDynamicFlight.getCanDelay()) {
										int aftStartAirportIndex=lastDynamicFlight.getStartAirportIndex();
										ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(aftStartAirportIndex);
										iteratorDfsBodyFragList.add(bodyFlightFrag);
									}
								}
							}
							//reset airline data
							iteratorResultDfragNumberArray[headAirlineIndex]=0;
				 			iteratorResultScoreArray[headAirlineIndex]=0;
				 			iteratorResultEarnScoreArray[headAirlineIndex]=0;
				 			iteratorResultCostScoreArray[headAirlineIndex]=java.lang.Double.MAX_VALUE;
				 			int tailAirlineIndex=iteratorMatchArray[headAirlineIndex];
				 			//reset iterator data
							iteratorAirplaneIdIndex=headAirlineIndex;
							iteratorDfsHeadFrag=iteratorHeadFragArray.get(headAirlineIndex);
							iteratorDfsTailFrag=iteratorTailFragArray.get(tailAirlineIndex);
							iteratorDfsTailSPortIndex=iteratorDfsTailFrag.getFirstDynamicFlight().getStartAirportIndex();
							//change tail plane and type first
							DynamicFlight dynamicFlight=iteratorDfsTailFrag.getFirstDynamicFlight();
							double tailTotalScore=0;
							while(dynamicFlight!=null) {
								dynamicFlight.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
								tailTotalScore+=dynamicFlight.getScore();
								dynamicFlight=dynamicFlight.getNextDynamicFlight();
							}
							iteratorDfsTailFrag.setTotalScore(tailTotalScore);
							//try dfs greedy
							iteratorDfsRunDfragNumber=0;
							iteratorDfsRecResultNumber=0;
							iteratorDfsRecDfragNumber=0;
							iteratorDfsRecScore=0;
							iteratorDfsFixAirline(iteratorDfsHeadFrag, 0, 0);
							//test if there is a best result
							if(iteratorDfsRecDfragNumber>0) {
								//dfs greedy success
								iteratorHeadAirlineStatus[iteratorAirplaneIdIndex]=HeadTailStatus.STATUS_FIXED;
//									System.out.println("airline success: "+InfoInterpreter.airplaneIdArray.get(iteratorAirplaneIdIndex)
//														+", earn score: "+iteratorDfsRecEarnScore+", cost score: "+iteratorDfsRecCostScore);
								//record score
								greedyTotalEarnScore+=iteratorDfsRecEarnScore;
								greedyTotalCostScore+=iteratorDfsRecCostScore;
								iteratorResultScoreArray[iteratorAirplaneIdIndex]=iteratorDfsRecScore;
								iteratorResultEarnScoreArray[iteratorAirplaneIdIndex]=iteratorDfsRecEarnScore;
								iteratorResultCostScoreArray[iteratorAirplaneIdIndex]=iteratorDfsRecCostScore;
								greedyUsedBodyCount+=iteratorDfsRecDfragNumber-2;
								//record result, remove used fragments
								DeputyFrag preDeputyFrag=null;
								DeputyFrag deputyFrag=null;
								int iteratorDfsRecDfragLIndex=iteratorDfsRecDfragNumber-1;
								for(int indexj=0;indexj<iteratorDfsRecDfragNumber;++indexj) {
									//record airline result
									deputyFrag=iteratorResultDfragList.get(iteratorResultDfragNumberArray[iteratorAirplaneIdIndex]);
									++iteratorResultDfragNumberArray[iteratorAirplaneIdIndex];
									deputyFrag.copyFrom(iteratorDfsRecDfragArray.get(indexj));
									//remove used body flight fragments
									if(indexj>0&&indexj<iteratorDfsRecDfragLIndex) {		
										FlightFrag flightFrag=deputyFrag.getFlightFrag();
										int startAirportIndex=flightFrag.getFirstDynamicFlight().getStartAirportIndex();
										ArrayList<FlightFrag> iteratorBodyFragList=iteratorDfsBodyFragArray.get(startAirportIndex);	
										iteratorBodyFragList.remove(flightFrag);
										if(flightFrag.isBodyConnectFrag()) {
											int midAirportIndex=flightFrag.getFirstDynamicFlight().getEndAirportIndex();
											iteratorBodyFragList=iteratorDfsBodyFragArray.get(midAirportIndex);
											iteratorBodyFragList.remove(flightFrag);
											++greedyUsedBodyConnectCount;
										}else if(flightFrag.isBodySingleFrag()) {
											++greedyUsedBodySingleCount;
										}
									}
								}
								//consume stop scene and capacity again
								preEndDateTime=iteratorDfsRecDfragArray.get(0).getEndDateTime();
								for(int indexj=1;indexj<iteratorDfsRecDfragNumber;++indexj) {
									deputyFrag=iteratorDfsRecDfragArray.get(indexj);
									int startAirportIndex=deputyFrag.getStartAirportIndex();
									int endAirportIndex=deputyFrag.getEndAirportIndex();
									long startDateTime=deputyFrag.getStartDateTime();
									long endDateTime=deputyFrag.getEndDateTime();
									Scene stopScene=stopSceneArray.get(startAirportIndex);
									if(stopScene!=null) {
										stopScene.consumeRemainStopNum(startAirportIndex, preEndDateTime, startDateTime);
									}
									preEndDateTime=endDateTime;
									CapacityLimitation startCapacityLimitation=capacityLimitationArray.get(startAirportIndex);
									if(startCapacityLimitation!=null) {
										startCapacityLimitation.consumeCapacityLimitation(startDateTime);
									}
									CapacityLimitation endCapacityLimitation=capacityLimitationArray.get(endAirportIndex);
									if(endCapacityLimitation!=null) {
										endCapacityLimitation.consumeCapacityLimitation(endDateTime);
									}
									//deal with body connect circumstance
									FlightFrag flightFrag=deputyFrag.getFlightFrag();
									if(flightFrag.isBodyConnectFrag()) {
										if(deputyFrag.getConnectOperation()==DeputyFrag.ConnectOperation.OP_ALLPART) {
											int midAirportIndex=flightFrag.getFirstDynamicFlight().getEndAirportIndex();
											long midEndDateTime=startDateTime+flightFrag.getFirstDynamicFlight().getTravelTime();
											long midStartDateTime=endDateTime-flightFrag.getLastDynamicFlight().getTravelTime();
											stopScene=stopSceneArray.get(midAirportIndex);
											if(stopScene!=null) {
												stopScene.consumeRemainStopNum(midAirportIndex, midEndDateTime, midStartDateTime);
											}
											CapacityLimitation midCapacityLimitation=capacityLimitationArray.get(midAirportIndex);
											if(midCapacityLimitation!=null) {
												midCapacityLimitation.consumeCapacityLimitation(midStartDateTime);
												midCapacityLimitation.consumeCapacityLimitation(midEndDateTime);
											}
										}
									}
								}
							}else {
								//dfs greedy failure
								iteratorHeadAirlineStatus[iteratorAirplaneIdIndex]=HeadTailStatus.STATUS_WAIT;
								System.out.println("airline failure: "+InfoInterpreter.airplaneIdArray.get(iteratorAirplaneIdIndex));
							}
						}
					}
				}
				System.out.println("greedy iterating, max cost: "+iteratorAirlineMaxCostScore);
				System.out.println("greedyTotalEarnScore: "+greedyTotalEarnScore);
				System.out.println("greedyTotalCostScore: "+greedyTotalCostScore);
				System.out.println("greedyUsedBodyCount: "+greedyUsedBodyCount);
				System.out.println("greedyUsedBodySingleCount: "+greedyUsedBodySingleCount);
				System.out.println("greedyUsedBodyConnectCount: "+greedyUsedBodyConnectCount+"\n");
			}
			
			//calculate remain body fragment count
			int remainBodyFragCount=0;
			int remainBodyConnectCount=0;
			int remainBodySingleCount=0;
			int remainBodyPreCount=0;
			int remainBodyAftCount=0;
			Set<Integer> remainConnnectIdSet=new HashSet<>();
			for(int index=0, len=iteratorDfsBodyFragArray.size();index<len;++index) {
				int airportIndex=index;
				ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(airportIndex);
				for(int indexi=0, leni=iteratorDfsBodyFragList.size();indexi<leni;++indexi) {
					if(iteratorDfsBodyFragList.get(indexi).isBodyConnectFrag()){
						remainConnnectIdSet.add(iteratorDfsBodyFragList.get(indexi).getFirstDynamicFlight().getFlightIdIndex());
						int startAirportIndex=iteratorDfsBodyFragList.get(indexi).getFirstDynamicFlight().getStartAirportIndex();
						if(startAirportIndex==airportIndex) {
							++remainBodyPreCount;
						}else {
							++remainBodyAftCount;
						}
					}else {
						++remainBodySingleCount;
					}
				}
			}
			remainBodyConnectCount=remainConnnectIdSet.size();
			remainBodyFragCount=remainBodyConnectCount+remainBodySingleCount;
			
			System.out.println("remainBodyFragCount: "+remainBodyFragCount);
			System.out.println("remainBodySingleCount: "+remainBodySingleCount);
			System.out.println("remainBodyConnectCount: "+remainBodyConnectCount);
			System.out.println("remainBodyPreCount: "+remainBodyPreCount);
			System.out.println("remainBodyAftCount: "+remainBodyAftCount+"\n");
		}
		catch (Exception e){
			e.printStackTrace();
		}	
		
		System.out.println("iteratorAirlines done\n\n");
	}
	
	private void iteratorDfsFixAirline(FlightFrag flightFrag, double earnScore, double costScore) {
//		boolean printFlag=false;
//		if(InfoInterpreter.airplaneIdArray.get(iteratorAirplaneIdIndex).equals("118")) {
//			printFlag=true;
//		}
		DeputyFrag iteratorDfsDfrag=iteratorDfsRunDfragArray.get(iteratorDfsRunDfragNumber);
		int startAirportIndex=-1;
		int endAirportIndex=-1;
		
		if(iteratorDfsRunDfragNumber==0) {
		//head
			iteratorDfsDfrag.resetHeadFrag(flightFrag);
			endAirportIndex=iteratorDfsDfrag.getEndAirportIndex();
			double iteratorEarnScore=earnScore;
			double iteratorCostScore=costScore;
			//boundary check for cost score
			if(iteratorCostScore<airlineMaxCostScore) {
				//continue to dfs
				flightFrag.setIsSearched(true);
				++iteratorDfsRunDfragNumber;
				//try tail first
				if(endAirportIndex==iteratorDfsTailSPortIndex) {
					iteratorDfsFixAirline(iteratorDfsTailFrag, iteratorEarnScore, iteratorCostScore);
				}
				//try body then
				ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(endAirportIndex);
				for(int index=0, len=iteratorDfsBodyFragList.size();index<len;++index) {
					FlightFrag nextBodyFlightFrag=iteratorDfsBodyFragList.get(index);
					if(!nextBodyFlightFrag.isSearched()) {
						iteratorDfsFixAirline(nextBodyFlightFrag, iteratorEarnScore, iteratorCostScore);
					}
				}
				--iteratorDfsRunDfragNumber;
				flightFrag.setIsSearched(false);
			}
		}else {
		//body or tail
			
			//get last ending Information
			int lastDfsRunDfragIndex=iteratorDfsRunDfragNumber-1;
			DeputyFrag lastDeputyFrag=iteratorDfsRunDfragArray.get(lastDfsRunDfragIndex);
			int lastFlightIdIndex=lastDeputyFrag.getLastFlightIdIndex();
			int lastEndAirportIndex=lastDeputyFrag.getEndAirportIndex();
			long lastEndDateTime=lastDeputyFrag.getEndDateTime();
			
			//body single
			if(flightFrag.isBodySingleFrag()) {
				DynamicFlight dynamicFlight=flightFrag.getFirstDynamicFlight();
				startAirportIndex=dynamicFlight.getStartAirportIndex();
				endAirportIndex=dynamicFlight.getEndAirportIndex();
				//test limitation first
				if(!queryIsLimitation(iteratorAirplaneIdIndex, startAirportIndex, endAirportIndex))
				{
					//then change plane
					globalDeputyDynamicFlight1.copyFrom(dynamicFlight);
					globalDeputyDynamicFlight1.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
					int flightIdIndex=globalDeputyDynamicFlight1.getFlightIdIndex();
					long earliestStartDateTime=lastEndDateTime+getFlightIntervalTime(lastFlightIdIndex, flightIdIndex);
					boolean aheadResult=false;
					boolean delayResult=false;
					//try ahead
					if(dynamicFlight.getCanAhead()) {
						aheadResult=fitAheadDynamicFlight(globalDeputyDynamicFlight1,earliestStartDateTime);
					}
					//try delay
					if(!aheadResult&&globalDeputyDynamicFlight1.getCanDelay())  {
						globalDeputyDynamicFlight1.resetStartDateTime();
						long originStartDateTime=globalDeputyDynamicFlight1.getOriginStartDateTime();
						if(originStartDateTime>=earliestStartDateTime) {
							delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
						}else {
							delayResult=globalDeputyDynamicFlight1.delayFlight(earliestStartDateTime-originStartDateTime);
							if(delayResult) {
								delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
							}
						}
					}
					//adjust time success
					if(aheadResult||delayResult) {
						boolean canStopResult=true;
						long startDateTime=globalDeputyDynamicFlight1.getStartDateTime();
						Scene stopScene=stopSceneArray.get(lastEndAirportIndex);
						if(stopScene!=null&&!stopScene.canStopInScene(lastEndAirportIndex, lastEndDateTime, startDateTime)) {
							canStopResult=false;
						}
						//test if can stop
						if(canStopResult) {
							iteratorDfsDfrag.resetBodySingleFrag(flightFrag, globalDeputyDynamicFlight1);
							double iteratorEarnScore=earnScore+iteratorDfsDfrag.getEarnScore();
							double iteratorCostScore=costScore+iteratorDfsDfrag.getCostScore();
							//boundary check for cost score
							if(iteratorCostScore<airlineMaxCostScore) {
								//continue to dfs
								flightFrag.setIsSearched(true);
								++iteratorDfsRunDfragNumber;
								//try tail first
								if(endAirportIndex==iteratorDfsTailSPortIndex) {
									iteratorDfsFixAirline(iteratorDfsTailFrag, iteratorEarnScore, iteratorCostScore);
								}
								//try body then
								ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(endAirportIndex);
								for(int index=0, len=iteratorDfsBodyFragList.size();index<len;++index) {
									FlightFrag nextBodyFlightFrag=iteratorDfsBodyFragList.get(index);
									if(!nextBodyFlightFrag.isSearched()) {
										iteratorDfsFixAirline(nextBodyFlightFrag, iteratorEarnScore, iteratorCostScore);
									}
								}
								--iteratorDfsRunDfragNumber;
								flightFrag.setIsSearched(false);
							}
						}
					}
				}
			}else if(flightFrag.isBodyConnectFrag()) {
			//body connect
				//get pre and aft flights information
				DynamicFlight firstDynamicFlight=flightFrag.getFirstDynamicFlight();
				DynamicFlight lastDynamicFlight=flightFrag.getLastDynamicFlight();
				startAirportIndex=firstDynamicFlight.getStartAirportIndex();
				int midAirportIndex=firstDynamicFlight.getEndAirportIndex();
				endAirportIndex=lastDynamicFlight.getEndAirportIndex();
				//start with body connect prepart
				if(lastEndAirportIndex==firstDynamicFlight.getStartAirportIndex()) {
					//test pre flight limitation first
					if(!queryIsLimitation(iteratorAirplaneIdIndex, startAirportIndex, midAirportIndex))
					{
					//try prepart
						//change pre flight's plane
						globalDeputyDynamicFlight1.copyFrom(firstDynamicFlight);
						globalDeputyDynamicFlight1.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
						int preFlightIdIndex=globalDeputyDynamicFlight1.getFlightIdIndex();
						long preEarliestStartDateTime=lastEndDateTime+getFlightIntervalTime(lastFlightIdIndex, preFlightIdIndex);
						boolean preAheadResult=false;
						boolean preDelayResult=false;
						//try pre ahead
						if(globalDeputyDynamicFlight1.getCanAhead()) {
							preAheadResult=fitAheadDynamicFlight(globalDeputyDynamicFlight1,preEarliestStartDateTime);
						}
						//try pre delay
						if(!preAheadResult&&globalDeputyDynamicFlight1.getCanDelay())  {
							globalDeputyDynamicFlight1.resetStartDateTime();
							long preOriginStartDateTime=globalDeputyDynamicFlight1.getOriginStartDateTime();
							if(preOriginStartDateTime>=preEarliestStartDateTime) {
								preDelayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
							}else {
								preDelayResult=globalDeputyDynamicFlight1.delayFlight(preEarliestStartDateTime-preOriginStartDateTime);
								if(preDelayResult) {
									preDelayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
								}
							}
						}
						//adjust pre time success
						if(preAheadResult||preDelayResult) {
							boolean preCanStopResult=true;
							long preStartDateTime=globalDeputyDynamicFlight1.getStartDateTime();
							Scene preStopScene=stopSceneArray.get(lastEndAirportIndex);
							if(preStopScene!=null&&!preStopScene.canStopInScene(lastEndAirportIndex, lastEndDateTime, preStartDateTime)) {
								preCanStopResult=false;
							}
							//test if can pre flight stop
							if(preCanStopResult) {
								globalDeputyDynamicFlight2.copyFrom(lastDynamicFlight);
								globalDeputyDynamicFlight2.cancelFlight();
								iteratorDfsDfrag.resetBodyConnectFrag(flightFrag, globalDeputyDynamicFlight1, globalDeputyDynamicFlight2, DeputyFrag.ConnectOperation.OP_PREPART);
								double preIteratorEarnScore=earnScore+iteratorDfsDfrag.getEarnScore();
								double preIteratorCostScore=costScore+iteratorDfsDfrag.getCostScore();
								//boundary check for cost score
								if(preIteratorCostScore<airlineMaxCostScore) {
									//continue to dfs with prepart operation
									flightFrag.setIsSearched(true);
									++iteratorDfsRunDfragNumber;
									//try tail first
									if(midAirportIndex==iteratorDfsTailSPortIndex) {
										iteratorDfsFixAirline(iteratorDfsTailFrag, preIteratorEarnScore, preIteratorCostScore);
									}
									//try body then
									ArrayList<FlightFrag> preIteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(midAirportIndex);
									for(int index=0, len=preIteratorDfsBodyFragList.size();index<len;++index) {
										FlightFrag nextBodyFlightFrag=preIteratorDfsBodyFragList.get(index);
										if(!nextBodyFlightFrag.isSearched()) {
											iteratorDfsFixAirline(nextBodyFlightFrag, preIteratorEarnScore, preIteratorCostScore);
										}
									}
									--iteratorDfsRunDfragNumber;
									flightFrag.setIsSearched(false);
								}
								
								//after prepart operation success, continue to try all part
								double allpartPreCostScore=preIteratorCostScore-Configuration.getCancelFlightParam()*lastDynamicFlight.getImportRatio()*0.4;
								//boundary check for cost score
								if(allpartPreCostScore<airlineMaxCostScore) {
									if(!queryIsLimitation(iteratorAirplaneIdIndex, midAirportIndex, endAirportIndex)) {
										//reset prepart flight
										globalDeputyDynamicFlight1.copyFrom(firstDynamicFlight);
										globalDeputyDynamicFlight1.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
										long preOriginStartDateTime=globalDeputyDynamicFlight1.getOriginStartDateTime();
										if(preStartDateTime>preOriginStartDateTime) {
											globalDeputyDynamicFlight1.delayFlight(preStartDateTime-preOriginStartDateTime) ;
										}else if(preStartDateTime<preOriginStartDateTime){
											globalDeputyDynamicFlight1.aheadFlight(preOriginStartDateTime-preStartDateTime);
										}
										//then adjust aft flight
										globalDeputyDynamicFlight2.copyFrom(lastDynamicFlight);
										globalDeputyDynamicFlight2.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
										int aftFlightIdIndex=globalDeputyDynamicFlight2.getFlightIdIndex();
										long aftEarliestStartDateTime=globalDeputyDynamicFlight1.getEndDateTime()+getFlightIntervalTime(preFlightIdIndex, aftFlightIdIndex);
										boolean aftAheadResult=false;
										boolean aftDelayResult=false;
										//try aft ahead
										if(globalDeputyDynamicFlight2.getCanAhead()) {
											aftAheadResult=fitAheadDynamicFlight(globalDeputyDynamicFlight2, aftEarliestStartDateTime);
										}
										//try aft delay
										if(!aftAheadResult&&globalDeputyDynamicFlight2.getCanDelay()) {
											globalDeputyDynamicFlight2.resetStartDateTime();
											long aftOriginStartDateTime=globalDeputyDynamicFlight2.getOriginStartDateTime();
											if(aftOriginStartDateTime>=aftEarliestStartDateTime) {
												aftDelayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight2);
											}else {
												aftDelayResult=globalDeputyDynamicFlight2.delayFlight(aftEarliestStartDateTime-aftOriginStartDateTime);
												if(aftDelayResult) {
													aftDelayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight2);
												}
											}
										}
										//adjust aft time success
										if(aftAheadResult||aftDelayResult) {
											boolean aftCanStopResult=true;
											long aftStartDateTime=globalDeputyDynamicFlight2.getStartDateTime();
											long preEndDateTime=globalDeputyDynamicFlight1.getEndDateTime();
											int preEndAirportIndex=globalDeputyDynamicFlight1.getEndAirportIndex();
											Scene aftStopScene=stopSceneArray.get(preEndAirportIndex);
											if(aftStopScene!=null&&!aftStopScene.canStopInScene(preEndAirportIndex, preEndDateTime, aftStartDateTime)) {
												aftCanStopResult=false;
											}
											//test if can aft stop
											if(aftCanStopResult) {
												iteratorDfsDfrag.resetBodyConnectFrag(flightFrag, globalDeputyDynamicFlight1, globalDeputyDynamicFlight2, DeputyFrag.ConnectOperation.OP_ALLPART);
												double allIteratorEarnScore=earnScore+iteratorDfsDfrag.getEarnScore();
												double allIteratorCostScore=costScore+iteratorDfsDfrag.getCostScore();
												//boundary check for cost score
												if(allIteratorCostScore<airlineMaxCostScore) {
													//continue to dfs with allpart operation
													flightFrag.setIsSearched(true);
													++iteratorDfsRunDfragNumber;
													//try tail first
													if(endAirportIndex==iteratorDfsTailSPortIndex) {
														iteratorDfsFixAirline(iteratorDfsTailFrag, allIteratorEarnScore, allIteratorCostScore);
													}
													//try body then
													ArrayList<FlightFrag> allIteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(endAirportIndex);
													for(int index=0, len=allIteratorDfsBodyFragList.size();index<len;++index) {
														FlightFrag nextBodyFlightFrag=allIteratorDfsBodyFragList.get(index);
														if(!nextBodyFlightFrag.isSearched()) {
															iteratorDfsFixAirline(nextBodyFlightFrag, allIteratorEarnScore, allIteratorCostScore);
														}
													}
													--iteratorDfsRunDfragNumber;
													flightFrag.setIsSearched(false);
												}
											}
										}
									}
								}
							}
						}
					}
					
					//after try prepart and allpart operations,  try straighten
					if(flightFrag.getCanStraighten()&&!queryIsLimitation(iteratorAirplaneIdIndex, startAirportIndex, endAirportIndex)) {	
						//straighten at first
						long travelTime=getEmptyTravelTime(iteratorAirplaneTypeIndex, iteratorAirplaneIdIndex, startAirportIndex, endAirportIndex);
						if(travelTime<=0) {
							travelTime=firstDynamicFlight.getTravelTime()+lastDynamicFlight.getTravelTime();
						}
						double aftFlightImportRatio=lastDynamicFlight.getImportRatio();
						globalDeputyDynamicFlight1.copyFrom(firstDynamicFlight);
						globalDeputyDynamicFlight1.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
						globalDeputyDynamicFlight1.setStraightenPrePart(travelTime, endAirportIndex, aftFlightImportRatio);
						int flightIdIndex=globalDeputyDynamicFlight1.getFlightIdIndex();
						long earliestStartDateTime=lastEndDateTime+getFlightIntervalTime(lastFlightIdIndex, flightIdIndex);
						boolean aheadResult=false;
						boolean delayResult=false;
						//try straighten ahead
						if(globalDeputyDynamicFlight1.getCanAhead()) {
							aheadResult=fitAheadDynamicFlight(globalDeputyDynamicFlight1,earliestStartDateTime);
						}
						//try straighten delay
						if(!aheadResult&&globalDeputyDynamicFlight1.getCanDelay())  {
							globalDeputyDynamicFlight1.resetStartDateTime();
							long originStartDateTime=globalDeputyDynamicFlight1.getOriginStartDateTime();
							if(originStartDateTime>=earliestStartDateTime) {
								delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
							}else {
								delayResult=globalDeputyDynamicFlight1.delayFlight(earliestStartDateTime-originStartDateTime);
								if(delayResult) {
									delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight1);
								}
							}
						}
						//adjust straighten time success
						if(aheadResult||delayResult) {
							boolean canStopResult=true;
							long startDateTime=globalDeputyDynamicFlight1.getStartDateTime();
							Scene stopScene=stopSceneArray.get(lastEndAirportIndex);
							if(stopScene!=null&&!stopScene.canStopInScene(lastEndAirportIndex, lastEndDateTime, startDateTime)) {
								canStopResult=false;
							}
							//test if can straighten stop
							if(canStopResult) {
								globalDeputyDynamicFlight2.copyFrom(lastDynamicFlight);
								globalDeputyDynamicFlight2.setStraightenAftPart();
								iteratorDfsDfrag.resetBodyConnectFrag(flightFrag, globalDeputyDynamicFlight1, globalDeputyDynamicFlight2, DeputyFrag.ConnectOperation.OP_STRAIGHTEN);
								double iteratorEarnScore=earnScore+iteratorDfsDfrag.getEarnScore();
								double iteratorCostScore=costScore+iteratorDfsDfrag.getCostScore();
								//boundary check for cost score
								if(iteratorCostScore<airlineMaxCostScore) {
									//continue to dfs with straighten operation
									flightFrag.setIsSearched(true);
									++iteratorDfsRunDfragNumber;
									//try tail first
									if(endAirportIndex==iteratorDfsTailSPortIndex) {
										iteratorDfsFixAirline(iteratorDfsTailFrag, iteratorEarnScore, iteratorCostScore);
									}
									//try body then
									ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(endAirportIndex);
									for(int index=0, len=iteratorDfsBodyFragList.size();index<len;++index) {
										FlightFrag nextBodyFlightFrag=iteratorDfsBodyFragList.get(index);
										if(!nextBodyFlightFrag.isSearched()) {
											iteratorDfsFixAirline(nextBodyFlightFrag, iteratorEarnScore, iteratorCostScore);
										}
									}
									--iteratorDfsRunDfragNumber;
									flightFrag.setIsSearched(false);
								}
							}
						}
					}
				}else if(lastEndAirportIndex==lastDynamicFlight.getStartAirportIndex()) {
				//start with body aft
					startAirportIndex=lastDynamicFlight.getStartAirportIndex();
					endAirportIndex=lastDynamicFlight.getEndAirportIndex();
					//test limitation first
					if(!queryIsLimitation(iteratorAirplaneIdIndex, startAirportIndex, endAirportIndex))
					{
						//change plane then
						globalDeputyDynamicFlight2.copyFrom(lastDynamicFlight);
						globalDeputyDynamicFlight2.changePlane(iteratorAirplaneIdIndex, iteratorAirplaneTypeIndex);
						int flightIdIndex=globalDeputyDynamicFlight2.getFlightIdIndex();
						long earliestStartDateTime=lastEndDateTime+getFlightIntervalTime(lastFlightIdIndex, flightIdIndex);
						boolean aheadResult=false;
						boolean delayResult=false;
						//try ahead
						if(globalDeputyDynamicFlight2.getCanAhead()) {
							aheadResult=fitAheadDynamicFlight(globalDeputyDynamicFlight2,earliestStartDateTime);
						}
						//try delay
						if(!aheadResult&&globalDeputyDynamicFlight2.getCanDelay())  {
							globalDeputyDynamicFlight2.resetStartDateTime();
							long originStartDateTime=globalDeputyDynamicFlight2.getOriginStartDateTime();
							if(originStartDateTime>=earliestStartDateTime) {
								delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight2);
							}else {
								delayResult=globalDeputyDynamicFlight2.delayFlight(earliestStartDateTime-originStartDateTime);
								if(delayResult) {
									delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight2);
								}
							}
						}
						//adjust time success
						if(aheadResult||delayResult) {
							boolean canStopResult=true;
							long startDateTime=globalDeputyDynamicFlight2.getStartDateTime();
							Scene stopScene=stopSceneArray.get(lastEndAirportIndex);
							if(stopScene!=null&&!stopScene.canStopInScene(lastEndAirportIndex, lastEndDateTime, startDateTime)) {
								canStopResult=false;
							}
							//test if can stop
							if(canStopResult) {
								globalDeputyDynamicFlight1.copyFrom(firstDynamicFlight);
								globalDeputyDynamicFlight1.cancelFlight();
								iteratorDfsDfrag.resetBodyConnectFrag(flightFrag, globalDeputyDynamicFlight1, globalDeputyDynamicFlight2, DeputyFrag.ConnectOperation.OP_AFTPART);
								double iteratorEarnScore=earnScore+iteratorDfsDfrag.getEarnScore();
								double iteratorCostScore=costScore+iteratorDfsDfrag.getCostScore();
								//boundary check for cost score
								if(iteratorCostScore<airlineMaxCostScore) {
									//continue to dfs with aftpart operation
									flightFrag.setIsSearched(true);
									++iteratorDfsRunDfragNumber;
									//try tail first
									if(endAirportIndex==iteratorDfsTailSPortIndex) {
										iteratorDfsFixAirline(iteratorDfsTailFrag, iteratorEarnScore, iteratorCostScore);
									}
									//try body then
									ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(endAirportIndex);
									for(int index=0, len=iteratorDfsBodyFragList.size();index<len;++index) {
										FlightFrag nextBodyFlightFrag=iteratorDfsBodyFragList.get(index);
										if(!nextBodyFlightFrag.isSearched()) {
											iteratorDfsFixAirline(nextBodyFlightFrag, iteratorEarnScore, iteratorCostScore);
										}
									}
									--iteratorDfsRunDfragNumber;
									flightFrag.setIsSearched(false);
								}
							}
						}
					}
				}
			}else if(flightFrag.isTailFrag()) {
			//tail
				//try delay tail
				DynamicFlight firstDynamicFlight=flightFrag.getFirstDynamicFlight();
				DynamicFlight lastDynamicFlight=flightFrag.getLastDynamicFlight();
				DynamicFlight dynamicFlight=flightFrag.getFirstDynamicFlight();
				boolean delayResult=true;
				int preFlightIdIndex=lastFlightIdIndex;
				long preEndDateTime=lastEndDateTime;
				int flightIdIndex;
				long earliestStartDateTime;
				long startDateTime;
				double tailCostScore=0;
				while(dynamicFlight!=null) {
					flightIdIndex=dynamicFlight.getFlightIdIndex();
					earliestStartDateTime=preEndDateTime+getFlightIntervalTime(preFlightIdIndex, flightIdIndex);
					startDateTime=dynamicFlight.getStartDateTime();
					if(startDateTime>=earliestStartDateTime) {
						if(dynamicFlight==firstDynamicFlight) {
							//record first flight, last flight will be in deputy 2
							globalDeputyDynamicFlight1.copyFrom(firstDynamicFlight);
						}
						break;
					}else {
						globalDeputyDynamicFlight2.copyFrom(dynamicFlight);
						delayResult=globalDeputyDynamicFlight2.delayFlight(earliestStartDateTime-startDateTime);
						if(delayResult) {
							delayResult=fitDelayDynamicFlight(globalDeputyDynamicFlight2);
						}
						if(!delayResult) {
							break;
						}
						tailCostScore+=globalDeputyDynamicFlight2.getScore();
						if(dynamicFlight==firstDynamicFlight) {
							//record first flight, last flight will be in deputy 2
							globalDeputyDynamicFlight1.copyFrom(globalDeputyDynamicFlight2);
						}
					}
					dynamicFlight=dynamicFlight.getNextDynamicFlight();
					preFlightIdIndex=flightIdIndex;
					preEndDateTime=globalDeputyDynamicFlight2.getEndDateTime();
				}
				//delay tail success
				if(delayResult) {
					boolean canStopResult=true;
					startDateTime=globalDeputyDynamicFlight1.getStartDateTime();
					Scene stopScene=stopSceneArray.get(lastEndAirportIndex);
					if(stopScene!=null&&!stopScene.canStopInScene(lastEndAirportIndex, lastEndDateTime, startDateTime)) {
						canStopResult=false;
					}
					//test if can stop
					if(canStopResult) {
						if(dynamicFlight!=null) {
							globalDeputyDynamicFlight2.copyFrom(lastDynamicFlight);
						}
						iteratorDfsDfrag.resetTailFrag(flightFrag, globalDeputyDynamicFlight1, globalDeputyDynamicFlight2, tailCostScore);
						double iteratorEarnScore=earnScore+iteratorDfsDfrag.getEarnScore();
						double iteratorCostScore=costScore+iteratorDfsDfrag.getCostScore();
						//boundary check for cost score
						if(iteratorCostScore<airlineMaxCostScore) {
							//got a result for dfs
							flightFrag.setIsSearched(true);
							++iteratorDfsRunDfragNumber;
							//record result number for present iterator airline
							++iteratorDfsRecResultNumber;
							//test if it is a better result
							boolean betterFlag=false;
							double iteratorDfsRunScore=0;
							if(iteratorDfsMode==IteratorDfsMode.DFSMODE_GENTLE) {
							//gentle mode
								iteratorDfsRunScore=iteratorCostScore;
								if(iteratorDfsRunScore<iteratorDfsRecScore||(iteratorDfsRunScore==iteratorDfsRecScore&&iteratorEarnScore>iteratorDfsRecEarnScore)) {
									betterFlag=true;
								}
							}else if(iteratorDfsMode==IteratorDfsMode.DFSMODE_GREEDY) {
							//greedy mode
								iteratorDfsRunScore=iteratorEarnScore-iteratorCostScore;
								if(iteratorDfsRunScore>iteratorDfsRecScore||(iteratorDfsRunScore==iteratorDfsRecScore&&iteratorEarnScore>iteratorDfsRecEarnScore)) {
									betterFlag=true;
								}
							}
							//if it is a better result 
							if(betterFlag) {
								iteratorDfsRecScore=iteratorDfsRunScore;
								iteratorDfsRecEarnScore=iteratorEarnScore;
								iteratorDfsRecCostScore=iteratorCostScore;
								iteratorDfsRecDfragNumber=iteratorDfsRunDfragNumber;
								for(int index=0;index<iteratorDfsRunDfragNumber;++index) {
									iteratorDfsRecDfragArray.get(index).copyFrom(iteratorDfsRunDfragArray.get(index));
								}
							}
							--iteratorDfsRunDfragNumber;
							flightFrag.setIsSearched(false);
						}
					}
				}
			}
		}
	}

	private void resultAirlines() {
		System.out.println("resultAirlines\n");
		
		for(int index=0, len=iteratorDfsBodyFragArray.size();index<len;++index) {
			
			ArrayList<FlightFrag> iteratorDfsBodyFragList=iteratorDfsBodyFragArray.get(index);
			for(int indexi=0, leni=iteratorDfsBodyFragList.size();indexi<leni;++indexi) {
				FlightFrag remainBodyFrag=iteratorDfsBodyFragList.get(indexi);
				remainBodyFrag.getFirstDynamicFlight().cancelFlight();
				remainBodyFrag.getLastDynamicFlight().cancelFlight();
			}
		}
		
		for(int index=0, len=iteratorResultDfragArray.size();index<len;++index) {
			int airplaneIdIndex=index;
			ArrayList<DeputyFrag> iteratorResultDfragList=iteratorResultDfragArray.get(airplaneIdIndex); 
			int iteratorResultDfragListLength=iteratorResultDfragNumberArray[airplaneIdIndex];
			DeputyFrag headDeputyFrag=iteratorResultDfragList.get(0);
			int airplaneTypeIndex=headDeputyFrag.getAirplaneTypeIndex();
			//deal with body
			for(int indexi=1;indexi<iteratorResultDfragListLength-1;++indexi) {
				DeputyFrag bodyDeputyFrag=iteratorResultDfragList.get(indexi);
				FlightFrag bodyFlightFrag=bodyDeputyFrag.getFlightFrag();
				long startDateTime=bodyDeputyFrag.getStartDateTime();
				long endDateTime=bodyDeputyFrag.getEndDateTime();
				if(bodyFlightFrag.isBodySingleFrag()) {
					DynamicFlight dynamicFlight=bodyFlightFrag.getFirstDynamicFlight();
					dynamicFlight.changePlane(airplaneIdIndex, airplaneTypeIndex);
					long originStartDateTime=dynamicFlight.getOriginStartDateTime();
					if(startDateTime>originStartDateTime) {
						dynamicFlight.delayFlight(startDateTime-originStartDateTime);
					}else if(startDateTime<originStartDateTime) {
						dynamicFlight.aheadFlight(originStartDateTime-startDateTime);
					}
				}else if(bodyFlightFrag.isBodyConnectFrag()) {
					DynamicFlight preDynamicFlight=bodyFlightFrag.getFirstDynamicFlight();
					DynamicFlight aftDynamicFlight=bodyFlightFrag.getLastDynamicFlight();
					DeputyFrag.ConnectOperation connectOperation=bodyDeputyFrag.getConnectOperation();
					if(connectOperation==DeputyFrag.ConnectOperation.OP_PREPART) {
						preDynamicFlight.changePlane(airplaneIdIndex, airplaneTypeIndex);
						long preOriginStartDateTime=preDynamicFlight.getOriginStartDateTime();
						if(startDateTime>preOriginStartDateTime) {
							preDynamicFlight.delayFlight(startDateTime-preOriginStartDateTime);
						}else if(startDateTime<preOriginStartDateTime) {
							preDynamicFlight.aheadFlight(preOriginStartDateTime-startDateTime);
						}
						
						aftDynamicFlight.cancelFlight();
					}
					if(connectOperation==DeputyFrag.ConnectOperation.OP_AFTPART) {
						preDynamicFlight.cancelFlight();
						
						aftDynamicFlight.changePlane(airplaneIdIndex, airplaneTypeIndex);
						long aftOriginStartDateTime=aftDynamicFlight.getStartDateTime();
						if(startDateTime>aftOriginStartDateTime) {
							aftDynamicFlight.delayFlight(startDateTime-aftOriginStartDateTime);
						}else if(startDateTime<aftOriginStartDateTime) {
							aftDynamicFlight.aheadFlight(aftOriginStartDateTime-startDateTime);
						}
					}
					if(connectOperation==DeputyFrag.ConnectOperation.OP_ALLPART) {
						preDynamicFlight.changePlane(airplaneIdIndex, airplaneTypeIndex);
						long preOriginStartDateTime=preDynamicFlight.getOriginStartDateTime();
						if(startDateTime>preOriginStartDateTime) {
							preDynamicFlight.delayFlight(startDateTime-preOriginStartDateTime);
						}else if(startDateTime<preOriginStartDateTime) {
							preDynamicFlight.aheadFlight(preOriginStartDateTime-startDateTime);
						}
						
						aftDynamicFlight.changePlane(airplaneIdIndex, airplaneTypeIndex);
						long aftOriginEndDateTime=aftDynamicFlight.getEndDateTime();
						if(endDateTime>aftOriginEndDateTime) {
							aftDynamicFlight.delayFlight(endDateTime-aftOriginEndDateTime);
						}else if(endDateTime<aftOriginEndDateTime) {
							aftDynamicFlight.aheadFlight(aftOriginEndDateTime-endDateTime);
						}
					}
					if(connectOperation==DeputyFrag.ConnectOperation.OP_STRAIGHTEN) {
						preDynamicFlight.changePlane(airplaneIdIndex, airplaneTypeIndex);
						long travelTime=bodyDeputyFrag.getEndDateTime()-bodyDeputyFrag.getStartDateTime();
						int endAirportIndex=bodyDeputyFrag.getEndAirportIndex();
						double nextImportRatio=aftDynamicFlight.getImportRatio();
						preDynamicFlight.setStraightenPrePart(travelTime, endAirportIndex, nextImportRatio);
						long preOriginStartDateTime=preDynamicFlight.getOriginStartDateTime();
						if(startDateTime>preOriginStartDateTime) {
							preDynamicFlight.delayFlight(startDateTime-preOriginStartDateTime);
						}else if(startDateTime<preOriginStartDateTime) {
							preDynamicFlight.aheadFlight(preOriginStartDateTime-startDateTime);
						}
						
						aftDynamicFlight.setStraightenAftPart();
					}
				}
			}
			//deal with tail
			DeputyFrag tailDeputyFrag=iteratorResultDfragList.get(iteratorResultDfragListLength-1);
			FlightFrag tailFlightFrag=tailDeputyFrag.getFlightFrag();
			long tailStartDateTime=tailDeputyFrag.getStartDateTime();
			DynamicFlight tailFirstDynamicFlight=tailFlightFrag.getFirstDynamicFlight();
			long tailFirstOriginStartDateTime=tailFirstDynamicFlight.getStartDateTime();
			if(tailStartDateTime>tailFirstOriginStartDateTime) {
				tailFirstDynamicFlight.delayFlight(tailStartDateTime-tailFirstOriginStartDateTime);
			}
			int preFlightIdIndex=tailFirstDynamicFlight.getFlightIdIndex();
			long preEndDateTime=tailFirstDynamicFlight.getEndDateTime();
			int flightIdIndex;
			long earliestStartDateTime;
			long startDateTime;
			DynamicFlight tailDynamicFlight=tailFirstDynamicFlight.getNextDynamicFlight();
			while(tailDynamicFlight!=null) {
				flightIdIndex=tailDynamicFlight.getFlightIdIndex();
				earliestStartDateTime=preEndDateTime+getFlightIntervalTime(preFlightIdIndex, flightIdIndex);
				startDateTime=tailDynamicFlight.getStartDateTime();
				if(startDateTime>=earliestStartDateTime) {
					break;
				}else {
					tailDynamicFlight.delayFlight(earliestStartDateTime-startDateTime);
				}
				preFlightIdIndex=flightIdIndex;
				preEndDateTime=tailDynamicFlight.getEndDateTime();
				tailDynamicFlight=tailDynamicFlight.getNextDynamicFlight();
			}
		}
		
		System.out.println("resultAirlines done\n\n");
	}
	
	private void readInputData(InputStream inputStream){

		try {
			XSSFWorkbook workBook = new XSSFWorkbook(inputStream);
			//读取航班信息
			XSSFSheet flightSheet = workBook.getSheet("航班");
			Iterator<Row> rowIterator = flightSheet.iterator();
			while(rowIterator.hasNext()){
				Row row = rowIterator.next();
				if(row.getRowNum() == 0)
					continue;
				OriginFlight flight = new OriginFlight(row);
				originFlightArray.add(flight);
				int planeIdIndex=flight.getAirplaneIdIndex();
				if(planeIdIndex<originAirLineArray.size()) {
					originAirLineArray.get(planeIdIndex).add(flight);
				}else {
					ArrayList<OriginFlight> flightlist=new ArrayList<>();
					flightlist.add(flight);
					originAirLineArray.add(flightlist);
				}
			}

			//读取航线-飞机限制信息
			Map<Integer, ArrayList<AirplaneLimitation>> airplanelimitationmap=new HashMap<>();
			XSSFSheet airplaneLimitSheet = workBook.getSheet("航线-飞机限制");
			rowIterator = airplaneLimitSheet.iterator();
			while(rowIterator.hasNext()){
				Row row = rowIterator.next();
				if(row.getRowNum() == 0)
					continue;
				AirplaneLimitation airplaneLimitation = new AirplaneLimitation(row);
				Integer key = airplaneLimitation.getAirplaneIdIndex();
				if(airplanelimitationmap.containsKey(key)) {
					airplanelimitationmap.get(key).add(airplaneLimitation);
				}else {
					ArrayList<AirplaneLimitation> airplaneLimitationList = new ArrayList<>();
					airplaneLimitationList.add(airplaneLimitation);
					airplanelimitationmap.put(key, airplaneLimitationList);
				}
			}
			for(int index=0, len=InfoInterpreter.airplaneIdArray.size();index<len;++index) {
				Integer key=index;
				ArrayList<AirplaneLimitation> limitationlist=airplanelimitationmap.get(key);
				if(limitationlist!=null) {
					//sorted limitation list is quick for later searching
					Collections.sort(limitationlist);
				}
				airplaneLimitationArray.add(limitationlist);  //element could be null
			}

			//读取机场关闭限制信息
			Map<Integer, ArrayList<AirportClose>> airportclosemap=new HashMap<>();
			XSSFSheet airportCloseSheet = workBook.getSheet("机场关闭限制");
			rowIterator = airportCloseSheet.iterator();
			while(rowIterator.hasNext()){
				Row row = rowIterator.next();
				if(row.getRowNum() == 0)
					continue;
				AirportClose airportClose = new AirportClose(row);
				Integer key = airportClose.getAirportIndex();
				if(airportclosemap.containsKey(key)) {
					airportclosemap.get(key).add(airportClose);
				}else {
					ArrayList<AirportClose> airportCloseList = new ArrayList<>();
					airportCloseList.add(airportClose);
					airportclosemap.put(key, airportCloseList);
				}
			}
			for(int index=0, len=InfoInterpreter.airportNameArray.size();index<len;++index) {
				Integer key=index;	
				airportCloseArray.add(airportclosemap.get(key));  //element could be null
			}

			//读取故障信息
			Map<Integer, ArrayList<Scene>> scenemap=new HashMap<>();
			XSSFSheet typhoonSceneSheet = workBook.getSheet("台风场景");
			rowIterator = typhoonSceneSheet.iterator();
			while(rowIterator.hasNext()){
				Row row = rowIterator.next();
				if(row.getRowNum() == 0)
					continue;
				Scene scene = new Scene(row);
				Integer key=scene.getAirportIndex();
				if(scenemap.containsKey(key)) {
					scenemap.get(key).add(scene);
				}else {
					ArrayList<Scene> scenelist = new ArrayList<>();
					scenelist.add(scene);
					scenemap.put(key, scenelist);
				}
			}
			for(int index=0, len=InfoInterpreter.airportNameArray.size();index<len;++index) {
				Integer key=index;	
				ArrayList<Scene> sceneList=scenemap.get(key);
				Scene startScene=null;
				Scene endScene=null;
				Scene stopScene=null;
				if(sceneList!=null){
					for(int indexi=0, leni=sceneList.size();indexi<leni;++indexi) {
						Scene scene=sceneList.get(indexi);
						if(scene.isStartSceneType()) {
							startScene=scene;
						}
						if(scene.isEndSceneType()) {
							endScene=scene;
						}
						if(scene.isStopSceneType()) {
							stopScene=scene;
						}
					}
				}
				startSceneArray.add(startScene);  //element could be null
				endSceneArray.add(endScene);  //element could be null
				stopSceneArray.add(stopScene);  //element could be null
			}

			//读取飞行时间信息
			int planetypenumber=InfoInterpreter.airplaneTypeArray.size();
			int portnumber=InfoInterpreter.airportNameArray.size();
			travelTimeCollection=new long[planetypenumber][portnumber][portnumber];
			XSSFSheet travelTimeSheet = workBook.getSheet("飞行时间");
			rowIterator = travelTimeSheet.iterator();
			Integer planetypeindex;
			int startportindex,endportindex;
			while(rowIterator.hasNext()){
				Row row = rowIterator.next();
				if(row.getRowNum() == 0)
					continue;
				DataFormatter df = new DataFormatter();
				planetypeindex = InfoInterpreter.airplaneTypeMap.get(df.formatCellValue(row.getCell(0)));
				if(planetypeindex!=null) {
					startportindex = InfoInterpreter.airportNameMap.get(df.formatCellValue(row.getCell(1)));
					endportindex = InfoInterpreter.airportNameMap.get(df.formatCellValue(row.getCell(2)));
					travelTimeCollection[planetypeindex][startportindex][endportindex] = 60000*Integer.parseInt(df.formatCellValue(row.getCell(3)));
				}
			}
			
			domesticAirportRecord=new boolean[InfoInterpreter.airportNameArray.size()];
			XSSFSheet airportSheet = workBook.getSheet("机场");
            rowIterator = airportSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                DataFormatter df = new DataFormatter();
                String airport = df.formatCellValue(row.getCell(0));
                int airportNameIndex=InfoInterpreter.airportNameMap.get(airport);
                int domesticFlag = Integer.parseInt(df.formatCellValue(row.getCell(1)));
                if(domesticFlag == 1){
                	domesticAirportRecord[airportNameIndex]=true;
                }
            }

			//关闭excel
			workBook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean queryIsLimitation(int planeIdIndex, int startPortIndex, int endPortIndex) {
		ArrayList<AirplaneLimitation> limitationList=airplaneLimitationArray.get(planeIdIndex);
		if(limitationList!=null) {
			int airportnumber=InfoInterpreter.airportNameArray.size();
			int searchNumber=startPortIndex*airportnumber+endPortIndex;
			int lowindex=0;
			int highindex=limitationList.size()-1;
			int midindex, tempresult;
			while(lowindex<=highindex) {
				midindex=(lowindex+highindex)/2;
				tempresult=limitationList.get(midindex).compareSearchNumber(searchNumber);
				if(tempresult>0) {
					highindex=midindex-1;
				}
				else if(tempresult<0) {
					lowindex=midindex+1;
				}else {
					return true;
				}
			}
		}
		return false;
	}

	private Scene getStartScene(DynamicFlight dynamicFlight) {
		Scene startScene=null;
		int startAirportIndex = dynamicFlight.getStartAirportIndex();
		long startDateTime= dynamicFlight.getStartDateTime();
		Scene tempStartScene=startSceneArray.get(startAirportIndex);
		if(tempStartScene!=null&&tempStartScene.isStartInScene(startAirportIndex, startDateTime)) {
			startScene=tempStartScene;
		}
		return startScene;
	}

	private Scene getEndScene(DynamicFlight dynamicFlight) {
		Scene endScene=null;
		int endAirportIndex = dynamicFlight.getEndAirportIndex();
		long endDateTime= dynamicFlight.getEndDateTime();
		Scene tempEndScene=endSceneArray.get(endAirportIndex);
		if(tempEndScene!=null&&tempEndScene.isEndInScene(endAirportIndex, endDateTime)) {
			endScene=tempEndScene;
		}
		return endScene;
	}

	private Scene getStopScene(DynamicFlight preDynamicFlight, DynamicFlight dynamicFlight) {
		Scene stopScene=null;
		int airportIndex=preDynamicFlight.getEndAirportIndex();
		long startTime=preDynamicFlight.getEndDateTime();
		long endTime=dynamicFlight.getStartDateTime();
		Scene tempStopScene=stopSceneArray.get(airportIndex);
		if(tempStopScene!=null&&tempStopScene.isStopInScene(airportIndex, startTime, endTime)) {
			stopScene=tempStopScene;
		}
		return stopScene;
	}
	
	private Scene getStopScene(int stopAirportIndex, int stopStartDateTime, int stopEndDateTime) {
		Scene stopScene=null;
		Scene tempStopScene=stopSceneArray.get(stopAirportIndex);
		if(tempStopScene!=null&&tempStopScene.isStopInScene(stopAirportIndex, stopStartDateTime, stopEndDateTime)) {
			stopScene=tempStopScene;
		}
		return stopScene;
		
	}
	
	private CapacityLimitation getInStartCapacityLimitation(DynamicFlight dynamicFlight) {
		CapacityLimitation beforeCapacityLimitation=null;
		int startAirportIndex=dynamicFlight.getStartAirportIndex();
		long startDateTime=dynamicFlight.getStartDateTime();
		CapacityLimitation tempCapacityLimitation=capacityLimitationArray.get(startAirportIndex);
		if(tempCapacityLimitation!=null) {
			if(tempCapacityLimitation.isInCapacityLimitation(startDateTime)) {
				beforeCapacityLimitation=tempCapacityLimitation;
			}
		}
		return beforeCapacityLimitation;
	}
	
	private CapacityLimitation getInEndCapacityLimitation(DynamicFlight dynamicFlight) {
		CapacityLimitation afterCapacityLimitation=null;
		int endAirportIndex=dynamicFlight.getEndAirportIndex();
		long endDateTime=dynamicFlight.getEndDateTime();
		CapacityLimitation tempCapacityLimitation=capacityLimitationArray.get(endAirportIndex);
		if(tempCapacityLimitation!=null) {
			if(tempCapacityLimitation.isInCapacityLimitation(endDateTime)) {
				afterCapacityLimitation=tempCapacityLimitation;
			}
		}
		return afterCapacityLimitation;
	}
	
	private boolean isInAdjustWindow(DynamicFlight dynamicFlight) {
		long startDateTime=dynamicFlight.getStartDateTime();
		if(adjustTimeWindow.isInAdjustTimeWindow(startDateTime)) {
			return true;
		}
		return false;
	}

	private AirportClose getStartAirportClose(DynamicFlight dynamicFlight) {
		AirportClose startAirportClose=null;
		int startAirportIndex=dynamicFlight.getStartAirportIndex();
		ArrayList<AirportClose> startAirportCloseList=airportCloseArray.get(startAirportIndex);
		if(startAirportCloseList!=null) {
			long startDateTime=dynamicFlight.getStartDateTime();
			for(int index=0, len=startAirportCloseList.size();index<len;++index) {
				if(startAirportCloseList.get(index).isClosed(startDateTime)) {
					startAirportClose=startAirportCloseList.get(index);
					break;
				}
			}
		}
		return startAirportClose;
	}

	private AirportClose getEndAirportClose(DynamicFlight dynamicFlight) {
		AirportClose endAirportClose=null;
		int endAirportIndex=dynamicFlight.getEndAirportIndex();
		ArrayList<AirportClose> endAirportCloseList=airportCloseArray.get(endAirportIndex);
		if(endAirportCloseList!=null) {
			long endDateTime=dynamicFlight.getEndDateTime();
			for(int index=0, len=endAirportCloseList.size();index<len;++index) {
				if(endAirportCloseList.get(index).isClosed(endDateTime)) {
					endAirportClose=endAirportCloseList.get(index);
					break;
				}
			}
		}
		return endAirportClose;
	}

	private long getEmptyTravelTime(int planeTypeIndex, int planeIdIndex, int startEmptyAiportIndex, int endEmpyAirportIndex) {
		//if both domestic && if not in limitation list && if exist in travel time list
		if(startEmptyAiportIndex!=endEmpyAirportIndex
				&&domesticAirportRecord[startEmptyAiportIndex]
						&&domesticAirportRecord[endEmpyAirportIndex]
								&&!queryIsLimitation(planeIdIndex, startEmptyAiportIndex, endEmpyAirportIndex)
								&&travelTimeCollection[planeTypeIndex][startEmptyAiportIndex][endEmpyAirportIndex]>0) {

			return travelTimeCollection[planeTypeIndex][startEmptyAiportIndex][endEmpyAirportIndex];
		}
		//otherwise return -1
		return -1;
	}

	private long getFlightIntervalTime(int preFlightIdIndex, int flightIdIndex) {
		long intervalTime = Configuration.maxIntervalTime;
        if(preFlightIdIndex<originFlightArray.size()
     		   &&flightIdIndex<originFlightArray.size()) {
     	   OriginFlight preOriginFlight=originFlightArray.get(preFlightIdIndex);
     	   OriginFlight originFlight=originFlightArray.get(flightIdIndex);
     	   if(preOriginFlight.getNextOriginFlight()==originFlight) {
     		   long originIntervalTime=originFlight.getStartDateTime()-preOriginFlight.getEndDateTime();
     		   if(originIntervalTime<intervalTime) {
     			   intervalTime=originIntervalTime;
     		   }
     	   }
        }
		return intervalTime;
	}
	
	private boolean fitAheadDynamicFlight(DynamicFlight dynamicFlight, long earliestStartDateTime) {
		int startAirportIndex=dynamicFlight.getStartAirportIndex();
		int endAirportIndex=dynamicFlight.getEndAirportIndex();
		CapacityLimitation startCapacityLimitation=capacityLimitationArray.get(startAirportIndex);
		CapacityLimitation endCapacityLimitation=capacityLimitationArray.get(endAirportIndex);
		while(true) {
			long aheadInterval=0;
			long startDateTime=dynamicFlight.getStartDateTime();
			long endDateTime=dynamicFlight.getEndDateTime();
			Scene startScene=getStartScene(dynamicFlight);
			if(startScene!=null) {
				aheadInterval=java.lang.Long.max(aheadInterval, startDateTime-startScene.getStartDateTime());
			}
			Scene endScene=getEndScene(dynamicFlight);
			if(endScene!=null) {
				aheadInterval=java.lang.Long.max(aheadInterval, endDateTime-endScene.getStartDateTime());
			}
			AirportClose startAirportClose=getStartAirportClose(dynamicFlight);
			if(startAirportClose!=null) {
				aheadInterval=java.lang.Long.max(aheadInterval, startDateTime-startAirportClose.getStartDateTime(startDateTime));
			}
			AirportClose endAirportClose=getEndAirportClose(dynamicFlight);
			if(endAirportClose!=null) {
				aheadInterval=java.lang.Long.max(aheadInterval, endDateTime-endAirportClose.getStartDateTime(endDateTime));
			}
			if(startCapacityLimitation!=null&&!startCapacityLimitation.canInCapacityLimitation(startDateTime)) {
				aheadInterval=java.lang.Long.max(aheadInterval, 300000);
			}
			if(endCapacityLimitation!=null&&!endCapacityLimitation.canInCapacityLimitation(endDateTime)) {
				aheadInterval=java.lang.Long.max(aheadInterval, 300000);
			}
			if(startDateTime-aheadInterval<earliestStartDateTime) {
				//ahead failed
				return false;
			}
			
			if(aheadInterval>0) {
				if(!dynamicFlight.aheadFlight(aheadInterval)) {
					//ahead failed
					return false;
				}
			}else {
				//ahead success
				return true;
			}
		}
	}

	private boolean fitDelayDynamicFlight(DynamicFlight dynamicFlight) {
		int startAirportIndex=dynamicFlight.getStartAirportIndex();
		int endAirportIndex=dynamicFlight.getEndAirportIndex();
		CapacityLimitation startCapacityLimitation=capacityLimitationArray.get(startAirportIndex);
		CapacityLimitation endCapacityLimitation=capacityLimitationArray.get(endAirportIndex);
		while(true) {
			long delayInterval=0;
			long startDateTime=dynamicFlight.getStartDateTime();
			long endDateTime=dynamicFlight.getEndDateTime();
			Scene startScene=getStartScene(dynamicFlight);
			if(startScene!=null) {
				delayInterval=java.lang.Long.max(delayInterval, startScene.getEndDateTime()-startDateTime);
			}
			Scene endScene=getEndScene(dynamicFlight);
			if(endScene!=null) {
				delayInterval=java.lang.Long.max(delayInterval, endScene.getEndDateTime()-endDateTime);
			}
			AirportClose startAirportClose=getStartAirportClose(dynamicFlight);
			if(startAirportClose!=null) {
				delayInterval=java.lang.Long.max(delayInterval, startAirportClose.getEndDateTime(startDateTime)-startDateTime);
			}
			AirportClose endAirportClose=getEndAirportClose(dynamicFlight);
			if(endAirportClose!=null) {
				delayInterval=java.lang.Long.max(delayInterval, endAirportClose.getEndDateTime(endDateTime)-endDateTime);
			}
			if(startCapacityLimitation!=null&&!startCapacityLimitation.canInCapacityLimitation(startDateTime)) {
				delayInterval=java.lang.Long.max(delayInterval, 300000);
			}
			if(endCapacityLimitation!=null&&!endCapacityLimitation.canInCapacityLimitation(endDateTime)) {
				delayInterval=java.lang.Long.max(delayInterval, 300000);
			}
			
			if(delayInterval>0) {
				if(!dynamicFlight.delayFlight(delayInterval)) {
					//delay failed
					return false;
				}
			}else {
				//delay success
				return true;
			}
		}
	}

}

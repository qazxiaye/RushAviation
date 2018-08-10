package minion.rushAviation;


import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by minion rush on 17/8/16.
 */
public class Main {

    public static void main(String[] args) {
        String inputDataFilePath = "data/厦航大赛数据20170814.xlsx";
        String resultDataFilePath = "data/mycsv/minion rush_766505.582_0.csv";
        try {
            InputStream inputDataStream = new FileInputStream(inputDataFilePath);
            ResultSolver resultSolver = new ResultSolver(inputDataStream);
            resultSolver.runSolver();
            double score = resultSolver.generateResult(resultDataFilePath);
            System.out.println("选手所得分数为：" + score+"\n\n");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}

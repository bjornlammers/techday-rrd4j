package nl.avisi.techday.rrd4j.heatmap;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class HeatMap {
    public static void main(String[] args) throws IOException, ParseException {
        HeatMap heatMapCreator = new HeatMap();
        double[][] heatMap = heatMapCreator.create();
        heatMapCreator.printHeatMap(heatMap);
    }
    
    public double[][] create() throws IOException, ParseException {
        RrdDb rrdDb = new RrdDb(getRrdFile(), true);

        System.out.println("== Last update time was: " + rrdDb.getLastUpdateTime());
        System.out.println("== Last info was: " + rrdDb.getInfo());

        // fetch data
        double[][] heatMap = new double[7][24];
        System.out.println("== Fetching data for the whole month");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH");
        Date startDate = formatter.parse("2013-06-23 00");
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        start.set(Calendar.HOUR, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        for (int week = 0; week < 4; week++) {
            for (int dagVanWeek = 0; dagVanWeek < 7; dagVanWeek++) {
                for (int uurVanDag = 0; uurVanDag < 24; uurVanDag++) {
                    // TODO: fetch the data for one hour
/*
                    FetchRequest request = rrdDb.createFetchRequest(ConsolFun.???, startInSeconds, endInSeconds);
                    FetchData fetchData = request.fetchData();
                    Double fractionOpen = fetchData.getAggregate("open", ConsolFun.TOTAL);
*/
                    // TODO: divide total by number of data points; check for invalid data points (NaN)
/*
                    int rowCount = fetchData.getRowCount();
                    ...
                    heatMap[dagVanWeek][uurVanDag] += ...;
 */
                    start.add(Calendar.HOUR_OF_DAY, +1);
                }
            }
        }
        return heatMap;
    }
    
    public void printHeatMap(double[][] heatMap) {
        System.out.println();
        DecimalFormat decimalFormatter = new DecimalFormat("0.00");
        String[] daysOfWeek = new String[] { "zat", "zon", "maa", "din", "woe", "don", "vri" };
        for (int dagVanWeek = 0; dagVanWeek < 7; dagVanWeek++) {
            System.out.print(daysOfWeek[dagVanWeek]);
            for (int uurVanDag = 0; uurVanDag < 24; uurVanDag++) {
                if (heatMap[dagVanWeek][uurVanDag] == Double.NaN) {
                    System.out.print(" [?.??]");
                } else {
                    System.out.print(" [" + decimalFormatter.format(heatMap[dagVanWeek][uurVanDag] / 4) + "]");
                }
            }
            System.out.println();
        }
    }
    
    private String getRrdFile() {
        return this.getClass().getClassLoader().getResource("status.rrd").getPath();
    }
}

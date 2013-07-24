package nl.avisi.techday.rrd4j.grapher;

import org.apache.log4j.Logger;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.rrd4j.ConsolFun.AVERAGE;

public class ExampleGrapher {
    private static final Logger LOG = Logger.getLogger(ExampleGrapher.class);
    
    public static void main(final String[] args) throws IOException, ParseException {
        ExampleGrapher grapher = new ExampleGrapher();
        grapher.showDatasources();
        grapher.createTemperatuurGraph();
    }
    
    public void showDatasources() throws IOException {
        RrdDb rrdDb = new RrdDb(getRrdFile());
        String[] dsNames = rrdDb.getDsNames();
        for (int i = 0; i < dsNames.length; i++) {
            Datasource datasource = rrdDb.getDatasource(dsNames[i]);
            LOG.info("Datasource: [" + datasource.getName() + "] minval=[" + datasource.getMinValue() + "] maxval=["
                    + datasource.getMaxValue() + "] heart=[" + datasource.getHeartbeat() + "]");
        }
    }
    
    public void createTemperatuurGraph() throws ParseException {
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setWidth(800);
        gDef.setHeight(600);
        gDef.setFilename("temp-graph.png");
        SimpleDateFormat parser = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date start = parser.parse("24-07-2013 09:00:00");
        Date end = parser.parse("24-07-2013 17:00:00");
        gDef.setStartTime(Util.getTimestamp(start));
        gDef.setEndTime(Util.getTimestamp(end));
        gDef.setTitle("Temperatuur");
        gDef.datasource("temperatuur", getRrdFile(), "temperatuur", AVERAGE);
        gDef.line("temperatuur", Color.RED, "temperatuur", 3f);
        
        gDef.setImageInfo("");
        gDef.setPoolUsed(false);
        gDef.setImageFormat("png");

        // create graph
        try {
            new RrdGraph(gDef);
            LOG.info("Wrote new temperature graph");
        } catch (IOException e) {
            LOG.error("Couldn't write temperature graph", e);
        }
    }
    
    private String getRrdFile() {
        return this.getClass().getClassLoader().getResource("nu.rrd").getPath();
    }
}

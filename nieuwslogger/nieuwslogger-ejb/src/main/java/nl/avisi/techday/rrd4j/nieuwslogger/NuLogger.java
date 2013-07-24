package nl.avisi.techday.rrd4j.nieuwslogger;

import static org.rrd4j.ConsolFun.AVERAGE;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@LocalBean
public class NuLogger {
    private static final String GAUGE_TEMPERATUUR = "temperatuur";

    // TODO: define this
    private static final String RRD4J_FILE = "/your/path/to/nu.rrd";
    
    // TODO: define this
    private static final String GRAPH_PATH = "/your/graphs/folder/";

    private static final Logger LOG = Logger.getLogger(NuLogger.class);

    private HttpClient client;
    private RrdDef rrdDef;
    
    @PostConstruct
    public void init() {
        LOG.info("init");
        client = new HttpClient();
        HttpClientParams params = new HttpClientParams();
        params.setSoTimeout(1000);
        client.setParams(params);
        rrdDef = createRrdDef();
        checkRrdFile();
    }
    
    /* TODO: schedule this method to collect data at a convenient interval */
    public void retrieveData() {
        LOG.debug("Retrieving data");
        Document nusite = getRemoteStatus();
        if (nusite == null) {
            LOG.warn("Data could not be retrieved");
        } else {
            NuSiteParser parser = new NuSiteParser(nusite);
            try {
                RrdDb rrdDb = new RrdDb(RRD4J_FILE);
                Sample sample = rrdDb.createSample();
                long timestamp = Util.getTimestamp(new Date());
                LOG.info("creating sample with timestamp: " + timestamp);
                sample.setTime(timestamp);
                Integer temperatuur = parser.getTemperatuur();
                if (temperatuur != null) {
                    sample.setValue(GAUGE_TEMPERATUUR, temperatuur);
                }
                // TODO: add other values to the sample
                sample.update();
                rrdDb.close();
            } catch (IOException e) {
                LOG.error("Could not create sample in RRD DB");
                e.printStackTrace();
            }
        }
    }
    
    @Schedule(hour = "*", minute = "*", second = "15", persistent = false)
    public void createHourGraph() {
        LOG.info("Creating hour graphs");
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setWidth(800);
        gDef.setHeight(600);
        gDef.setFilename(GRAPH_PATH + "temp-hour.png");
        Calendar now = new GregorianCalendar();
        Calendar oneHourEarlier = new GregorianCalendar();
        oneHourEarlier.add(Calendar.HOUR, -1);
        gDef.setStartTime(Util.getTimestamp(oneHourEarlier));
        gDef.setEndTime(Util.getTimestamp(now));
        gDef.setTitle("Last hour");
        gDef.datasource("temperatuur", RRD4J_FILE, GAUGE_TEMPERATUUR, AVERAGE);
        gDef.line("temperatuur", Color.RED, "temperatuur", 3f);
        
        // TODO: create other graphs, or add other data to this graph
        gDef.setImageInfo("");
        gDef.setPoolUsed(false);
        gDef.setImageFormat("png");

        // create graph
        try {
            new RrdGraph(gDef);
            LOG.info("Wrote new hour graph");
        } catch (IOException e) {
            LOG.error("Couldn't write hour graph", e);
        }
    }
    
    private Document getRemoteStatus() {
        Document doc;
        try {
            doc = Jsoup.connect("http://www.nu.nl/").get();
        } catch (IOException e) {
            LOG.error("Kon nu.nl HTML niet ophalen", e);
            doc = null;
        }
        return doc;
    }
    
    private RrdDef createRrdDef() {
        String rrdPath = RRD4J_FILE;
        RrdDef rrdDef = new RrdDef(rrdPath, /* step in seconds */ 60);
        rrdDef.addDatasource(GAUGE_TEMPERATUUR, DsType.GAUGE, /* TODO: heartbeat in seconds*/ 0, /* TODO: minimum accdeptable value */ 0, /* TODO: maximum acceptable value */ 0);
        // TODO: add other datasources
        // Keep all data for 1440 rows (= 1440 minutes = 24 hours = 1 day); half the data points must be valid
        rrdDef.addArchive(AVERAGE, 0, 1, 24 * 60);
        // Keep the aggregates of 5 minutes for 1 week
        rrdDef.addArchive(AVERAGE, 0.5, 5, 24 * 7 * (60 / 5));
        // Keep the aggregates of one hour for a year
        rrdDef.addArchive(AVERAGE, 0.5, 60, 366 * 24 * 7);
        return rrdDef;
    }

    private void checkRrdFile() {
        File rrdFile = new File(rrdDef.getPath());
        LOG.debug("checkRrdFile: " + rrdFile.getAbsolutePath());
        if (rrdFile.exists()) {
            LOG.info("RRD file exists");
        } else {
            LOG.warn("RRD file does not exist");
            try {
                RrdDb db = new RrdDb(rrdDef);
                db.close();
            } catch (IOException e) {
                LOG.fatal("Unable to create RRD file " + rrdFile.getAbsolutePath(), e);
                throw new IllegalStateException("RRD file could not be created");
            }
        }
    }
}
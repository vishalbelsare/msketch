package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanation;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.GlobalRatioMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.SupportMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.AttributeEncoder;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Summarizer that works over both cube and row-based labeled ratio-based
 * outlier summarization.
 */
public class APLOutlierSummarizer extends APLSummarizer {
    private Logger log = LoggerFactory.getLogger("APLOutlierSummarizer");
    private String countColumn = null;
    private macrobase.APrioriLinear aplKernel;
    private boolean useSupport;
    private boolean useGlobalRatio;

    public long aplTime = 0;
    public long mergeTime = 0;
    public long queryTime = 0;

    public List<Integer> o1results;
    public List<int[]> encoded;
    public AttributeEncoder encoder;

    @Override
    public List<String> getAggregateNames() {
        return Arrays.asList("Outliers", "Count");
    }

    @Override
    public double[][] getAggregateColumns(DataFrame input) {
        double[] outlierCol = input.getDoubleColumnByName(outlierColumn);
        double[] countCol = processCountCol(input, countColumn,  outlierCol.length);

        double[][] aggregateColumns = new double[2][];
        aggregateColumns[0] = outlierCol;
        aggregateColumns[1] = countCol;

        return aggregateColumns;
    }

    @Override
    public List<QualityMetric> getQualityMetricList() {
        List<QualityMetric> qualityMetricList = new ArrayList<>();
        if (useSupport) {
            qualityMetricList.add(
                    new SupportMetric(0)
            );
        }
        if (useGlobalRatio) {
            qualityMetricList.add(
                    new GlobalRatioMetric(0, 1)
            );
        }
        return qualityMetricList;
    }

    @Override
    public List<Double> getThresholds() {
        List<Double> thresholds = new ArrayList<>();
        if (useSupport) {
            thresholds.add(minOutlierSupport);
        } else {
            thresholds.add(minRatioMetric);
        }
        return thresholds;
    }

    @Override
    public double getNumberOutliers(double[][] aggregates) {
        double count = 0.0;
        double[] outlierCount = aggregates[0];
        for (int i = 0; i < outlierCount.length; i++) {
            count += outlierCount[i];
        }
        return count;
    }

    public void process(DataFrame input) throws Exception {
        encoder = new AttributeEncoder();
        encoder.setColumnNames(attributes);
        long startTime = System.currentTimeMillis();
        encoded = encoder.encodeAttributes(
                input.getStringColsByName(attributes)
        );
        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("Encoded in: {}", elapsed);
        log.debug("Encoded Categories: {}", encoder.getNextKey());

        thresholds = getThresholds();
        qualityMetricList = getQualityMetricList();
        aplKernel = new APrioriLinear(
                qualityMetricList,
                thresholds
        );
        aplKernel.setDoContainment(doContainment);

        double[][] aggregateColumns = getAggregateColumns(input);
        List<String> aggregateNames = getAggregateNames();
        long start = System.nanoTime();
        List<APLExplanationResult> aplResults = aplKernel.explain(encoded, aggregateColumns, encoder.getNextKey());
        aplTime += System.nanoTime() - start;
        mergeTime += aplKernel.mergeTime;
        queryTime += aplKernel.queryTime;
        System.out.format("APL %f, merge %f, query %f\n", (System.nanoTime() - start)/1.e9, aplKernel.mergeTime/1.e9, aplKernel.queryTime/1.e9);
        o1results = aplKernel.o1results;
        numOutliers = (long)getNumberOutliers(aggregateColumns);

        explanation = new APLExplanation(
                encoder,
                numEvents,
                numOutliers,
                aggregateNames,
                qualityMetricList,
                thresholds,
                aplResults
        );
    }

    public String getCountColumn() {
        return countColumn;
    }
    public void setCountColumn(String countColumn) {
        this.countColumn = countColumn;
    }
    public double getMinRatioMetric() {
        return minRatioMetric;
    }
    public void setUseSupport(boolean useSupport) { this.useSupport = useSupport; }
    public void setUseGlobalRatio(boolean useGlobalRatio) { this.useGlobalRatio = useGlobalRatio; }

    public void resetTime() {
        this.aplTime = 0;
        this.mergeTime = 0;
        this.queryTime = 0;
    }
}
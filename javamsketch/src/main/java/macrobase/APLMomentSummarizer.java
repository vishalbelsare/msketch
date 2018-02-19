package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanation;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APrioriLinear;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.AttributeEncoder;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Summarizer that works over both cube and row-based labeled ratio-based
 * outlier summarization.
 */
public class APLMomentSummarizer extends APLSummarizer {
    private Logger log = LoggerFactory.getLogger("APLMomentSummarizer");
    private String minColumn = null;
    private String maxColumn = null;
    private String logMinColumn = "lmin";
    private String logMaxColumn = "lmax";
    private List<String> momentColumns;
    private List<String> logMomentColumns;
    private double percentile;
    private boolean useCascade = false;
    private boolean[] useStages;
    private boolean verbose;

    @Override
    public List<String> getAggregateNames() {
        ArrayList<String> aggregateNames = new ArrayList<>();
        aggregateNames.add("Minimum");
        aggregateNames.add("Maximum");
        aggregateNames.add("Log Minimum");
        aggregateNames.add("Log Maximum");
        aggregateNames.addAll(momentColumns);
        aggregateNames.addAll(logMomentColumns);
        return aggregateNames;
    }

    @Override
    public double[][] getAggregateColumns(DataFrame input) {
        double[][] aggregateColumns = new double[4+momentColumns.size()+logMomentColumns.size()][];
        int curCol = 0;
        aggregateColumns[curCol++] = input.getDoubleColumnByName(minColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(maxColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(logMinColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(logMaxColumn);
        for (int i = 0; i < momentColumns.size(); i++) {
            aggregateColumns[curCol++] = input.getDoubleColumnByName(momentColumns.get(i));
        }
        for (int i = 0; i < logMomentColumns.size(); i++) {
            aggregateColumns[curCol++] = input.getDoubleColumnByName(logMomentColumns.get(i));
        }

        processCountCol(input, momentColumns.get(0), aggregateColumns[0].length);
        return aggregateColumns;
    }

    @Override
    public Map<String, int[]> getAggregationOps() {
        Map<String, int[]> aggregationOps = new HashMap<>();
        aggregationOps.put("add", IntStream.range(4, 4+momentColumns.size()+logMomentColumns.size()).toArray());
        aggregationOps.put("min", new int[]{0, 2});
        aggregationOps.put("max", new int[]{1, 3});
        return aggregationOps;
    }

    @Override
    public List<QualityMetric> getQualityMetricList() {
        List<QualityMetric> qualityMetricList = new ArrayList<>();
        if (useCascade) {
            EstimatedSupportMetric metric = new EstimatedSupportMetric(0, 1, 2, 3, 4, 4+momentColumns.size(),
                    (100.0 - percentile) / 100.0, 1e-4, true);
            metric.setCascadeStages(useStages);
            metric.setVerbose(verbose);
            qualityMetricList.add(metric);
        } else {
            qualityMetricList.add(
                    new EstimatedSupportMetric(0, 1, 2, 3, 4, 4+momentColumns.size(),
                            (100.0 - percentile) / 100.0, 1e-4, false)
            );
        }
        return qualityMetricList;
    }

    @Override
    public List<Double> getThresholds() {
        return Arrays.asList(minOutlierSupport);
    }

    @Override
    public double getNumberOutliers(double[][] aggregates) {
        double count = 0.0;
        double[] counts = aggregates[4];
        for (int i = 0; i < counts.length; i++) {
            count += counts[i];
        }
        return count * percentile / 100.0;
    }

    public String getMinColumn() {
        return minColumn;
    }
    public void setMinColumn(String minColumn) {
        this.minColumn = minColumn;
    }
    public String getMaxColumn() {
        return maxColumn;
    }
    public void setMaxColumn(String maxColumn) {
        this.maxColumn = maxColumn;
    }
    public List<String> getMomentColumns() {
        return momentColumns;
    }
    public void setMomentColumns(List<String> momentColumns) {
        this.momentColumns = momentColumns;
    }
    public void setLogMomentColumns(List<String> logMomentColumns) {
        this.logMomentColumns = logMomentColumns;
    }
    public void setPercentile(double percentile) {
        this.percentile = percentile;
    }
    public void setCascade(boolean useCascade) { this.useCascade = useCascade; }
    public double getMinRatioMetric() {
        return minRatioMetric;
    }
    public void setUseStages(boolean[] useStages) { this.useStages = useStages; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
}
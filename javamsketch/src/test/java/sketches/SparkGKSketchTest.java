package sketches;

import data.TestDataSource;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SparkGKSketchTest {
    @Test
    public void testSimple() throws Exception {
        SparkGKSketch sketch = new SparkGKSketch();
        int size = 100;
        sketch.setSizeParam(100);
        sketch.initialize();

        int n = 20000;
        double[] data = TestDataSource.getUniform(n+1);
        sketch.add(data);

        List<Double> ps = Arrays.asList(.1, .5, .9);
        double[] qs = sketch.getQuantiles(ps);

        double[] expectedQs = QuantileUtil.getTrueQuantiles(ps, data);

        assertArrayEquals(expectedQs, qs, n/size);
    }

}
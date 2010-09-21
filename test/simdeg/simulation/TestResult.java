package simdeg.simulation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test that results equality functions are corrects.
 */
public class TestResult {

    private static CollusionGroup buildCollusionGroup(double probability) {
        CollusionGroup collusionGroup = new CollusionGroup(probability);
        collusionGroup.add(new Worker());
        collusionGroup.add(new Worker());
        return collusionGroup;
    }

    private static InterCollusionGroup buildInterCollusionGroup(double probability) {
        InterCollusionGroup interCollusionGroup = new InterCollusionGroup(probability);
        interCollusionGroup.add(buildCollusionGroup(0.0d));
        interCollusionGroup.add(buildCollusionGroup(0.0d));
        return interCollusionGroup;
    }

    @Test
    public void correctResult() {
        Result result1 = Result.getCorrectResult();
        Result result2 = Result.getCorrectResult();
        assertTrue("All correct results are equals", result1.equals(result1));
        assertTrue("All correct results are equals", result1.equals(result2));
    }

    @Test
    public void failedResult() {
        Result result1 = Result.getFailedResult();
        Result result2 = Result.getFailedResult();
        Result result3 = Result.getCorrectResult();
        assertFalse("Failed results are unique", result1.equals(result2));
        assertFalse("Failed results are unique", result1.equals(result3));
    }

    @Test
    public void colludedResult() {
        CollusionGroup group = buildCollusionGroup(0.1d);
        Result result1 = Result.getColludedResult(group);
        Result result2 = Result.getColludedResult(group);
        Result result3 = Result.getColludedResult(buildCollusionGroup(0.1d));
        assertTrue("Colluding results are equals", result1.equals(result2));
        assertFalse("Distinct colluding results are not equal", result1.equals(result3));
    }

    @Test
    public void interColludedResult() {
        InterCollusionGroup group = buildInterCollusionGroup(0.5d);
        Result result1 = Result.getInterColludedResult(group);
        Result result2 = Result.getInterColludedResult(group);
        Result result3 = Result.getInterColludedResult(buildInterCollusionGroup(0.5d));
        assertTrue("Colluding results are equals", result1.equals(result2));
        assertFalse("Distinct colluding results are not equal", result1.equals(result3));
    }

}

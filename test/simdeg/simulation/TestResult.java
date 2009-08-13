package simdeg.simulation;

import static simdeg.simulation.Result.ResultType.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestResult {

    @Test public void result() {
        new Result(CORRECT);
        new Result(ATTACK);
        new Result(FAILED, (CollusionGroup)null);
        new Result(BUG, false);
    }

    @Test public void equals() {
        Result result1 = new Result(BUG);
        Result result2 = new Result(BUG);
        Result result3 = new Result(FAILED);
        Result result4 = new Result(FAILED);
        Result result5 = new Result(CORRECT);
        assertTrue("A result is equal to itself", result1.equals(result1));
        assertTrue("Colluding results are equal", result1.equals(result2));
        assertFalse("Failed results are unique", result1.equals(result3));
        assertFalse("Distinct results are different", result1.equals(result5));
        assertTrue("In both direction", result3.equals(result3));
        assertFalse("Unicity again", result3.equals(result4));
        assertFalse("Again", result3.equals(result5));
    }

}

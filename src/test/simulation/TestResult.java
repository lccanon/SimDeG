package simdeg.simulation;

import static simdeg.simulation.Result.ResultType.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestResult {

    @Test public void result() {
        new Result(ATTACK);
        new Result(FAILED, (CollusionGroup)null);
        new Result(BUG, false);
    }

    @Test public void equals() {
        Result result1 = new Result(ATTACK);
        Result result2 = new Result(ATTACK);
        Result result3 = new Result(FAILED);
        Result result4 = new Result(FAILED);
        Result result5 = new Result(CORRECT);
        assertTrue(result1.equals(result1));
        assertTrue(result1.equals(result2));
        assertFalse(result1.equals(result3));
        assertFalse(result1.equals(result5));
        assertTrue(result3.equals(result3));
        assertFalse(result3.equals(result4));
        assertFalse(result3.equals(result5));
    }

}

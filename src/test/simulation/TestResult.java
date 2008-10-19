package simdeg.simulation;

import static simdeg.simulation.Result.ResultType.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestResult {

    @Test public void answer() {
        new Result(ATTACK);
        new Result(FAILED, (CollusionGroup)null);
        new Result(BUG, false);
    }

    @Test public void equals() {
        Result answer1 = new Result(ATTACK);
        Result answer2 = new Result(ATTACK);
        Result answer3 = new Result(FAILED);
        Result answer4 = new Result(FAILED);
        Result answer5 = new Result(CORRECT);
        assertTrue(answer1.equals(answer1));
        assertTrue(answer1.equals(answer2));
        assertFalse(answer1.equals(answer3));
        assertFalse(answer1.equals(answer5));
        assertTrue(answer3.equals(answer3));
        assertFalse(answer3.equals(answer4));
        assertFalse(answer3.equals(answer5));
    }

}

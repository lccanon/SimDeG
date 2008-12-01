package simdeg.simulation;

import org.junit.Test;

import simdeg.util.OutOfRangeException;

public class TestCollusionGroup {

    @Test(expected=OutOfRangeException.class)
    public void collusionGroupException() {
        new CollusionGroup(1.1d, "");
    }

    @Test(timeout=100)
    public void isColluding() {
        CollusionGroup group = new CollusionGroup(0.5d, "");
        while(group.isColluding());
        while(!group.isColluding());
    }

}

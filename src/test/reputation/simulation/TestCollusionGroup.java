package simdeg.reputation.simulation;

import org.junit.Test;

public class TestCollusionGroup {

    @Test(expected=IllegalArgumentException.class)
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

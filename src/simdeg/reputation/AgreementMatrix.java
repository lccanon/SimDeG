package simdeg.reputation;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Logger;

import simdeg.util.RV;
import simdeg.util.Estimator;
import simdeg.util.DynamicMatrix;
import simdeg.util.OutOfRangeException;

//class AgreementMatrix extends DynamicMatrix<Worker> {
public class AgreementMatrix extends DynamicMatrix<Worker> {

    /** Logger */
    private static final Logger logger
        = Logger.getLogger(AgreementMatrix.class.getName());

    protected AgreementMatrix(Estimator estimatorBase) {
        super(estimatorBase);
        /* Test wether the default estimator was correctly initialized */
        if (estimatorBase.getMean() < 1.0d - estimatorBase.DEFAULT_ERROR
                || estimatorBase.getMean() > 1.0d)
            throw new OutOfRangeException(estimatorBase.getMean(),
                    1.0d - estimatorBase.DEFAULT_ERROR, 1.0d);
    }

    protected final void increaseAgreement(Worker worker, Worker otherWorker) {
        final Set<Worker> set1 = getSet(worker);
        final Set<Worker> set2 = getSet(otherWorker);
        if (set1 == null || set2 == null)
            return;
        /* Update estimator */
        getEstimator(set1, set2).setSample(1.0d);
        /* Test the possibility of merging both sets */
        if (set1 != set2 && getEstimator(set1, set2).getMean()
                > 1.0d - 1.0d / (2.0d + set1.size() + set2.size())
                && getEstimator(set1, set2).getMean()
                > 1.0d - getEstimator(set1, set2).getError())
            merge(set1, set2);
    }

    protected final void decreaseAgreement(Worker worker, Worker otherWorker) {
        Set<Worker> set1 = getSet(worker);
        Set<Worker> set2 = getSet(otherWorker);
        if (set1 == null || set2 == null || worker == otherWorker)
            return;
        /* Test the possibility of splitting the current set */
        if (set1 == set2) {
            split(getSet(worker), worker);
            split(getSet(otherWorker), otherWorker);
            set1 = getSet(worker);
            set2 = getSet(otherWorker);
        }
        /* Update estimator */
        getEstimator(set1, set2).setSample(0.0d);
    }

    @SuppressWarnings("unchecked")
    protected final RV[][] getAgreements(Set<? extends Worker> workers) {
        final List<Set<Worker>> sets = new ArrayList<Set<Worker>>(getSets(workers));
        RV[][] result = new RV[sets.size()][sets.size()];
        for (int j=0; j<result[0].length; j++)
            result[0][j] = getEstimator(getBiggest(), sets.get(j));
        for (int i=1; i<result.length; i++)
            for (int j=0; j<result[i].length; j++)
                result[i][j] = getEstimator(sets.get(i-1), sets.get(j));
        return result;
    }

}

package simdeg.reputation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import simdeg.util.DynamicMatrix;
import simdeg.util.Estimator;
import simdeg.util.RV;

class AgreementMatrix<W extends Worker> extends DynamicMatrix<W> {

//    /** Logger */
//    private static final Logger logger
//        = Logger.getLogger(AgreementMatrix.class.getName());

	protected AgreementMatrix(Estimator estimatorBase) {
		super(estimatorBase);
	}

	protected final void increaseAgreement(W worker, W otherWorker) {
		final Set<W> set1 = getSet(worker);
		final Set<W> set2 = getSet(otherWorker);
		if (set1 == null || set2 == null)
			return;
		/* Update estimator */
		getEstimator(set1, set2).setSample(1.0d);
		/* Test the possibility of merging both sets */
		if (set1 != set2
				&& getEstimator(set1, set2).getSampleCount() > set1.size()
						+ set2.size()
				&& getEstimator(set1, set2).getSampleCount(0.0d) == 0.0d)
			merge(set1, set2);
	}

	protected final void decreaseAgreement(W worker, W otherWorker) {
		Set<W> set1 = getSet(worker);
		Set<W> set2 = getSet(otherWorker);
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

	protected final RV[][] getAgreements(Set<W> workers) {
		final List<Set<W>> sets = new ArrayList<Set<W>>(getSets(workers));
		RV[][] result = new RV[sets.size()][sets.size()];
		for (int j = 0; j < result[0].length; j++)
			result[0][j] = getEstimator(getLargest(), sets.get(j));
		for (int i = 1; i < result.length; i++)
			for (int j = 0; j < result[i].length; j++)
				result[i][j] = getEstimator(sets.get(i - 1), sets.get(j));
		return result;
	}

	protected final int countAgreerMajority() {
		final Set<W> largest = getLargest();
		int count = 0;
		for (Set<W> set : getSets(getAll()))
			if (getEstimator(set, largest).getSampleCount() > set.size()
					&& getEstimator(set, largest).getSampleCount(0.0d) == 0.0d)
				count += set.size();
		return count;
	}

}
package simdeg.scheduling;


import java.util.Set;
import java.util.HashSet;

import simdeg.reputation.Worker;
import simdeg.util.Collections;
import simdeg.util.RandomManager;

/**
 * Basic implementation of the ResourcesGrouper process which create groups of one
 * worker.
 */
public class SparingResourcesGrouper extends ResourcesGrouper {

   /**
    * Gets a group containing the worker given in argument.
    */
    Set<Worker> getGroup(Worker worker) {
        return Collections.addElement(worker, new HashSet<Worker>());
    }

    /**
     * Gets an extension of the group of workers given in argument (in case
     * of results selection failure). Return null if all the known workers
     * are already in the initial group.
     */
    Set<Worker> getGroupExtension(Set<Worker> workers) {
        if (workers.size() == this.workers.size())
            return null;
        Set<Worker> currentWorkers = new HashSet<Worker>(this.workers);
        currentWorkers.removeAll(workers);
        return Collections.getRandomSubGroup(1, currentWorkers,
                RandomManager.getRandom("scheduling"));
    }

}

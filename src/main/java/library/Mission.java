package library;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Mission {
    private final String name;
    private MissionStatus status;
    private final Set<Rocket> assignedRockets;

    public Mission(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Mission name cannot be null or blank");
        }
        this.name = name;
        this.status = MissionStatus.SCHEDULED;
        this.assignedRockets = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public MissionStatus getStatus() {
        return status;
    }

    void assignRocket(Rocket rocket) {
        if (rocket == null) {
            throw new IllegalArgumentException("Cannot assign null rocket");
        }
        if (this.status == MissionStatus.ENDED) {
            throw new IllegalStateException("Cannot assign rockets to an ENDED mission.");
        }
        assignedRockets.add(rocket);
    }

    void unassignAllRockets() {
        this.assignedRockets.clear();
    }

    public int getRocketCount() {
        return assignedRockets.size();
    }

    public Set<Rocket> getAssignedRockets() {
        return Collections.unmodifiableSet(assignedRockets);
    }

    void setStatus(MissionStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (this.status == MissionStatus.ENDED) {
            throw new IllegalStateException("Cannot change status of an ENDED mission.");
        }
        this.status = newStatus;
    }
}

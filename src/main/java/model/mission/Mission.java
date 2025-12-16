package model.mission;

import model.rocket.Rocket;

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

    public void assignRocket(Rocket rocket) {
        if (rocket == null) {
            throw new IllegalArgumentException("Cannot assign null rocket");
        }
        if (this.status == MissionStatus.ENDED) {
            throw new IllegalStateException("Cannot assign rockets to an ENDED mission.");
        }
        assignedRockets.add(rocket);
    }

    public void unassignAllRockets() {
        this.assignedRockets.clear();
    }

    public int getRocketCount() {
        return assignedRockets.size();
    }

    public Set<Rocket> getAssignedRockets() {
        return Collections.unmodifiableSet(assignedRockets);
    }

    public void setStatus(MissionStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (this.status == MissionStatus.ENDED) {
            throw new IllegalStateException("Cannot change status of an ENDED mission.");
        }
        this.status = newStatus;
    }

    @Override
    public String toString() {
        return "Mission{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", rockets=" + assignedRockets.size() +
                '}';
    }
}

package library;

public class Rocket {
    private final String name;
    private RocketStatus status;
    private String missionName;

    public Rocket(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Rocket name cannot be null or blank");
        }
        this.name = name;
        this.status = RocketStatus.ON_GROUND;
        this.missionName = null;
    }

    public String getName() {
        return name;
    }

    public RocketStatus getStatus() {
        return status;
    }

    public String getMissionName() {
        return missionName;
    }

    void assignToMission(String missionName) {
        if (missionName == null || missionName.isBlank()) {
            throw new IllegalArgumentException("Mission name cannot be null");
        }

        if (this.missionName != null) {
            throw new IllegalStateException("Rocket is already assigned to mission: " + this.missionName);
        }

        this.missionName = missionName;
        this.status = RocketStatus.IN_SPACE;
    }

    void unassign() {
        this.missionName = null;
        this.status = RocketStatus.ON_GROUND;
    }

    void setStatus(RocketStatus newStatus) {
        if (newStatus == RocketStatus.ON_GROUND && this.missionName != null) {
            throw new IllegalStateException("Cannot set to ON_GROUND while assigned to a mission.");
        }
        this.status = newStatus;
    }

}

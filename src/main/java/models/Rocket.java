package models;

public class Rocket {
    private String name;
    private RocketStatus status;
    private Mission mission;

    public Rocket(String name, RocketStatus status, Mission mission) {
        this.name = name;
        this.mission = mission;
        if (status == null) {
            this.status = RocketStatus.ON_GROUND;
        } else {
            this.status = status;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RocketStatus getStatus() {
        return status;
    }

    public void setStatus(RocketStatus status) {
        this.status = status;
    }

    public Mission getMission() {
        return mission;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }
}

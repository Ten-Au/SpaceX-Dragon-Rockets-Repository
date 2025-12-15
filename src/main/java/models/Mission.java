package models;

public class Mission {
    private String name;
    private MissionStatus status;

    public Mission(String name, MissionStatus status) {
        this.name = name;
        if (status == null) {
            this.status = MissionStatus.SCHEDULED;
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

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

}

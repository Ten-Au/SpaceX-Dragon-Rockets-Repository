package repository;

import model.mission.Mission;
import model.mission.MissionStatus;
import model.rocket.Rocket;
import model.rocket.RocketStatus;

import java.util.Set;

public interface SpaceXRepository {
    void addRocket(Rocket rocket);
    void addMission(Mission mission);

    void assignRocketToMission(String rocketName, String missionName);
    void assignRocketsToMission(String missionName, Set<String> rocketNames);

    void changeRocketStatus(String rocketName, RocketStatus newStatus);
    void changeMissionStatus(String missionName, MissionStatus newStatus);

    String getSummary();
}

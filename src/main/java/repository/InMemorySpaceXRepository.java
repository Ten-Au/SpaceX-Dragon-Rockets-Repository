package repository;

import model.mission.Mission;
import model.mission.MissionStatus;
import model.rocket.Rocket;
import model.rocket.RocketStatus;

import java.util.*;

public class InMemorySpaceXRepository implements SpaceXRepository {
    private final Map<String, Rocket> rockets = new HashMap<>();
    private final Map<String, Mission> missions = new HashMap<>();

    @Override
    public synchronized void addRocket(Rocket rocket) {
        if (rocket == null) throw new IllegalArgumentException("Rocket cannot be null");
        if (rockets.containsKey(rocket.getName())) {
            throw new IllegalArgumentException("Rocket " + rocket.getName() + " already exists.");
        }
        rockets.put(rocket.getName(), rocket);
    }

    @Override
    public synchronized void addMission(Mission mission) {
        if (mission == null) throw new IllegalArgumentException("Mission cannot be null");
        if (missions.containsKey(mission.getName())) {
            throw new IllegalArgumentException("Mission " + mission.getName() + " already exists.");
        }
        missions.put(mission.getName(), mission);
    }

    @Override
    public synchronized void assignRocketToMission(String rocketName, String missionName) {
        Rocket rocket = getRocketOrThrow(rocketName);
        Mission mission = getMissionOrThrow(missionName);

        rocket.assignToMission(missionName);

        mission.assignRocket(rocket);

        updateMissionStatusAuto(mission);
    }

    @Override
    public synchronized void assignRocketsToMission(String missionName, Set<String> rocketNames) {
        if (rocketNames == null || rocketNames.isEmpty()) {
            return;
        }

        Mission mission = getMissionOrThrow(missionName);

        List<Rocket> rocketsToAssign = new ArrayList<>();

        for (String rocketName : rocketNames) {
            Rocket rocket = getRocketOrThrow(rocketName);
            if (rocket.getMissionName() != null) {
                throw new IllegalStateException(
                        String.format("Transaction failed: Rocket '%s' is already assigned to mission '%s'.",
                                rocket.getName(), rocket.getMissionName())
                );
            }
            rocketsToAssign.add(rocket);
        }

        for (Rocket rocket : rocketsToAssign) {
            rocket.assignToMission(missionName);
            mission.assignRocket(rocket);
        }

        updateMissionStatusAuto(mission);
    }

    @Override
    public synchronized void changeRocketStatus(String rocketName, RocketStatus newStatus) {
        Rocket rocket = getRocketOrThrow(rocketName);

        rocket.setStatus(newStatus);

        if (rocket.getMissionName() != null) {
            Mission mission = missions.get(rocket.getMissionName());
            updateMissionStatusAuto(mission);
        }
    }

    @Override
    public synchronized void changeMissionStatus(String missionName, MissionStatus newStatus) {
        Mission mission = getMissionOrThrow(missionName);

        if (newStatus == MissionStatus.ENDED) {
            for (Rocket rocket : mission.getAssignedRockets()) {
                rocket.unassign();
            }
            mission.unassignAllRockets();
            mission.setStatus(MissionStatus.ENDED);
            return;
        }

        validateManualStatusChange(mission, newStatus);
        mission.setStatus(newStatus);
    }

    @Override
    public synchronized String getSummary() {
        List<Mission> sortedMissions = missions.values().stream()
                .sorted(Comparator
                        .comparingInt(Mission::getRocketCount).reversed()
                        .thenComparing(Comparator.comparing(Mission::getName).reversed())
                )
                .toList();

        StringBuilder sb = getStringBuilder(sortedMissions);
        return sb.toString();
    }

    private StringBuilder getStringBuilder(List<Mission> sortedMissions) {
        StringBuilder sb = new StringBuilder();

        for (Mission mission : sortedMissions) {
            sb.append(String.format("• %s - %s - Dragons: %d\n",
                    mission.getName(),
                    toPrettyString(mission.getStatus()),
                    mission.getRocketCount()));

            for (Rocket rocket : mission.getAssignedRockets()) {
                sb.append(String.format("o %s - %s\n",
                        rocket.getName(),
                        toPrettyString(rocket.getStatus())));
            }
        }
        return sb;
    }

    @Override
    public synchronized Optional<Rocket> findRocket(String name) {
        return Optional.ofNullable(rockets.get(name));
    }

    @Override
    public synchronized Optional<Mission> findMission(String name) {
        return Optional.ofNullable(missions.get(name));
    }

    private Rocket getRocketOrThrow(String name) {
        if (!rockets.containsKey(name)) throw new IllegalArgumentException("Rocket not found: " + name);
        return rockets.get(name);
    }

    private Mission getMissionOrThrow(String name) {
        if (!missions.containsKey(name)) throw new IllegalArgumentException("Mission not found: " + name);
        return missions.get(name);
    }

    private void updateMissionStatusAuto(Mission mission) {
        if (mission.getStatus() == MissionStatus.ENDED) return;

        Set<Rocket> assigned = mission.getAssignedRockets();

        if (assigned.isEmpty()) {
            mission.setStatus(MissionStatus.SCHEDULED);
            return;
        }

        boolean anyInRepair = assigned.stream()
                .anyMatch(r -> r.getStatus() == RocketStatus.IN_REPAIR);

        if (anyInRepair) {
            mission.setStatus(MissionStatus.PENDING);
        } else {
            mission.setStatus(MissionStatus.IN_PROGRESS);
        }
    }

    private void validateManualStatusChange(Mission mission, MissionStatus newStatus) {
        Set<Rocket> assigned = mission.getAssignedRockets();

        switch (newStatus) {
            case SCHEDULED:
                if (!assigned.isEmpty()) {
                    throw new IllegalStateException("Cannot revert to SCHEDULED. Rockets are assigned.");
                }
                break;

            case PENDING:
                boolean hasRepair = assigned.stream().anyMatch(r -> r.getStatus() == RocketStatus.IN_REPAIR);
                if (assigned.isEmpty() || !hasRepair) {
                    throw new IllegalStateException("Cannot set to PENDING. Requires at least one assigned rocket to be IN_REPAIR.");
                }
                break;

            case IN_PROGRESS:
                boolean anyInRepair = assigned.stream().anyMatch(r -> r.getStatus() == RocketStatus.IN_REPAIR);
                if (assigned.isEmpty()) {
                    throw new IllegalStateException("Cannot set to IN_PROGRESS. No rockets assigned.");
                }
                if (anyInRepair) {
                    throw new IllegalStateException("Cannot set to IN_PROGRESS. One or more rockets are IN_REPAIR.");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported status: " + newStatus);
        }
    }

    private String toPrettyString(Enum<?> status) {
        String name = status.name();
        return switch (name) {
            case "ON_GROUND" -> "On ground";
            case "IN_SPACE" -> "In space";
            case "IN_REPAIR" -> "In repair";
            case "IN_PROGRESS" -> "In progress";
            case "SCHEDULED" -> "Scheduled";
            case "PENDING" -> "Pending";
            case "ENDED" -> "Ended";
            default -> name;
        };
    }
}

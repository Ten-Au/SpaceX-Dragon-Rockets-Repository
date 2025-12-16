import model.mission.Mission;
import model.mission.MissionStatus;
import model.rocket.Rocket;
import model.rocket.RocketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemorySpaceXRepository;
import repository.SpaceXRepository;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;

@DisplayName("SpaceX Repository Library Tests")
class SpaceXRepositoryTest {

    private SpaceXRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemorySpaceXRepository();
    }

    @Test
    @DisplayName("Should successfully add a new rocket and set initial status to 'On ground'")
    void givenNewRocket_whenAddRocket_thenSuccess() {
        Rocket r = new Rocket("Falcon 9");
        repository.addRocket(r);

        assertTrue(repository.getSummary().contains("Falcon 9 - On ground"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding a rocket with a duplicate name")
    void givenRocketAlreadyExists_whenAddRocket_thenThrowException() {
        repository.addRocket(new Rocket("Falcon 9"));
        assertThrows(IllegalArgumentException.class, () -> {
            repository.addRocket(new Rocket("Falcon 9"));
        }, "Adding a rocket with an existing name should fail");
    }

    @Test
    @DisplayName("Should successfully add a new mission and set initial status to 'Scheduled'")
    void givenNewMission_whenAddMission_thenSuccessAndStatusScheduled() {
        Mission m = new Mission("Mars");
        repository.addMission(m);

        String summary = repository.getSummary();
        assertTrue(summary.contains("Mars"));
        assertTrue(summary.contains("Scheduled"));
    }

    @Test
    @DisplayName("Should update rocket status to 'In space' and mission to 'In progress' upon single assignment")
    void givenRocketAndMission_whenAssignRocketToMission_thenStatusInProgress() {
        Rocket r = new Rocket("Falcon 9");
        Mission m = new Mission("Mars");

        repository.addRocket(r);
        repository.addMission(m);

        repository.assignRocketToMission("Falcon 9", "Mars");

        String summary = repository.getSummary();
        assertTrue(summary.contains("In progress"));
        assertTrue(summary.contains("Falcon 9 - In space"));
        assertTrue(summary.contains("Dragons: 1"));
    }

    @Test
    @DisplayName("Should perform atomic bulk assignment: if one rocket is invalid, none are assigned")
    void givenInvalidRocketBatch_whenAssignRocketsToMission_thenTransactionFails() {
        repository.addMission(new Mission("MoonBase"));
        repository.addRocket(new Rocket("R1"));
        repository.addRocket(new Rocket("R2"));

        // R3 is missing from repo, so this batch is invalid
        Set<String> batch = Set.of("R1", "R2", "R3");

        assertThrows(IllegalArgumentException.class, () -> {
            repository.assignRocketsToMission("MoonBase", batch);
        });

        // Verify "Nothing" happened (All-or-Nothing transaction safety)
        String summary = repository.getSummary();
        assertTrue(summary.contains("Dragons: 0"), "Mission should have 0 dragons if bulk assignment failed");
    }

    @Test
    @DisplayName("Should auto-update mission status to 'Pending' when an assigned rocket goes to 'In repair'")
    void givenMissionWithAssignedRockets_whenRocketBreaks_thenMissionPending() {
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.addRocket(new Rocket("R2"));

        repository.assignRocketToMission("R1", "Mars");
        repository.assignRocketToMission("R2", "Mars");

        // Change one rocket to IN_REPAIR
        repository.changeRocketStatus("R1", RocketStatus.IN_REPAIR);

        String summary = repository.getSummary();
        assertTrue(summary.contains("Mars - Pending"));
        assertTrue(summary.contains("R1 - In repair"));
    }

    @Test
    @DisplayName("Should auto-update mission status to 'In Progress' when all broken rockets are fixed")
    void givenPendingMission_whenRocketFixed_thenMissionInProgress() {
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.assignRocketToMission("R1", "Mars");
        repository.changeRocketStatus("R1", RocketStatus.IN_REPAIR);

        repository.changeRocketStatus("R1", RocketStatus.IN_SPACE);

        String summary = repository.getSummary();
        assertTrue(summary.contains("Mars - In progress"));
    }

    @Test
    @DisplayName("Should prevent manual status change to 'In Progress' if a rocket is 'In repair'")
    void givenPendingMissionWithBrokenRocket_whenManualSetInProgress_thenThrowException() {
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.assignRocketToMission("R1", "Mars");
        repository.changeRocketStatus("R1", RocketStatus.IN_REPAIR);

        assertThrows(IllegalStateException.class, () -> {
            repository.changeMissionStatus("Mars", MissionStatus.IN_PROGRESS);
        }, "Should not allow In Progress status while rocket is in repair");
    }

    @Test
    @DisplayName("Should prevent manual status change to 'Scheduled' if rockets are assigned")
    void givenMissionWithRockets_whenManualSetScheduled_thenThrowException() {
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.assignRocketToMission("R1", "Mars");

        assertThrows(IllegalStateException.class, () -> {
            repository.changeMissionStatus("Mars", MissionStatus.SCHEDULED);
        }, "Should not allow Scheduled status while rockets are assigned");
    }

    @Test
    @DisplayName("Should release all rockets and reset mission count to 0 when mission is set to 'Ended'")
    void givenActiveMission_whenSetEnded_thenRocketsReleasedAndCountReset() {
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.addRocket(new Rocket("R2"));
        repository.assignRocketToMission("R1", "Mars");
        repository.assignRocketToMission("R2", "Mars");

        repository.changeMissionStatus("Mars", MissionStatus.ENDED);

        String summary = repository.getSummary();
        assertTrue(summary.contains("Mars - Ended - Dragons: 0"));

        repository.addMission(new Mission("Luna"));
        assertDoesNotThrow(() -> {
            repository.assignRocketToMission("R1", "Luna");
        }, "Released rocket should be assignable to a new mission");
    }

    @Test
    @DisplayName("Should prevent any modifications to a mission once it is 'Ended'")
    void givenEndedMission_whenModify_thenThrowException() {
        repository.addMission(new Mission("Mars"));
        repository.changeMissionStatus("Mars", MissionStatus.ENDED);

        repository.addRocket(new Rocket("R1"));
        assertThrows(IllegalStateException.class, () -> {
            repository.assignRocketToMission("R1", "Mars");
        }, "Should not allow assigning rockets to an Ended mission");

        assertThrows(IllegalStateException.class, () -> {
            repository.changeMissionStatus("Mars", MissionStatus.SCHEDULED);
        }, "Should not allow status changes on an Ended mission");
    }

    @Test
    @DisplayName("Should sort summary by rocket count (descending) then by name (descending alphabetical)")
    void givenMultipleMissions_whenGetSummary_thenSortedByRocketsAndName() {
        repository.addMission(new Mission("Alpha"));
        repository.addRocket(new Rocket("A1"));
        repository.addRocket(new Rocket("A2"));
        repository.assignRocketToMission("A1", "Alpha");
        repository.assignRocketToMission("A2", "Alpha");

        repository.addMission(new Mission("Beta"));
        repository.addRocket(new Rocket("B1"));
        repository.addRocket(new Rocket("B2"));
        repository.addRocket(new Rocket("B3"));
        repository.assignRocketToMission("B1", "Beta");
        repository.assignRocketToMission("B2", "Beta");
        repository.assignRocketToMission("B3", "Beta");

        repository.addMission(new Mission("Charlie"));
        repository.addRocket(new Rocket("C1"));
        repository.addRocket(new Rocket("C2"));
        repository.assignRocketToMission("C1", "Charlie");
        repository.assignRocketToMission("C2", "Charlie");

        String summary = repository.getSummary();

        int idxBeta = summary.indexOf("Beta");
        int idxCharlie = summary.indexOf("Charlie");
        int idxAlpha = summary.indexOf("Alpha");

        assertTrue(idxBeta < idxCharlie, "Beta (3) should be before Charlie (2)");
        assertTrue(idxCharlie < idxAlpha, "Charlie (2) should be before Alpha (2) due to alphabetical desc sort");
    }
}

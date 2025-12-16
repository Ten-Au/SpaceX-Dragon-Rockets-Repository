import library.Mission;
import library.MissionStatus;
import library.Rocket;
import library.RocketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import library.InMemorySpaceXRepository;
import library.SpaceXRepository;

import java.util.Optional;
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
    @DisplayName("Should successfully add a new rocket and find it with initial status 'On ground'")
    void givenNewRocket_whenAddRocket_thenFoundOnGround() {
        // Given
        Rocket r = new Rocket("Falcon 9");

        // When
        repository.addRocket(r);
        Optional<Rocket> found = repository.findRocket("Falcon 9");

        // Then
        assertTrue(found.isPresent(), "Rocket should be found");
        assertEquals(RocketStatus.ON_GROUND, found.get().getStatus(), "Initial status should be ON_GROUND");
    }

    @Test
    @DisplayName("Should return empty Optional when searching for a non-existent rocket")
    void givenNoRocket_whenFindRocket_thenEmpty() {
        // When
        Optional<Rocket> found = repository.findRocket("GhostRocket");

        // Then
        assertTrue(found.isEmpty(), "Should return empty Optional for non-existent rocket");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding a rocket with a duplicate name")
    void givenRocketAlreadyExists_whenAddRocket_thenThrowException() {
        // Given
        repository.addRocket(new Rocket("Falcon 9"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            repository.addRocket(new Rocket("Falcon 9"));
        }, "Adding a rocket with an existing name should fail");
    }

    @Test
    @DisplayName("Should successfully add a new mission and find it with initial status 'Scheduled'")
    void givenNewMission_whenAddMission_thenFoundScheduled() {
        // Given
        Mission m = new Mission("Mars");

        // When
        repository.addMission(m);
        Optional<Mission> found = repository.findMission("Mars");

        // Then
        assertTrue(found.isPresent(), "Mission should be found");
        assertEquals(MissionStatus.SCHEDULED, found.get().getStatus(), "Initial status should be SCHEDULED");
    }

    @Test
    @DisplayName("Should update rocket status to 'In space' and mission to 'In progress' upon single assignment")
    void givenRocketAndMission_whenAssignRocketToMission_thenStatusUpdated() {
        // Given
        repository.addRocket(new Rocket("Falcon 9"));
        repository.addMission(new Mission("Mars"));

        // When
        repository.assignRocketToMission("Falcon 9", "Mars");

        // Then
        Rocket rocket = repository.findRocket("Falcon 9").get();
        Mission mission = repository.findMission("Mars").get();
        
         assertEquals(RocketStatus.IN_SPACE, rocket.getStatus(), "Rocket should be IN_SPACE");
        assertEquals("Mars", rocket.getMissionName(), "Rocket should be linked to mission");
        
         assertEquals(MissionStatus.IN_PROGRESS, mission.getStatus(), "Mission should be IN_PROGRESS");
        assertEquals(1, mission.getRocketCount(), "Mission should have 1 rocket assigned");
    }

    @Test
    @DisplayName("Should perform atomic bulk assignment: if one rocket is invalid, none are assigned")
    void givenInvalidRocketBatch_whenAssignRocketsToMission_thenTransactionFails() {
        // Given
        repository.addMission(new Mission("MoonBase"));
        repository.addRocket(new Rocket("R1"));
        repository.addRocket(new Rocket("R2"));
        // "R3" is missing
        Set<String> batch = Set.of("R1", "R2", "R3");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            repository.assignRocketsToMission("MoonBase", batch);
        });

        // Verify "Nothing" happened (All-or-Nothing transaction safety)
        Mission mission = repository.findMission("MoonBase").get();
        assertEquals(0, mission.getRocketCount(), "Mission should have 0 dragons if bulk assignment failed");

        Rocket r1 = repository.findRocket("R1").get();
        assertEquals(RocketStatus.ON_GROUND, r1.getStatus(), "Rocket R1 should remain ON_GROUND");
    }

    @Test
    @DisplayName("Should auto-update mission status to 'Pending' when an assigned rocket goes to 'In repair'")
    void givenMissionWithAssignedRockets_whenRocketBreaks_thenMissionPending() {
        // Given
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.addRocket(new Rocket("R2"));
        repository.assignRocketsToMission("Mars", Set.of("R1", "R2"));

        // When
        repository.changeRocketStatus("R1", RocketStatus.IN_REPAIR);

        // Then
        Mission mission = repository.findMission("Mars").get();
        Rocket r1 = repository.findRocket("R1").get();
        
         assertEquals(MissionStatus.PENDING, mission.getStatus(), "Mission should be PENDING");
         assertEquals(RocketStatus.IN_REPAIR, r1.getStatus(), "Rocket should be IN_REPAIR");
    }

    @Test
    @DisplayName("Should auto-update mission status to 'In Progress' when all broken rockets are fixed")
    void givenPendingMission_whenRocketFixed_thenMissionInProgress() {
        // Given
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.assignRocketToMission("R1", "Mars");
        repository.changeRocketStatus("R1", RocketStatus.IN_REPAIR);

        // When
        repository.changeRocketStatus("R1", RocketStatus.IN_SPACE);

        // Then
        Mission mission = repository.findMission("Mars").get();
         assertEquals(MissionStatus.IN_PROGRESS, mission.getStatus(), "Mission should be IN_PROGRESS");
    }

    @Test
    @DisplayName("Should prevent manual status change to 'In Progress' if a rocket is 'In repair'")
    void givenPendingMissionWithBrokenRocket_whenManualSetInProgress_thenThrowException() {
        // Given
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.assignRocketToMission("R1", "Mars");
        repository.changeRocketStatus("R1", RocketStatus.IN_REPAIR);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            repository.changeMissionStatus("Mars", MissionStatus.IN_PROGRESS);
        }, "Should not allow In Progress status while rocket is in repair");
    }

    @Test
    @DisplayName("Should prevent manual status change to 'Scheduled' if rockets are assigned")
    void givenMissionWithRockets_whenManualSetScheduled_thenThrowException() {
        // Given
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.assignRocketToMission("R1", "Mars");

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            repository.changeMissionStatus("Mars", MissionStatus.SCHEDULED);
         }, "Should not allow Scheduled status while rockets are assigned");
    }

    @Test
    @DisplayName("Should release all rockets and reset mission count to 0 when mission is set to 'Ended'")
    void givenActiveMission_whenSetEnded_thenRocketsReleasedAndCountReset() {
        // Given
        repository.addMission(new Mission("Mars"));
        repository.addRocket(new Rocket("R1"));
        repository.addRocket(new Rocket("R2"));
        repository.assignRocketsToMission("Mars", Set.of("R1", "R2"));

        // Act: End the mission
        repository.changeMissionStatus("Mars", MissionStatus.ENDED);

        // Then
        Mission mission = repository.findMission("Mars").get();
        assertEquals(MissionStatus.ENDED, mission.getStatus());
         assertEquals(0, mission.getRocketCount(), "Ended mission should have 0 dragons");

        // Verify rockets are physically released
        Rocket r1 = repository.findRocket("R1").get();
        assertEquals(RocketStatus.ON_GROUND, r1.getStatus(), "Rocket should reset to ON_GROUND");
        assertNull(r1.getMissionName(), "Rocket should have no mission assigned");

        // Verify we can re-assign the released rocket
        repository.addMission(new Mission("Luna"));
        assertDoesNotThrow(() -> {
            repository.assignRocketToMission("R1", "Luna");
        });
    }

    @Test
    @DisplayName("Should prevent any modifications to a mission once it is 'Ended'")
    void givenEndedMission_whenModify_thenThrowException() {
        // Given
        repository.addMission(new Mission("Mars"));
        repository.changeMissionStatus("Mars", MissionStatus.ENDED);

        // When & Then
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
    void givenMissions_whenGetSummary_thenSortedByRocketsAndName() {
        // Given
        repository.addMission(new Mission("Alpha"));
        repository.addRocket(new Rocket("A1"));
        repository.addRocket(new Rocket("A2"));
        repository.assignRocketsToMission("Alpha", Set.of("A1", "A2"));
        
        repository.addMission(new Mission("Beta"));
        repository.addRocket(new Rocket("B1"));
        repository.addRocket(new Rocket("B2"));
        repository.addRocket(new Rocket("B3"));
        repository.assignRocketsToMission("Beta", Set.of("B1", "B2", "B3"));
        
        repository.addMission(new Mission("Charlie"));
        repository.addRocket(new Rocket("C1"));
        repository.addRocket(new Rocket("C2"));
        repository.assignRocketsToMission("Charlie", Set.of("C1", "C2"));

        // When
        String summary = repository.getSummary();

        // Then
        int idxBeta = summary.indexOf("Beta");
        int idxCharlie = summary.indexOf("Charlie");
        int idxAlpha = summary.indexOf("Alpha");

        assertTrue(idxBeta < idxCharlie, "Beta (3) should be before Charlie (2)");
        assertTrue(idxCharlie < idxAlpha, "Charlie (2) should be before Alpha (2) due to alphabetical desc sort");
    }
}
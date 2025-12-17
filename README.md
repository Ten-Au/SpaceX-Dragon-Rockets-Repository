# SpaceX Dragon Rockets Repository Library

A lightweight, thread-safe, in-memory library for managing the lifecycle of SpaceX rockets and missions.

## Overview

This library provides a repository to manage the state and associations of SpaceX Dragons (Rockets) and Missions. It enforces strict business rules regarding status synchronization—ensuring that the status of a mission automatically reflects the operational state of its assigned rockets.

## Features

* **Lifecycle Management:** Full CRUD capabilities for Rockets and Missions.
* **Automatic Status Synchronization:**
    * *In Repair* rockets automatically trigger *Pending* mission status.
    * *In Space* rockets automatically trigger *In Progress* mission status.
* **Transactional Integrity:** Atomic "All-or-Nothing" bulk assignment prevents partial data updates.
* **Thread Safety:** Thread-safe implementation suitable for concurrent environments.
* **Strict Encapsulation:** Prevents invalid state transitions (e.g., cannot modify "Ended" missions).

---

## Architecture & Design Decisions

This solution prioritizes simplicity and correctness, adhering to the "Keep it simple" guideline while ensuring robust OO design.



### 1. Unified Repository (The Aggregate Root)
* **Decision:** A single `SpaceXRepository` interface manages both `Rocket` and `Mission` entities instead of separate repositories.
* **Reasoning:** This allows the Repository to act as an **Aggregate Root**. It guarantees transactional consistency: a status change in a rocket immediately updates the mission within the same transaction, preventing invalid intermediate states that could occur if the repositories were decoupled without a complex service layer.

### 2. Packaging & Encapsulation
* **Decision:** All classes are grouped in a single feature package (`com.spacex.library`).
* **Reasoning:** This enables **package-private** visibility for sensitive lifecycle methods like `Rocket.unassign()` and `Mission.setStatus()`. This strictly prevents external consumers from bypassing the Repository API and corrupting data, forcing all interactions through the validated repository methods.

### 3. Concurrency Strategy
* **Decision:** The implementation uses `synchronized` methods for the repository to manage state.
* **Reasoning:** For a "simple" in-memory store required to be library-grade, a synchronized monitor is the most robust way to ensure atomicity and thread safety. It prevents cross-collection inconsistency (e.g., a rocket being assigned in one map but the mission not updating in the other) without the complexity of fine-grained locks.

### 4. "Ended" State as Terminal
* **Decision:** The `ENDED` status is treated as a strict terminal state.
* **Reasoning:** Setting a mission to `ENDED` triggers a cleanup routine that physically unassigns all rockets (resetting the count to 0), effectively making the mission immutable thereafter. This simplifies the lifecycle model by preventing "zombie" missions.

---

## AI Usage Declaration

Per the requirements, an LLM (Gemini) through GitHub Copilot was used as a **Thought Partner** and **Automation Tool** to assist my development process, while I retained full architectural control.

**How I used it:**
* **Design Validation:** Analyzed my initial Unified Repository design against the requirements to ensure it met the "Simple Implementation" criteria while maintaining SOLID principles.
* **Edge Case Discovery:** Searched for logical gaps in my status transition rules.
* **Test Generation:** Generated the boilerplate code for the `JUnit 5` test suite, allowing me to focus on verifying the complex status synchronization logic.
* **Code Generation:** Assisted with automatic code generation for repository logic.

---

## Future Improvements & Scalability

To scale this library to a larger distributed system, the following improvements would be considered:

1.  **Persistence Layer:** Replace the `HashMap` in-memory store with a persistent database (e.g., PostgreSQL).
2.  **Optimistic Locking:** Implement `version` fields and CAS (Compare-And-Swap) logic to improve concurrency throughput under high contention.
3.  **Event-Driven Architecture:** Decouple Rocket and Mission logic by publishing domain events (e.g., `RocketStatusChanged`).

---

## Usage

**Prerequisites:** Java 11+

```java
// 1. Initialize
SpaceXRepository repo = new InMemorySpaceXRepository();

// 2. Add Data
repo.addRocket(new Rocket("Falcon 9"));
repo.addMission(new Mission("Artemis I"));

// 3. Assign (Auto-updates statuses to 'In Space' / 'In Progress')
repo.assignRocketToMission("Falcon 9", "Artemis I");

// 4. Handle Anomalies (Auto-updates Mission to 'Pending')
repo.changeRocketStatus("Falcon 9", RocketStatus.IN_REPAIR);

// 5. Generate Report
System.out.println(repo.getSummary());

# Raga.io

- [Elvis Perlika](mailto:elvis.perlika@studio.unibo.it)
- [Eleonora Falconi](mailto:eleonora.falconi2@studio.unibo.it)

## AI Disclaimer

During the preparation of this work, the authors used Chat-GPT and Gemini to
refine the report by improving sentence structure and correcting grammatical errors.
After using these tools, the authors reviewed and edited the
content as needed and takes full responsibility for the content of the
final report/artifact.

## Abstract

<!-- Brief description of the project, its goals, and its achievements. -->

Agar.io is an online, massively multiplayer action game. Players take on the role of a small, circular cell inside a map that resembles a [Petri dish](https://arc.net/l/quote/ihqtspfk). The primary goal is to grow as large as possible by consuming smaller cells, both those controlled by other players and those that are scattered randomly throughout the game world as food. This simple premise leads to a dynamic and competitive environment where a player's size directly dictates their power and vulnerability. Players can join a randomly assigned game session, create a new session, or join an existing one using a unique session ID. The session ends only when the player is eaten or decides to quit.

The goal of this project is to design and develop a clone of Agar.io with a robust client–server architecture, ensuring scalability, performance, and smooth multiplayer interaction.

Achievements:

- Developed a real-time client–server architecture supporting multiple concurrent players.
- Implemented smooth synchronization and communication protocols to minimize lag.
- Designed an efficient game loop for rendering, collision detection, and player interaction.
- Created scalable server logic capable of handling large numbers of simultaneous connections.
- Implemented core game mechanics: cell growth, collision handling, food spawning, and player elimination.
- Delivered a competitive multiplayer experience close to the original Agar.io.

## Concept

<!-- Here you should explain:

- The type of product developed with that project, for example (non-exhaustive):
  - Application (with GUI, be it mobile, web, or desktop)
  - Command-line application (CLI could be used by humans or scripts)
  - Library
  - Web-service(s) -->
  
This project is a desktop application with a graphical user interface (GUI) packaged as a Java Archive (JAR) file. It can be executed on any system with a Java Runtime Environment (JRE) installed.

<!-- - Use case collection
  - _where_ are the users?
  - _when_ and _how frequently_ do they interact with the system?
  - _how_ do they _interact_ with the system? which _devices_ are they using?
  - does the system need to _store_ user's __data__? _which_? _where_?
  - most likely, there will be _multiple_ __roles__ -->

- **Primary Users**: The game is designed for casual gamers who enjoy competitive multiplayer experiences. Players can be located anywhere worldwide, as it is built for online play.  
- **Usage Patterns**: Sessions are typically short and played during leisure time, ranging from just a few minutes up to an hour.  
- **Interaction Method**: Players interact through a **graphical user interface (GUI)** on desktop computers, using a mouse to control their in-game cell.  
- **Data Handling**: No personal data is stored. The system only maintains temporary session data (e.g., player positions, scores), which is discarded once the session ends.  
- **User Roles**: There is only one role: **Player**. All users share the same capabilities, with no role-based differences or special permissions.  

## Requirements

<!-- - The requirements must explain __what__ (not how) the software being produced should do.
  - you should not focus on the particular problems, but exclusively on what you want the application to do.

- Requirements must be clearly identified, and possibly numbered

- Requirements are divided into:
  - __Functional__: some functionality the software should provide to the user
  - __Non-functional__: requirements that do not directly concern behavioural aspects, such as consistency, availability, etc.
  - __Implementation__: constrain the entire phase of system realization, for instance by requiring the use of a specific programming language and/or a specific software tool
    - these constraints should be adequately justified by political / economic / administrative reasons...
    - ... otherwise, implementation choices should emerge _as a consequence of_ design

- If there are domain-specific terms, these should be explained in a glossary

- Each requirement must have its own __acceptance criteria__
  - these will be important for the validation phase -->

### Functional Requirements

1. Ability to join a random game session.
2. Ability to create a new game session.
3. Ability to join an existing session using a unique session ID.
4. Support for multiple concurrent players in a session.
    - *Acceptance criteria:* at least 5 concurrent players per session.
5. Real-time visibility and interaction between players.
    - *Acceptance criteria:* latency < 200ms.
6. Low-latency handling of movements and actions.
    - *Acceptance criteria:* maintain a frame rate of at least 30 FPS during typical gameplay.
7. Mechanism for consuming smaller cells (players or food) to increase size.
    - *Acceptance criteria:* players can consume smaller cells and visibly grow in size.
8. Random spawning of food items within the map.
9. Collision detection and outcome resolution based on cell size.
10. Sessions conclude when a player is eliminated or chooses to exit.
11. Intuitive GUI for session management.
    - Button to join a random session.
    - Nickname input field.
    - Button to create a new session and join it.
    - Input field for entering a session ID to join an existing session.
    - Button to join the specified session.
12. Visual representation of player cells, food items, and other players.
    - Display player names.
    - Display the session ID so players can share it with friends.
13. Scalable server architecture to handle increasing player numbers.  
14. A master server coordinating multiple game servers that manage individual game sessions.
15. Dynamic allocation of players to game servers based on load and availability.
16. The server architecture should support scaling to accommodate increasing numbers of concurrent sessions and players.
17. Backup and failover mechanisms to ensure session continuity in case of game server failure.

## Design

<!-- This chapter explains the strategies used to meet the requirements identified in the analysis.
Ideally, the design should be the same, regardless of the technological choices made during the implementation phase.

> You can re-order the sections as you prefer, but all the sections must be present in the end -->

### Architecture

<!-- - Which architectural style?
  - why? -->

### Infrastructure

<!-- - are there _infrastructural components_ that need to be introduced? _how many_?
  - e.g. _clients_, _servers_, _load balancers_, _caches_, _databases_, _message brokers_, _queues_, _workers_, _proxies_, _firewalls_, _CDNs_, _etc._

- how do components _distribute_ over the network? _where_?
  - e.g. do servers / brokers / databases / etc. sit on the same machine? on the same network? on the same datacenter? on the same continent?

- how do components _find_ each other?
  - how to _name_ components?
  - e.g. DNS, _service discovery_, _load balancing_, _etc._

> Component diagrams are welcome here -->

### Modelling

<!-- - which __domain entities__ are there?
  - e.g. _users_, _products_, _orders_, _etc._

- how do _domain entities_ __map to__ _infrastructural components_?
  - e.g. state of a video game on central server, while inputs/representations on clients
  - e.g. where to store messages in an IM app? for how long?

- which __domain events__ are there?
  - e.g. _user registered_, _product added to cart_, _order placed_, _etc._

- which sorts of __messages__ are exchanged?
  - e.g. _commands_, _events_, _queries_, _etc._

- what information does the __state__ of the system comprehend
  - e.g. _users' data_, _products' data_, _orders' data_, _etc._ -->

<!-- > Class diagram are welcome here -->

### Interaction

<!-- - how do components _communicate_? _when_? _what_?
- _which_ __interaction patterns__ do they enact?

> Sequence diagrams are welcome here -->

### Behaviour

<!-- - how does _each_ component __behave__ individually (e.g. in _response_ to _events_ or messages)?
  - some components may be _stateful_, others _stateless_

- which components are in charge of updating the __state__ of the system? _when_? _how_?

> State diagrams are welcome here -->

### Data and Consistency Issues

<!-- - Is there any data that needs to be stored?
  - _what_ data? _where_? _why_?

- how should _persistent data_ be __stored__?
  - e.g. relations, documents, key-value, graph, etc.
  - why?

- Which components perform queries on the database?
  - _when_? _which_ queries? _why_?
  - concurrent read? concurrent write? why?

- Is there any data that needs to be shared between components?
  - _why_? _what_ data? -->

### Fault-Tolerance

<!-- - Is there any form of data __replication__ / federation / sharing?
  - _why_? _how_ does it work?

- Is there any __heart-beating__, __timeout__, __retry mechanism__?
  - _why_? _among_ which components? _how_ does it work?

- Is there any form of __error handling__?
  - _what_ happens when a component fails? _why_? _how_? -->

### Availability

<!-- - Is there any __caching__ mechanism?
  - _where_? _why_?

- Is there any form of __load balancing__?
  - _where_? _why_?

- In case of __network partitioning__, how does the system behave?
  - _why_? _how_? -->

### Security

<!-- - Is there any form of __authentication__?
  - _where_? _why_?

- Is there any form of __authorization__?
  - which sort of _access control_?
  - which sorts of users / _roles_? which _access rights_?

- Are __cryptographic schemas__ being used?
  - e.g. token verification,
  - e.g. data encryption, etc. -->

---
<!-- Riparti da qui  -->

## Implementation

<!-- - which __network protocols__ to use?
  - e.g. UDP, TCP, HTTP, WebSockets, gRPC, XMPP, AMQP, MQTT, etc.
- how should _in-transit data_ be __represented__?
  - e.g. JSON, XML, YAML, Protocol Buffers, etc.
- how should _databases_ be __queried__?
  - e.g. SQL, NoSQL, etc.
- how should components be _authenticated_?
  - e.g. OAuth, JWT, etc.
- how should components be _authorized_?
  - e.g. RBAC, ABAC, etc. -->

We choose to use **TCP (Transmission Control Protocol)** for several key reasons, with reliability and order being the most critical. Unlike UDP, TCP is a "connection-oriented" protocol that ensures data packets are delivered, and if a packet is lost, it's automatically resent. This is crucial for maintaining a consistent game state, especially for core mechanics like player movements, collisions, and cell consumption, where a lost packet could lead to players appearing to be in different locations or a desynchronized game world. Furthermore, TCP guarantees that packets arrive in the correct order, which is essential for deterministic game logic. UDP is favored for fast-paced shooters where low latency is paramount, the nature of an Agar.io clone tolerates slightly higher latency in exchange for the absolute reliability and synchronization that TCP provides.

Below is a snippet from the `application.conf` file showing the configuration for using TCP as the transport protocol with Akka's remote artery:

```yaml
  remote {
    artery {
      transport = tcp
    }
  }
```

To represent in-transit data, we opted for **JSON (JavaScript Object Notation)** produced by Akka's Jackson serializer. The converse from entity to JSON and vice versa is handled automatically by Akka, which simplifies the serialization process.

```yaml
    serializers {
      jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
    }
    serialization-bindings {
      "akka.actor.typed.ActorRef" = jackson-json
      "akka.actor.typed.internal.adapter.ActorRefAdapter" = jackson-json
      "it.unibo.protocol.Message" = jackson-json
    }
```

All data is not stored persistently, as the game is designed for temporary sessions.

### Technological details

<!-- - any particular _framework_ / _technology_ being exploited goes here -->

- Scala: main programming language used for both client and server components.
- Akka: utilized for building the actor-based concurrency model, facilitating communication between different components.
  - Akka Actors: used to implement the actor model for concurrent processing.
  - Akka Cluster: employed to create a distributed system where multiple nodes can work together.
- Java Swing: used for creating the graphical user interface (GUI) of the client application.

## Validation

### Automatic Testing

<!-- - how were individual components **_unit_-test**ed?
- how was communication, interaction, and/or integration among components tested?
- how to ___end-to-end_-test__ the system?
  - e.g. production vs. test environment

- for each test specify:
  - rationale of individual tests
  - how were the test automated
  - how to run them
  - which requirement they are testing, if any

> recall that _deployment_ __automation__ is commonly used to _test_ the system in _production-like_ environment
---
> recall to test corner cases (crashes, errors, etc.) -->

### Acceptance test

<!-- - did you perform any _manual_ testing?
  - what did you test?
  - why wasn't it automatic? -->

## Release

<!-- - how where components organized into _inter-dependant modules_ or just a single monolith?
  - provide a _dependency graph_ if possible

- were modules distributed as a _single archive_ or _multiple ones_?
  - why?

- how were archive versioned?

- were archive _released_ onto some archive repository (e.g. Maven, PyPI, npm, etc.)?
  - how to _install_ them? -->

Each release is packaged into three separate JAR files: one for the **Mother Server**, one for the **Child Server**, and one for the **Client**.  
This modular structure enables independent deployment and scaling of each component according to demand.

All JAR files follow **semantic versioning** (e.g., `1.0.0`, `1.1.0`, `2.0.0`), making it easier to track changes and maintain compatibility across components.

The JARs are distributed through a public GitHub repository, where users can download the latest versions. Each release is also tagged in the repository for convenient access.

Since the executables run on any system with a **Java Runtime Environment (JRE)**, they are fully platform-independent. Currently, they are executed locally, but with proper configuration they can also be deployed on remote servers.

The installation process is straightforward:

1. Ensure that a compatible JRE is installed on the system.
2. Download the appropriate JAR files from the GitHub repository.
3. Run the JAR files using the command line with `java -jar <filename>.jar`.
   To launch the entire system, follow this sequence (different order is also possible, but the Mother Server must always be started first):
   1. Start the Mother Server first.
   2. Start one or more Child Servers.
   3. Finally, launch the Client application.

## Deployment

<!-- - should one install your software from scratch, how to do it?
  - provide instructions
  - provide expected outcomes -->

## User Guide

<!-- - how to use your software?
  - provide instructions
  - provide expected outcomes
  - provide screenshots if possible -->

## Self-evaluation

<!-- - An individual section is required for each member of the group
- Each member must self-evaluate their work, listing the strengths and weaknesses of the product
- Each member must describe their role within the group as objectively as possible.
It should be noted that each student is only responsible for their own section -->

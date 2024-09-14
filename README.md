# Xonix

**Xonix** is a classic arcade game that first appeared in 1984. The player controls a machine that must fill empty areas of the field while avoiding collisions with enemies and preventing the entire field from being filled by enemies.

## Key Features
- **Control**: The player controls a machine that moves across the field, leaving a trail behind.
- **Objective**: Fill a certain percentage of the field, avoiding collisions with enemies.
- **Enemies**: Enemies on the field try to hinder the player.
- **Score**: The player earns points for each captured section of the field.
- **Levels**: The game consists of several levels with increasingly difficult conditions.

## Installation and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/AndreyAA/xonix.git
   ```
   2. Run the game:
      #### with maven:
      ```maven
      mvn clean package exec:java
      ```

      #### with bash: 
      ```bash
        mvn clean package
        java -jar target/xonix-1.0-SNAPSHOT-jar-with-dependencies.jar
      ```
      
      ### with custom levels json file:
      ``` maven
      mvn clean package exec:java -Dexec.args="pathToCustomLevelJsonFile"
      ```  

## Controls
- **Arrow Keys**: Control the machine.
- **Spacebar**: Pause.

## Contribution
We welcome contributions to the development of the game.

## License
This project is licensed under the [MIT License](LICENSE).

## Contact
todo

---

**Xonix** Code games and take a fun! )
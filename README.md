# Minesweeper Solver in Java

## Overview
This repository contains a Minesweeper solver written in Java using the `Robot` class for automation. The solver detects board positions, updates state information, and makes logical moves to solve the game efficiently. It includes various heuristics for flagging mines and uncovering safe squares.

## Features
- **Automated Gameplay**: Uses screen capture and image processing to analyze the board.
- **Mouse Control**: Simulates human clicks for interacting with the game.
- **Board Calibration**: Dynamically detects the board size and position.
- **Heuristic-Based Solving**: Implements logical deduction to minimize random guessing.
- **Failsafe Handling**: Checks for inconsistencies and retries when necessary.

## Requirements
- Java 8 or later
- A running instance of Minesweeper
- Screen resolution matching the game's window

## Building and Running
### Compile the Project
```bash
javac Main.java
```
### Run the Solver
```bash
java Main
```

## How It Works
1. **Board Calibration**: Detects grid size, cell size, and position on the screen.
2. **Game Initialization**: Clicks the first square to start the game.
3. **Board Parsing**: Analyzes the screen to determine revealed numbers and flagged mines.
4. **Logical Deduction**: Uses patterns to safely uncover squares and flag mines.
5. **Random Guessing**: If no safe move is available, selects a random square.
6. **Iteration**: Repeats steps until the board is solved or a mine is hit.

## Future Improvements
- Support for different Minesweeper versions.
- Improved image recognition for different themes.
- Better heuristics to reduce the number of random guesses.

## License
This project is open-source under the MIT License.


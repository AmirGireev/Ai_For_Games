## Underwater AI Agent
This is an autonomous AI agent written in Java that plays a 2D underwater game. It automatically collects resources, avoids enemies, and manages its own upgrades.
Note: This repository only contains the AI logic (MeineKI.java) that I wrote. The game engine and graphics belong to my university and are not included.


# What the AI does:

Pathfinding: It finds the shortest way to items and navigates around obstacles using a grid-based search algorithm.

Enemy Avoidance: It detects moving enemies (fish) or water currents and automatically steers away from them.

Resource Management: It monitors its air supply to avoid drowning and uses collected coins to buy upgrades in the shop.

Smooth Movement: It calculates rotation angles so the character moves naturally.



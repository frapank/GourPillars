# Pillars of Luck 🏹⛓️  

---

## 🔥 Key Features  
- 🧱 Dynamic pillar collapse system  
- 🎁 Random items/blocks every 2 seconds (configurable)  
- 🏆 Player statistics tracking (kills, deaths, wins)
- 🛠️ Dual configuration system (`config.yml` + `language.yml`)  
- 📊 15+ Placeholders for integration with other plugins  
- 🏰 Future-proof arena-specific configurations  

---

## 🛠️ Configuration  

### PlaceholderAPI Variables  
``` 
Arena Info:
%pillars_minplayers%     - Minimum players to start
%pillars_maxplayers%     - Max players in arena
%pillars_waitingplayers% - Players in lobby
%pillars_arenaname%      - Current arena name
%pillars_aliveplayers%   - Remaining players
%pillars_time%           - Elapsed game time
%pillars_ingamekills%    - Player kills in game

Player Stats:
%pillars_kills%          - Total kills
%pillars_deaths%         - Total deaths
%pillars_wins%           - Total victories
%pillars_defeats%        - Total defeats
%pillars_gamesplayed%    - Total games played
%pillars_level%          - Player level
%pillars_xp%             - Current XP

Global Stats:
%pillars_arenacount%     - Active arenas
%pillars_playersinmatch% - Total players ingame
```

---

## 🕹️ Commands & Permissions  
| Command         | Permission | Description |
|-----------------|------------|-------------|
| `/randomjoin`   | - | Join random arena |
| `/join <arena>` | - | Join an arena |
| `/leave`        | - | leave arena |
| `/edit`         | `gpillars.admim` | arena configuration |
| `/build`        | `gpillars.build` | build session |

---

## 🎓 Gameplay Flow  
1. Players join arena lobby
2. Game starts when minimum players reached
3. Random items distributed periodically
4. Pillars begin collapsing after initial phase
5. Fight until one survivor remains!

---

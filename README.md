# Luck of the Pillars 🏹⛓️  

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
%luckofthepillars_minplayers%     - Minimum players to start
%luckofthepillars_waitingplayers% - Players in lobby
%luckofthepillars_arenaname%      - Current arena name
%luckofthepillars_aliveplayers%   - Remaining players
%luckofthepillars_time%           - Elapsed game time

Player Stats:
%luckofthepillars_kills%          - Total kills
%luckofthepillars_deaths%         - Total deaths
%luckofthepillars_wins%           - Total victories
%luckofthepillars_level%          - Player level
%luckofthepillars_xp%             - Current XP

Global Stats:
%luckofthepillars_arenacount%     - Active arenas
%luckofthepillars_playersinmatch% - Total players ingame
```

---

## 🕹️ Commands & Permissions  
| Command | Permission | Description |
|---------|------------|-------------|
| `/randomjoin` | - | Join random arena |
| `/join <arena>` | - | Join an arena |
| `/leave` | - | leave arena |
| `/edit` | `pillars.admin` | arena configuration |

---

## 🎓 Gameplay Flow  
1. Players join arena lobby
2. Game starts when minimum players reached
3. Random items distributed periodically
4. Pillars begin collapsing after initial phase
5. Fight until one survivor remains!

---

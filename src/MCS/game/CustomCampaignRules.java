package MCS.game;

import mindustry.Vars;
import mindustry.game.*;
import mindustry.gen.Groups;
import mindustry.type.Planet;

public class CustomCampaignRules extends CampaignRules {
    public CustomTeamRules enemy;
    public CustomTeamRules player;
    public CustomDifficulty customDiff;
    public float enemySpawnMultiplier; //percent%
    public float waveTimeMultiplier; //percent%
    public int extendWaves;
    public float unitFactoryActivationDelay;

    public CustomCampaignRules(Planet planet){
        enemy = new CustomTeamRules();
        player = new CustomTeamRules();
        customDiff = CustomDifficulty.normal;

        enemySpawnMultiplier = 100f;
        waveTimeMultiplier = 100f;
        extendWaves = 0;
        unitFactoryActivationDelay = 0f;

        fog = planet.campaignRuleDefaults.fog;
        showSpawns = planet.campaignRuleDefaults.showSpawns;
        sectorInvasion = planet.campaignRuleDefaults.sectorInvasion;
        randomWaveAI = planet.campaignRuleDefaults.randomWaveAI;
        rtsAI = planet.campaignRuleDefaults.rtsAI;
        pauseDisabled = planet.campaignRuleDefaults.pauseDisabled;
        clearSectorOnLose = planet.campaignRuleDefaults.clearSectorOnLose;
    }

    @Override
    public void apply(Planet planet, Rules rules) {
        rules.staticFog = rules.fog = fog;
        rules.showSpawns = showSpawns;
        rules.randomWaveAI = randomWaveAI;
        rules.pauseDisabled = pauseDisabled;
        if(planet.showRtsAIRule){
            boolean enabled = rtsAI && rules.attackMode;
            boolean swapped = rules.teams.get(rules.waveTeam).rtsAi != enabled;

            rules.teams.get(rules.waveTeam).rtsAi = enabled;
            rules.teams.get(rules.waveTeam).rtsMaxSquad = 15;

            if(swapped && Vars.state.isGame()){
                Groups.unit.each(u -> {
                    if(u.team == rules.waveTeam && !u.isPlayer()){
                        u.resetController();
                    }
                });
            }
        }

        planet.campaignRules.sectorInvasion = sectorInvasion;
        planet.campaignRules.clearSectorOnLose = clearSectorOnLose;

        rules.objectiveTimerMultiplier = waveTimeMultiplier / 100.0f;
        if(Vars.state.hasSector() && Vars.state.getSector().info.winWave > 0){
            rules.winWave = Vars.state.getSector().info.winWave + extendWaves;
        }

        rules.teams.get(rules.waveTeam).unitFactoryActivationDelay = unitFactoryActivationDelay;

        rules.teams.get(rules.waveTeam).blockHealthMultiplier = enemy.blockHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitHealthMultiplier = enemy.unitHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitCostMultiplier = enemy.unitCostMultiplier;
        rules.teams.get(rules.waveTeam).unitBuildSpeedMultiplier = enemy.unitBuildSpeedMultiplier;

        rules.teams.get(Vars.player.team()).blockHealthMultiplier = player.blockHealthMultiplier;
        rules.teams.get(Vars.player.team()).unitHealthMultiplier = player.unitHealthMultiplier;
        rules.teams.get(Vars.player.team()).unitCostMultiplier = player.unitCostMultiplier;
        rules.teams.get(Vars.player.team()).unitBuildSpeedMultiplier = player.unitBuildSpeedMultiplier;
    }

    public CustomTeamRules team(RuleTeam t){
        if(t == RuleTeam.enemy){
            return enemy;
        }
        else if(t == RuleTeam.player) {
            return player;
        }
        return null;
    }

    public class CustomTeamRules{
        public RuleTeam ruleteam;
        public float blockHealthMultiplier;
        public float unitHealthMultiplier;
        public float unitCostMultiplier;
        public float unitBuildSpeedMultiplier;
        public CustomTeamRules(){
            blockHealthMultiplier = 1f;
            unitHealthMultiplier = 1f;
            unitCostMultiplier = 1f;
            unitBuildSpeedMultiplier = 1f;
        }
    }
}

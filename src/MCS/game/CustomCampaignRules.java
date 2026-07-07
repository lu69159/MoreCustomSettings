package MCS.game;

import mindustry.Vars;
import mindustry.game.*;
import mindustry.gen.Groups;
import mindustry.type.Planet;

import java.lang.reflect.Field;

public class CustomCampaignRules extends CampaignRules {
    public CustomTeamRules enemy;
    public CustomTeamRules player;
    public CustomDifficulty customDiff;
    public float enemySpawnMultiplier; //percent%
    public float waveTimeMultiplier; //percent%
    public int extendWaves;
    public float unitFactoryActivationDelay;

    public boolean howSpawns; //改这个的家伙吃饱撑的吧。showSpawns/hideSpawns
    public Field howSpawnsField;
    public boolean isShowSpawns;

    private Field initSpawn(Planet planet){
        Field field;
        try{
            field = CampaignRules.class.getDeclaredField("showSpawns");
            isShowSpawns = true;
        }catch(NoSuchFieldException e){
            try{
                field = CampaignRules.class.getDeclaredField("hideSpawns");
                isShowSpawns = false;
            }catch (NoSuchFieldException ex){
                throw new RuntimeException(ex);
            }
        }
        return field;
    }
    public boolean howSpawnsBool(Planet planet){
         try{
             return howSpawnsField.getBoolean(planet.campaignRuleDefaults);
         }catch(IllegalAccessException e){
             throw new RuntimeException(e);
         }
    }

    public CustomCampaignRules(Planet planet){
        enemy = new CustomTeamRules();
        player = new CustomTeamRules();
        customDiff = CustomDifficulty.normal;

        enemySpawnMultiplier = 100f;
        waveTimeMultiplier = 100f;
        extendWaves = 0;
        unitFactoryActivationDelay = 0f;

        howSpawnsField = initSpawn(planet); // showSpawns/hideSpawns
        howSpawns = howSpawnsBool(planet);

        fog = planet.campaignRuleDefaults.fog;
        sectorInvasion = planet.campaignRuleDefaults.sectorInvasion;
        randomWaveAI = planet.campaignRuleDefaults.randomWaveAI;
        rtsAI = planet.campaignRuleDefaults.rtsAI;
        pauseDisabled = planet.campaignRuleDefaults.pauseDisabled;
        clearSectorOnLose = planet.campaignRuleDefaults.clearSectorOnLose;
    }

    @Override
    public void apply(Planet planet, Rules rules){
        try {
            var tmpSpawns = isShowSpawns ? rules.getClass().getField("showSpawns") : rules.getClass().getField("hideSpawns");
            tmpSpawns.setBoolean(rules, howSpawns);
        }catch(NoSuchFieldException | IllegalAccessException e){
            throw new RuntimeException(e);
        }

        rules.staticFog = rules.fog = fog;
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

    public void set(CustomDifficulty diff){
        waveTimeMultiplier = diff.waveTimeMultiplier * 100f;
        enemySpawnMultiplier = diff.enemySpawnMultiplier * 100f;

        enemy.unitCostMultiplier = diff.enemySpawnMultiplier;
        enemy.unitBuildSpeedMultiplier = diff.enemySpawnMultiplier;
        enemy.blockHealthMultiplier = diff.enemyHealthMultiplier;
        enemy.unitHealthMultiplier = diff.enemyHealthMultiplier;

        player.unitCostMultiplier = 1f;
        player.unitBuildSpeedMultiplier = 1f;
        player.blockHealthMultiplier = 1f;
        player.unitHealthMultiplier = 1f;

        extendWaves = 0;
        unitFactoryActivationDelay = 0f;
        customDiff = diff;
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

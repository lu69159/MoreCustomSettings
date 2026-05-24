package MCS.game;

import arc.*;
import mindustry.game.Difficulty;

import static mindustry.Vars.content;
import static mindustry.game.EventType.*;
import static MCS.main.*;
import static MCS.game.PlanetCustomRulesMaps.*;

public class MCSsaves{

    public MCSsaves(){
        Events.on(ClientLoadEvent.class, e -> {
            loadCustomSetting();
        });
    }

    public void saveCustomSetting(PlanetCustomCampaignRules customrules){
        CustomCampaignRules r = customrules.rules;
        String name = customrules.planet.name + "MCS";

        Core.settings.put(name + "EBH", r.enemy.blockHealthMultiplier);
        Core.settings.put(name + "EUH", r.enemy.unitHealthMultiplier);
        Core.settings.put(name + "EUC", r.enemy.unitCostMultiplier);
        Core.settings.put(name + "EUBS", r.enemy.unitBuildSpeedMultiplier);
        Core.settings.put(name + "PBH", r.player.blockHealthMultiplier);
        Core.settings.put(name + "PUH", r.player.unitHealthMultiplier);
        Core.settings.put(name + "PUC", r.player.unitCostMultiplier);
        Core.settings.put(name + "PUBS", r.player.unitBuildSpeedMultiplier);

        Core.settings.put(name + "ES", r.enemySpawnMultiplier);
        Core.settings.put(name + "WT", r.waveTimeMultiplier);
        Core.settings.put(name + "EW", r.extendWaves);
        Core.settings.put(name + "UFA", r.unitFactoryActivationDelay);

        for(int i = 0; i < Difficulty.all.length; i++){
            if(r.customDiff== CustomDifficulty.all[i]){
                Core.settings.put(name + "D", i);
            }
        }

        Core.settings.put(name + "SI", r.sectorInvasion);
        Core.settings.put(name + "fog", r.fog);
        Core.settings.put(name + "SS", r.showSpawns);
        Core.settings.put(name + "RW", r.randomWaveAI);
        Core.settings.put(name + "RTS", r.rtsAI);
        Core.settings.put(name + "CS", r.clearSectorOnLose);
    }

    public void loadCustomSetting(){
        for(var p : content.planets()){
            String name = p.name + "MCS";
            CustomCampaignRules r = new CustomCampaignRules(p);

            r.enemy.blockHealthMultiplier = Core.settings.getFloat(name + "EBH", 1f);
            r.enemy.unitHealthMultiplier = Core.settings.getFloat(name + "EUH", 1f);
            r.enemy.unitCostMultiplier =  Core.settings.getFloat(name + "EUC", 1f);
            r.enemy.unitBuildSpeedMultiplier = Core.settings.getFloat(name + "EUBS", 1f);
            r.player.blockHealthMultiplier = Core.settings.getFloat(name + "PBH", 1f);
            r.player.unitHealthMultiplier = Core.settings.getFloat(name + "PUH", 1f);
            r.player.unitCostMultiplier =  Core.settings.getFloat(name + "PUC", 1f);
            r.player.unitBuildSpeedMultiplier = Core.settings.getFloat(name + "PUBS", 1f);

            r.enemySpawnMultiplier = Core.settings.getFloat(name + "ES", 1f);
            r.waveTimeMultiplier = Core.settings.getFloat(name + "WT", 100f);
            r.extendWaves = Core.settings.getInt(name + "EW", 0);
            r.unitFactoryActivationDelay = Core.settings.getFloat(name + "UFA", 0f);

            r.customDiff = CustomDifficulty.all[Core.settings.getInt(name + "D", 2)];

            r.sectorInvasion = Core.settings.getBool(name + "SI", p.campaignRules.sectorInvasion);
            r.fog = Core.settings.getBool(name + "fog", p.campaignRules.fog);
            r.showSpawns = Core.settings.getBool(name + "SS", p.campaignRules.showSpawns);
            r.randomWaveAI = Core.settings.getBool(name + "RW", p.campaignRules.randomWaveAI);
            r.rtsAI = Core.settings.getBool(name + "RTS", p.campaignRules.randomWaveAI);
            r.clearSectorOnLose = Core.settings.getBool(name + "CS", p.clearSectorOnLose);

            rulesMaps.put(new PlanetCustomCampaignRules(p, r));
        }
    }
}

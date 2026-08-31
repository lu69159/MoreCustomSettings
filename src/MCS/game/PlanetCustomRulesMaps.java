package MCS.game;

import arc.struct.*;
import mindustry.type.Planet;
import MCS.game.enumClass.CustomDifficulty;

import static arc.Core.settings;
import static mindustry.Vars.content;

public class PlanetCustomRulesMaps{
    Seq<PlanetCustomCampaignRules> maps = new Seq<>();

    public void load(){
        loadCustomSetting();
    }

    public void save(Planet planet, CustomCampaignRules rules){
        PlanetCustomCampaignRules customrules = new PlanetCustomCampaignRules(planet, rules);
        saveCustomSetting(customrules);
        boolean found = false;
        for(int i = 0; i < maps.size; i++){
            if(maps.get(i).planet == planet){
                maps.set(i, customrules);
                found = true;
                break;
            }
        }
        if(!found){
            maps.add(customrules);
        }
    }

    public void reset(){
        for(var p : content.planets()){
            String name = p.name + "MCS";

            settings.remove(name + "EBH");
            settings.remove(name + "EUH");
            settings.remove(name + "EUC");
            settings.remove(name + "EUBS");
            settings.remove(name + "PBH");
            settings.remove(name + "PUH");
            settings.remove(name + "PUC");
            settings.remove(name + "PUBS");

            settings.remove(name + "ES");
            settings.remove(name + "WT");
            settings.remove(name + "EW");
            settings.remove(name + "UFA");

            settings.remove(name + "D");

            settings.remove(name + "SI");
            settings.remove(name + "fog");
            settings.remove(name + "HS");
            settings.remove(name + "RW");
            settings.remove(name + "RTS");
            settings.remove(name + "CS");
        }

        for(var pr : maps){
            pr.rules = new CustomCampaignRules(pr.planet);
        }
    }

    public void put(PlanetCustomCampaignRules rules){
        maps.add(rules);
    }

    public CustomCampaignRules get(Planet planet){
        for(var rules : maps){
            if(rules.planet == planet) return rules.rules;
        }
        return new CustomCampaignRules(planet);
    }

    private void loadCustomSetting(){
        for(var p : content.planets()){
            String name = p.name + "MCS";
            CustomCampaignRules r = new CustomCampaignRules(p);

            r.enemy.blockHealthMultiplier = settings.getFloat(name + "EBH", 1f);
            r.enemy.unitHealthMultiplier = settings.getFloat(name + "EUH", 1f);
            r.enemy.unitCostMultiplier =  settings.getFloat(name + "EUC", 1f);
            r.enemy.unitBuildSpeedMultiplier = settings.getFloat(name + "EUBS", 1f);
            r.player.blockHealthMultiplier = settings.getFloat(name + "PBH", 1f);
            r.player.unitHealthMultiplier = settings.getFloat(name + "PUH", 1f);
            r.player.unitCostMultiplier =  settings.getFloat(name + "PUC", 1f);
            r.player.unitBuildSpeedMultiplier = settings.getFloat(name + "PUBS", 1f);

            r.enemySpawnMultiplier = settings.getFloat(name + "ES", 100f);
            r.waveTimeMultiplier = settings.getFloat(name + "WT", 100f);
            r.extendWaves = settings.getInt(name + "EW", 0);
            r.unitFactoryActivationDelay = settings.getFloat(name + "UFA", 0f);

            r.customDiff = CustomDifficulty.all[settings.getInt(name + "D", 2)];

            r.sectorInvasion = settings.getBool(name + "SI", p.campaignRules.sectorInvasion);
            r.fog = settings.getBool(name + "fog", p.campaignRules.fog);
            r.hideSpawns = settings.getBool(name + "HS", p.campaignRules.hideSpawns);
            r.randomWaveAI = settings.getBool(name + "RW", p.campaignRules.randomWaveAI);
            r.rtsAI = settings.getBool(name + "RTS", p.campaignRules.randomWaveAI);
            r.clearSectorOnLose = settings.getBool(name + "CS", p.clearSectorOnLose);

            put(new PlanetCustomCampaignRules(p, r));

            if(settings.getBool("forceCampaignDifficulty") && settings.getBool(p.name + "-forceCD")){
                p.allowCampaignRules = true;
            }
        }
    }

    private void saveCustomSetting(PlanetCustomCampaignRules customrules){
        CustomCampaignRules r = customrules.rules;
        String name = customrules.planet.name + "MCS";


        settings.put(name + "EBH", r.enemy.blockHealthMultiplier);
        settings.put(name + "EUH", r.enemy.unitHealthMultiplier);
        settings.put(name + "EUC", r.enemy.unitCostMultiplier);
        settings.put(name + "EUBS", r.enemy.unitBuildSpeedMultiplier);
        settings.put(name + "PBH", r.player.blockHealthMultiplier);
        settings.put(name + "PUH", r.player.unitHealthMultiplier);
        settings.put(name + "PUC", r.player.unitCostMultiplier);
        settings.put(name + "PUBS", r.player.unitBuildSpeedMultiplier);

        settings.put(name + "ES", r.enemySpawnMultiplier);
        settings.put(name + "WT", r.waveTimeMultiplier);
        settings.put(name + "EW", r.extendWaves);
        settings.put(name + "UFA", r.unitFactoryActivationDelay);

        for(int i = 0; i < CustomDifficulty.all.length; i++){
            if(r.customDiff== CustomDifficulty.all[i]){
                settings.put(name + "D", i);
            }
        }

        settings.put(name + "SI", r.sectorInvasion);
        settings.put(name + "fog", r.fog);
        settings.put(name + "HS", r.hideSpawns);
        settings.put(name + "RW", r.randomWaveAI);
        settings.put(name + "RTS", r.rtsAI);
        settings.put(name + "CS", r.clearSectorOnLose);
    }

    public static class PlanetCustomCampaignRules{
        public Planet planet;
        public CustomCampaignRules rules;

        public PlanetCustomCampaignRules(Planet planet, CustomCampaignRules rules){
            this.planet = planet;
            this.rules = rules;
        }
    }
}

package MCS.game;

import arc.*;
import arc.struct.*;
import arc.util.serialization.Json.*;
import mindustry.type.Planet;

import static MCS.main.*;

public class PlanetCustomRulesMaps{
    private Seq<PlanetCustomCampaignRules> maps = new Seq<>();

    public void save(Planet planet, CustomCampaignRules rules){
        PlanetCustomCampaignRules customrules = new PlanetCustomCampaignRules(planet, rules);
        saves.saveCustomSetting(customrules);
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

    public void put(PlanetCustomCampaignRules rules){
        maps.add(rules);
    }

    public CustomCampaignRules get(Planet planet){
        for(var rules : maps){
            if(rules.planet == planet) return rules.rules;
        }
        return new CustomCampaignRules(planet);
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

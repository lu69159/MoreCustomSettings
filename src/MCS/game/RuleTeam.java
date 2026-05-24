package MCS.game;

import arc.Core;

public enum RuleTeam{
    enemy,
    player;


    public static final RuleTeam[] all = values();

    public String localized(){
        return Core.bundle.get("ruleteam." + name());
    }
}
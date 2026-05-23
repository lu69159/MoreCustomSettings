package MCS.game;

import arc.Core;

public enum RuleTeam{
    player,
    enemy;

    public static final RuleTeam[] all = values();

    public String localized(){
        return Core.bundle.get("ruleteam." + name());
    }
}
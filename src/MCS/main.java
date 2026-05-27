package MCS;

import MCS.ui.dialogs.*;
import MCS.ui.fragments.*;
import arc.Events;
import mindustry.mod.*;
import MCS.game.*;

import static mindustry.game.EventType.*;

public class main extends Mod{
    public static MCSsettingMenuDialog menu;
    public static BuildAttackFrag attacked;
    public static PlanetCustomRulesMaps rulesMaps;
    public static CustomMusicLoader musicLoader;

    public main(){
        menu = new MCSsettingMenuDialog();
        attacked = new BuildAttackFrag();
        rulesMaps = new PlanetCustomRulesMaps();
        musicLoader = new CustomMusicLoader();

        Events.run(ClientLoadEvent.class, () -> {
            musicLoader.load();
            rulesMaps.load();
            menu.load();
            attacked.load();
        });
    }
}

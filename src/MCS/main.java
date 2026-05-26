package MCS;

import mindustry.mod.*;
import MCS.ui.*;
import MCS.game.*;

public class main extends Mod{
    public static MCSsettingMenuDialog menu;
    public static PlanetCustomRulesMaps rulesMaps;
    public static CustomMusicLoader musicLoader;
    public static MCSsaves saves;

    public main(){
        menu = new MCSsettingMenuDialog();
        rulesMaps = new PlanetCustomRulesMaps();
        musicLoader = new CustomMusicLoader();
        saves = new MCSsaves();
    }
}

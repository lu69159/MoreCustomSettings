package MCS;

import arc.*;
import arc.files.Fi;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import MCS.ui.*;
import MCS.game.*;
import mindustry.ui.dialogs.CampaignRulesDialog;

import static mindustry.Vars.*;

public class main extends Mod{
    public static MCSsettingMenuDialog menu;
    public static PlanetCustomRulesMaps rulesMaps;
    public static MCSsaves saves;

    public main(){
        menu = new MCSsettingMenuDialog();
        rulesMaps = new PlanetCustomRulesMaps();
        saves = new MCSsaves();
    }

    @Override
    public void loadContent(){

    }
}

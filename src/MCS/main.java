package MCS;

import arc.Events;
import arc.files.*;
import mindustry.mod.*;
import MCS.game.*;
import MCS.ui.dialogs.*;
import MCS.ui.fragments.*;

import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class main extends Mod{
    public static Fi saveFolder = dataDirectory.child("MCS-data");

    public static MCSsettingMenuDialog menu;
    public static CustomAttackFrag attacked;
    public static PlanetCustomRulesMap rulesMap;
    public static CustomMusicLoader musicLoader;
    public static ContentManager contentManager;

    public main(){
        musicLoader = new CustomMusicLoader();
        attacked = new CustomAttackFrag();
        rulesMap = new PlanetCustomRulesMap();
        contentManager = new ContentManager();
        menu = new MCSsettingMenuDialog();

        Events.run(ClientLoadEvent.class, () -> {
            if(!saveFolder.exists()) saveFolder.mkdirs();

            musicLoader.load();
            attacked.load();
            rulesMap.load();
            contentManager.load();
            menu.load();
        });
    }
}

package MCS;

import MCS.ui.MCSUI;
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
    public static PlanetCustomRulesMap rulesMap;
    public static CustomMusicLoader musicLoader;
    public static ContentManager contentManager;
    public static MCSUI MCSui;

    public main(){
        musicLoader = new CustomMusicLoader();
        rulesMap = new PlanetCustomRulesMap();
        contentManager = new ContentManager();
        MCSui = new MCSUI();

        Events.run(ClientLoadEvent.class, () -> {
            if(!saveFolder.exists()) saveFolder.mkdirs();
            musicLoader.load();
            rulesMap.load();
            contentManager.load();
            MCSui.load();
        });
    }
}

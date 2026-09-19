package MCS;

import MCS.game.*;
import MCS.ui.*;
import MCS.ui.dialogs.*;
import MCS.ui.fragments.*;
import arc.Events;
import arc.files.*;
import mindustry.mod.*;

import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class main extends Mod{
    public static Fi saveFolder = dataDirectory.child("MCS-data");
    public static PlanetCustomRulesMap rulesMap;
    public static CustomMusicLoader musicLoader;
    public static ContentManager contentManager;
    public static MCSUI MCSui;

    public static boolean isFoo = false;

    public main(){
        musicLoader = new CustomMusicLoader();
        rulesMap = new PlanetCustomRulesMap();
        contentManager = new ContentManager();
        MCSui = new MCSUI();

        Events.run(ClientLoadEvent.class, () -> {
            isFoo();
            if(!saveFolder.exists()) saveFolder.mkdirs();
            musicLoader.load();
            rulesMap.load();
            contentManager.load();
            MCSui.load();
        });
    }

    public void isFoo(){
        try{
            Class.forName("mindustry.client.Main", false, getClass().getClassLoader());
            isFoo = true;
        }catch(Throwable t){
            isFoo = false;
        }
    }
}

package MCS.ui;

import arc.*;
import arc.func.*;
import mindustry.ai.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.dialogs.*;
import MCS.game.*;

import static arc.Core.settings;
import static mindustry.ui.dialogs.SettingsMenuDialog.*;
import static mindustry.Vars.*;

public class MCSsettingMenuDialog {
    public Cons<SettingsTable> settingBuilder = t -> {
        t.checkPref("enablecustomcampaigndifficulty", true, b -> {
            if(b){
                ui.campaignRules = new CustomCampaignRulesDialog();
                spawner = new CustomWaveSpawner();
            }
            else{
                ui.campaignRules = new CampaignRulesDialog();
                spawner = new WaveSpawner();
            }
        });
    };

    public MCSsettingMenuDialog(){
        Events.on(ClientLoadEvent.class, e -> {
            try{
                ui.settings.addCategory(Core.bundle.get("morecustomsettings"), Icon.settings, settingBuilder);
            } catch(Exception ex) {
                throw new RuntimeException(ex);
            }
            if(settings.getBool("enablecustomcampaigndifficulty")){
                ui.campaignRules = new CustomCampaignRulesDialog();
                spawner = new CustomWaveSpawner();
            }
        });
    }
}

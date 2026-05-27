package MCS.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.scene.style.Drawable;
import arc.util.*;
import mindustry.ai.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import MCS.game.*;

import static arc.Core.settings;
import static mindustry.ui.dialogs.SettingsMenuDialog.*;
import static mindustry.Vars.*;
import static MCS.main.*;

public class MCSsettingMenuDialog {
    private BaseDialog musicImportDialog, musicInGameDialog;

    public Cons<SettingsTable> settingBuilder = t -> {
        t.checkPref("enableBuildAttackFrag", false, b -> {
            attacked.enabled = b;
        });
        t.checkPref("bannedAttackedBlocksWhitelist", false, b -> {
            attacked.whitelist = b;
        });
        t.pref(new ButtonSetting("@bannedAttackedBlocks", Icon.cancel, () -> attacked.bannedAttackBlocksDialog.show(attacked.bannedAttackBlocks)));
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
        t.checkPref("enableCustomMusic", false, b -> {
            if(b){
                musicLoader.loadCustom();
            }
            else{
                musicLoader.reset();
            }
        });
        t.pref(new ButtonSetting("@importMusic", Icon.play, () -> musicImportDialog.show()));
        if(!mobile){
            t.pref(new ButtonSetting("@openMusicFolder", Icon.folder, () -> {
                if(musicLoader.musicFolder == null || !musicLoader.musicFolder.exists()) musicLoader.loadFolder();
                Core.app.openFolder(musicLoader.musicFolder.absolutePath());
            }));
        }
        t.pref(new ButtonSetting("@clearMusic", Icon.trash,
                () -> ui.showConfirm("@clearMusic", "@clearMusic.confirm", () -> musicLoader.delete())
        ));
    };

    public void load(){
        musicInGameDialog = new BaseDialog("@importMusic");
        musicInGameDialog.addCloseButton();
        musicInGameDialog.cont.table(Tex.button, t -> {
            t.defaults().size(200f, 60f).left();

            t.button("@importMusic.ambient", Styles.flatt, musicLoader.importMusic("a"));
            t.row();
            t.button("@importMusic.dark", Styles.flatt, musicLoader.importMusic("d"));
            t.row();
            t.button("@importMusic.boss", Styles.flatt, musicLoader.importMusic("b"));
            t.row();
        });

        musicImportDialog = new BaseDialog("@importMusic");
        musicImportDialog.addCloseButton();
        musicImportDialog.cont.table(Tex.button, t -> {
            t.defaults().size(200f, 60f).left();

            t.button("@importMusic.inGame", Styles.flatt, () -> musicInGameDialog.show());
            t.row();
            t.button("@importMusic.editor", Styles.flatt, musicLoader.importMenuMusic("editor"));
            t.row();
            t.button("@importMusic.menu", Styles.flatt, musicLoader.importMenuMusic("menu"));
            t.row();
        });

        try{
            ui.settings.addCategory(Core.bundle.get("morecustomsettings"), Icon.settings, settingBuilder);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
        if(settings.getBool("enablecustomcampaigndifficulty")){
            ui.campaignRules = new CustomCampaignRulesDialog();
            spawner = new CustomWaveSpawner();
        }
    }

    public class ButtonSetting extends SettingsTable.Setting{
        @Nullable Drawable icon;
        @Nullable Runnable onClick;
        public ButtonSetting(String name) {
            super(name);
        }

        public ButtonSetting(String name, Drawable icon, Runnable onClick){
            this(name);
            this.icon = icon;
            this.onClick = onClick;
        }

        @Override
        public void add(SettingsTable table) {
            table.button(name, icon, onClick).marginLeft(4).growX().row();
        }
    }
}

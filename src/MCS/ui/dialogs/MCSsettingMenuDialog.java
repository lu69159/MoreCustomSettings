package MCS.ui.dialogs;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import MCS.game.*;

import static arc.Core.settings;
import static mindustry.ui.dialogs.SettingsMenuDialog.*;
import static mindustry.Vars.*;
import static MCS.main.*;

public class MCSsettingMenuDialog {
    private BaseDialog blockStringDialog, unitStringDialog, musicImportDialog, musicInGameDialog, musicListDialog;
    private musicSquareSearchDialog musicSearchDialog;

    public Cons<SettingsTable> settingBuilder = t -> {
        t.pref(new TitleSetting("@settingtitle.music"));

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
                if (musicLoader.musicFolder == null || !musicLoader.musicFolder.exists()) musicLoader.loadFolder();
                Core.app.openFolder(musicLoader.musicFolder.absolutePath());
            }));
        }
        t.pref(new ButtonSetting("@clearMusic", Icon.trash,
                () -> ui.showConfirm("@clearMusic", "@clearMusic.confirm", () -> musicLoader.delete())
        ));
        t.pref(new ButtonSetting("@musicList", Icon.list, () -> {
            rebuildMusicList();
            musicListDialog.show();
        }));
        t.pref(new ButtonSetting("@musicSquare.search", Icon.zoom, () -> {
            musicSearchDialog.show();
        }));

        t.pref(new TitleSetting("@settingtitle.campaignDifficulty"));

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

        //forceCampaignDifficulty
        t.checkPref("forceCampaignDifficulty", false, b -> {
            if(b){
                for(var p : content.planets()){
                    if(!p.allowCampaignRules){
                        settings.put(p.name + "-forceCD", true);
                        p.allowCampaignRules = true;
                    }
                }
            }
            else{
                for(var p : content.planets()){
                    if(settings.getBool(p.name + "-forceCD")){
                        settings.remove(p.name + "-forceCD");
                        p.allowCampaignRules = false;
                    }
                }
            }
        });

        t.pref(new TitleSetting("@settingtitle.buildAttacked"));

        t.checkPref("enableBuildAttackFrag", false, b -> attacked.blockEnabled = b);
        t.pref(new ButtonSetting("@editAttackedString", Icon.pencil, () -> blockStringDialog.show()));
        t.checkPref("bannedAttackedBlocksWhitelist", false, b -> attacked.blockWhitelist = b);
        t.pref(new ButtonSetting("@bannedAttackedBlocks", Icon.cancel, () -> attacked.bannedAttackBlocksDialog.show(attacked.bannedAttackBlocks)));
        t.row();

        t.pref(new TitleSetting("@settingtitle.unitAttacked"));

        t.checkPref("enableUnitAttackFrag", false, u -> attacked.unitEnabled = u);
        t.sliderPref("unitHealthPercent", 50, 0, 100, 1, i -> i + "%");
        t.pref(new ButtonSetting("@editAttackedString", Icon.pencil, () -> unitStringDialog.show()));
        t.checkPref("bannedAttackedUnitsWhitelist", false, u -> attacked.unitWhitelist = u);
        t.pref(new ButtonSetting("@bannedAttackedUnits", Icon.cancel, () -> attacked.bannedAttackUnitsDialog.show(attacked.bannedAttackUnits)));
        t.row();

        t.pref(new GithubLink("Github"));
    };

    public void load(){
        blockStringDialog = new BaseDialog("@settings");
        blockStringDialog.buttons.defaults().size(210, 64);
        blockStringDialog.cont.table(t -> {
            t.field(settings.getString("blockStringMCS", Core.bundle.get("buildAttacked")), s -> attacked.tmpString = s).width(400f).center().padLeft(10f);
            t.button("@confirm", Icon.ok, () -> {
                attacked.blockString = attacked.tmpString;
                attacked.blockChanged = true;
                settings.put("blockStringMCS", attacked.blockString);
                blockStringDialog.hide();
            }).size(105f, 64f).padLeft(10f);
            t.button("@back", Icon.left, blockStringDialog::hide).size(105f, 64f);
        });
        blockStringDialog.addCloseListener();

        unitStringDialog = new BaseDialog("@settings");
        unitStringDialog.buttons.defaults().size(210, 64);
        unitStringDialog.cont.table(t -> {
            t.field(settings.getString("unitStringMCS", Core.bundle.get("unitAttacked")), s -> attacked.tmpString = s).width(400f).center().padLeft(10f);
            t.button("@confirm", Icon.ok, () -> {
                attacked.unitString = attacked.tmpString;
                attacked.unitEnabled = true;
                settings.put("unitStringMCS", attacked.unitString);
                unitStringDialog.hide();
            }).size(105f, 64f).padLeft(10f);
            t.button("@back", Icon.left, unitStringDialog::hide).size(105f, 64f);
        });
        unitStringDialog.addCloseListener();

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

        musicListDialog = new BaseDialog("@musicList"){{
            onResize(() -> rebuildMusicList());
        }};
        musicListDialog.addCloseButton();

        musicSearchDialog = new musicSquareSearchDialog();
        musicSearchDialog.setup();

        try{
            ui.settings.addCategory(Core.bundle.get("morecustomsettings"), Icon.settings, settingBuilder);
            Seq<Music> a = new Seq<>(control.sound.ambientMusic), b = new Seq<>(control.sound.bossMusic), d = new Seq<>(control.sound.darkMusic);
            control.sound.ambientMusic.clear();
            control.sound.darkMusic.clear();
            control.sound.bossMusic.clear();
            control.sound = new CustomSoundControl(){{
                ambientMusic = a;
                bossMusic = b;
                darkMusic = d;
            }};
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
        if(settings.getBool("enablecustomcampaigndifficulty")){
            ui.campaignRules = new CustomCampaignRulesDialog();
            spawner = new CustomWaveSpawner();
        }
    }

    private void rebuildMusicList(){
        musicLoader.loadFolder();
        musicListDialog.cont.clearChildren();
        musicListDialog.cont.pane(t -> {
            t.add("@importMusic.ambient").color(Pal.accent).padTop(10).left().row();
            boolean found = false;
            for(var f : musicLoader.ambient.seq()){
                if(musicLoader.isMusic(f)){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(f::name).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            t.add("@importMusic.dark").color(Pal.accent).padTop(10).left().row();
            found = false;
            for(var f : musicLoader.dark.seq()){
                if(musicLoader.isMusic(f)){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(f::name).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();

            t.add("@importMusic.boss").color(Pal.accent).padTop(10).left().row();
            found = false;
            for(var f : musicLoader.boss.seq()){
                if(musicLoader.isMusic(f)){
                    t.table(Styles.grayPanel, mt -> {
                        mt.labelWrap(f::name).left().fillX().expandX();
                        mt.button("@delete", Icon.trashSmall, () -> {
                            f.delete();
                            musicLoader.load();
                            rebuildMusicList();
                        }).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
            }
            if(!found) t.add("@musicList.empty").padLeft(10).left().row();
        }).width(Core.graphics.getWidth() / Scl.scl() * 0.75f).growY(); //.growX().growY();
    }

    public static class TitleSetting extends SettingsTable.Setting {
        public TitleSetting(String text) {
            super("");
            this.title = text;
        }

        public void add(SettingsTable table) {
            table.add(this.title).color(Pal.accent).padTop(25.0F).padRight(110.0F).padBottom(-5.0F).left().pad(5.0F);
            table.row();
            table.image().color(Pal.accent).height(3.0F).padRight(110.0F).padBottom(25.0F).left().fillX().padBottom(5.0F);
            table.row();
        }
    }
    public static class ButtonSetting extends SettingsTable.Setting{
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

    public static class GithubLink extends SettingsTable.Setting{
        public GithubLink(String name) { super(name); }

        @Override
        public void add(SettingsTable table) {
            table.add(new Table(t -> {
                t.button(Icon.github, new ImageButton.ImageButtonStyle(), () -> {
                    String url = "https://github.com/lu69159/MoreCustomSettings";
                    if (!Core.app.openURI(url)) {
                        ui.showInfoFade("@linkfail");
                        Core.app.setClipboardText(url);
                    }
                });
            })).row();
        }
    }
}

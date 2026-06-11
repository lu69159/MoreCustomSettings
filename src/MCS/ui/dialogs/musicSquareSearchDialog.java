package MCS.ui.dialogs;

import MCS.game.*;
import MCS.musicSquare.sources.*;
import MCS.musicSquare.sources.musics.*;
import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static MCS.main.*;
import static mindustry.Vars.*;

public class musicSquareSearchDialog extends BaseDialog {
    allResources resource =  new allResources();
    private Table resultTable;
    private TextField searchField;
    private String word = "";
    String previewingUrl = "";
    Image loadingSpinner;

    public musicSquareSearchDialog() {
        super("@musicSquare.search");
        addCloseButton();
        setFillParent(true);
        resource.onSearchBeginning = this::loading;
        resource.onSearchComplete = this::complete;
        hidden(() -> {
            if(!previewingUrl.isEmpty()) stopPreviewListening();
            searchField.setText("");
            word = "";
            previewingUrl = "";
            resource.allResults.clear();
            resultTable.clear();
            if(musicLoader.tmp.exists()){
                musicLoader.tmp.deleteDirectory();
                musicLoader.loadFolder();
            }
        });
    }

    public void setup() {
        cont.table(t -> {
            t.left();
            t.image(Icon.zoom).padRight(8f);
            searchField = t.field("", text -> word = text).growX().get();
            if(!mobile){
                searchField.keyDown(KeyCode.enter, () -> {
                    if(!word.isEmpty()){
                        resource.search(word);
                    }
                });
            }
            t.button("@searchMusic", Icon.zoom, () -> {
                if(!word.isEmpty()) resource.search(word);
            }).size(100f, 50f);
        }).fillX().padBottom(4).row();

        cont.pane(pane -> {
            pane.top().left();
            resultTable = pane;
        }).grow().row();

        buttons.defaults().size(210f,64f);
        //buttons.button("@musicSquare.loadMore", Icon.download, () -> resource.loadMore(10)); //TODO
    }

    private void loading(){
        resultTable.clear();
        resultTable.top().left();
        resultTable.add(Core.bundle.get("loading")).pad(20);
        loadingSpinner = resultTable.image(Icon.settings, Pal.accent).update(i -> {
            i.rotateBy(Time.delta * 360f / 20f);
        }).size(16f).padLeft(4f).get();
    }

    private void complete(){
        loadingSpinner = null;
        resultTable.clear();
        resultTable.top().left();
        if(resource.allResults.size == 0){
            resultTable.add(Core.bundle.get("musicList.empty")).pad(20);
        }else{
            for(var t : resource.allResults){
                trackShow(t);
            }
        }
    }

    private void trackShow(musicBase.Track t){
        resultTable.table(Styles.grayPanel, row -> {
            row.defaults().pad(4).left();

            Image cover = new Image();
            cover.setScaling(Scaling.fit);
            row.add(cover).size(48f);

            row.table(info -> {
                info.defaults().left();
                info.add(t.name != null ? t.name : "@unknown")
                    .color(Pal.accent).growX().wrap();
                info.row();
                info.add(t.artist != null ? t.artist : "")
                    .color(Color.lightGray).growX().wrap();
            }).width(300f).growX().padLeft(8f);

            if(t.url != null && !t.url.isEmpty()){
                var style = new ImageButton.ImageButtonStyle(Styles.clearNonei);
                style.imageUp = Icon.play;
                style.imageChecked = Icon.pause;
                row.button(Icon.play, style, () -> {
                    if(t.url.equals(previewingUrl)){
                        stopPreviewListening();
                    }else{
                        trackPreviewListening(t);
                    }
                }).update(i -> i.setChecked(t.url.equals(previewingUrl))).size(38f);
                row.button(Icon.download, Styles.clearNonei, () -> {
                    trackDownload(t);
                }).size(38f);
            }

            if(t.pic != null && !t.pic.isEmpty()){
                Http.get(t.pic, res -> {
                    try{
                        byte[] data = res.getResult();
                        Core.app.post(() -> {
                            try{
                                Pixmap pix = new Pixmap(data);
                                Texture tex = new Texture(pix);
                                pix.dispose();
                                cover.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
                            }catch(Exception ignored){}
                        });
                    }catch(Exception e){
                        cover.setDrawable(Core.atlas.find("error"));
                    }
                }, e -> {});
            }
        }).growX().pad(2).row();
    }

    private void trackPreviewListening(musicBase.Track t){
        if(t.url == null || t.url.isEmpty()) return;
        previewingUrl = t.url;
        musicLoader.loadFolder();
        musicLoader.tmp.mkdirs();

        Http.get(t.url, res -> {
            try{
                byte[] data = res.getResult();

                String ext = "mp3";
                String base = t.url.split("\\?")[0];
                int dot = base.lastIndexOf('.');
                if(dot > 0){
                    String e = base.substring(dot + 1).toLowerCase();
                    if(e.equals("ogg") || e.equals("mp3") || e.equals("flac") || e.equals("wav")){
                        ext = e;
                    }
                }

                Fi file = musicLoader.tmp.child("preview." + ext);
                file.writeBytes(data);

                Core.app.post(() -> {
                    try{
                        Music music = new Music(file){
                            @Override
                            public void setLooping(boolean isLooping){}
                        };
                        ((CustomSoundControl)control.sound).playPreView(music);
                    }catch(Exception ex){
                        ui.showException(ex);
                    }
                });
            }catch(Exception e){
                Core.app.post(() -> {
                    previewingUrl = "";
                    ui.showException(e);
                });
            }
        }, error -> Core.app.post(() -> {
            previewingUrl = "";
            ui.showInfo("@musicSquare.noAudio");
        }));
    }

    private void stopPreviewListening(){
        ((CustomSoundControl)control.sound).stopPreView();
        previewingUrl = "";
    }

    private void trackDownload(musicBase.Track t){
        BaseDialog dialog = new BaseDialog("@musicSquare.selectCategory");
        dialog.addCloseButton();
        dialog.cont.table(Tex.button, bt -> {
            bt.defaults().size(200f, 60f).left();
            bt.button("@importMusic.ambient", Styles.flatt, () -> {
                dialog.hide();
                t.download(musicLoader.ambient);
            });
            bt.row();
            bt.button("@importMusic.dark", Styles.flatt, () -> {
                dialog.hide();
                t.download(musicLoader.dark);
            });
            bt.row();
            bt.button("@importMusic.boss", Styles.flatt, () -> {
                dialog.hide();
                t.download(musicLoader.boss);
            });
            bt.row();
            bt.button("@importMusic.menu", Styles.flatt, () -> {
                dialog.hide();
                downloadNamed(t, "menu");
            });
            bt.row();
            bt.button("@importMusic.editor", Styles.flatt, () -> {
                dialog.hide();
                downloadNamed(t, "editor");
            });
        });
        dialog.show();
    }

    private void downloadNamed(musicBase.Track t, String name){
        if(t.url == null || t.url.isEmpty()) return;

        ui.loadfrag.show(Core.bundle.get("musicSquare.downloading"));

        Http.get(t.url, res -> {
            try{
                byte[] data = res.getResult();

                String ext = "mp3";
                String base = t.url.split("\\?")[0];
                int dot = base.lastIndexOf('.');
                if(dot > 0){
                    String e = base.substring(dot + 1).toLowerCase();
                    if(e.equals("ogg") || e.equals("mp3") || e.equals("flac") || e.equals("wav")){
                        ext = e;
                    }
                }

                musicLoader.loadFolder();
                for(var f : musicLoader.musicFolder.seq()){
                    if(f.name().split("__", 2)[0].equals(name)) f.delete();
                }
                musicLoader.musicFolder.child(name + "__" + data.length + "." + ext).writeBytes(data);

                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    musicLoader.load();
                    ui.showInfo("@musicSquare.downloaded");
                });
            }catch(Throwable e){
                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    ui.showException(new Exception("Download failed", e));
                });
            }
        }, error -> Core.app.post(() -> {
            ui.loadfrag.hide();
            ui.showException(new Exception("Download error: " + error));
        }));
    }
}

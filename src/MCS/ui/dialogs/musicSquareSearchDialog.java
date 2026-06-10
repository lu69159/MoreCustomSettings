package MCS.ui.dialogs;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Http.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import MCS.musicSquare.sources.*;
import MCS.musicSquare.sources.musicBase.Track;

import static mindustry.Vars.*;
import static MCS.main.*;

public class musicSquareSearchDialog extends BaseDialog {
    private final allResources resource = new allResources();
    private final Seq<String> enabledSources = Seq.with("netease", "qq", "kuwo");
    private static final String[] SOURCE_NAMES = {"netease", "qq", "kuwo"};

    private Table resultTable;
    private TextField searchField;
    private TextButton[] sourceButtons = new TextButton[SOURCE_NAMES.length];

    private @Nullable Music previewMusic;
    private @Nullable Fi previewFile;
    private String currentPreviewUid;
    private String keyword = "";

    public musicSquareSearchDialog() {
        super("@musicSquare.search");
        addCloseButton();
        setFillParent(true);

        setupUI();

        resource.setOnResultsUpdate(results ->
            Core.app.post(() -> displayResults(results))
        );

        shown(() -> Core.app.post(() -> Core.scene.setKeyboardFocus(searchField)));
        hidden(this::stopPreview);
    }

    private void setupUI() {
        cont.table(Tex.button, top -> {
            top.defaults().pad(4);
            top.image(Icon.zoom).size(32f);

            TextField field = top.field("", text -> keyword = text).growX().pad(4).get();
            searchField = field;
            field.keyDown(KeyCode.enter, this::startSearch);

            top.button("@searchMusic", Icon.zoom, this::startSearch).size(100f, 50f);
        }).growX().pad(8).row();

        cont.table(src -> {
            src.defaults().pad(2);
            for (int i = 0; i < SOURCE_NAMES.length; i++) {
                int idx = i;
                sourceButtons[i] = src.button(SOURCE_NAMES[idx], Styles.flatTogglet, () -> {
                    String s = SOURCE_NAMES[idx];
                    if (enabledSources.contains(s)) {
                        enabledSources.remove(s);
                    } else {
                        enabledSources.add(s);
                    }
                }).update(b -> b.setChecked(enabledSources.contains(SOURCE_NAMES[idx])))
                  .size(90f, 38f).get();
            }
        }).growX().row();

        cont.pane(pane -> {
            pane.top().left();
            resultTable = pane;
        }).grow().row();

        buttons.defaults().size(180f, 50f);
        buttons.button("@musicSquare.loadMore", Icon.download, this::loadMore);
    }

    private void startSearch() {
        if (keyword == null || keyword.trim().isEmpty()) return;
        stopPreview();
        resource.search(keyword.trim(), 10, enabledSources);
        resultTable.clear();
        resultTable.add(Core.bundle.get("loading")).pad(20);
    }

    private void loadMore() {
        resource.loadMore(10, enabledSources);
        if (resultTable.getChildren().size == 0) {
            resultTable.add(Core.bundle.get("loading")).pad(20);
        }
    }

    private void displayResults(Seq<Track> tracks) {
        resultTable.clear();
        resultTable.top().left();

        if (tracks.isEmpty()) {
            resultTable.add("@none.found").color(Color.lightGray).pad(20);
            return;
        }

        for (Track t : tracks) {
            String uid = t.uid;
            resultTable.table(Styles.grayPanel, row -> {
                row.defaults().pad(4).left();

                row.add(t.source).color(getSourceColor(t.source)).width(45f).center();

                row.table(info -> {
                    info.defaults().left();
                    info.add(t.title != null && !t.title.isEmpty() ? t.title : "@unknown")
                        .color(Pal.accent).growX().wrap();
                    info.row();
                    String detail = t.artist != null && !t.artist.isEmpty() ? t.artist : "";
                    if (t.qualityLabel != null) {
                        detail += " [gray]" + t.qualityLabel + "[]";
                    }
                    info.add(detail).color(Color.lightGray).growX().wrap();
                }).width(300f).growX();

                boolean isPlaying = uid.equals(currentPreviewUid) && previewMusic != null;
                row.button(isPlaying ? Icon.cancel : Icon.play, Styles.clearNonei, () -> {
                    if (isPlaying) {
                        stopPreview();
                    } else {
                        previewTrack(t);
                    }
                }).size(38f);

                row.button(Icon.download, Styles.clearNonei, () -> downloadTrack(t)).size(38f);
            }).growX().pad(2).row();
        }
    }

    private static Color getSourceColor(String source) {
        switch (source) {
            case "netease": return Color.red;
            case "qq": return Color.green;
            case "kuwo": return Color.orange;
            case "joox": return Color.purple;
            default: return Color.white;
        }
    }

    private void previewTrack(Track track) {
        stopPreview();

        if (!track.detailsLoaded || track.audioUrl == null) {
            showLoadingWhile(() -> {
                resource.fetchDetails(track);
                return track.audioUrl;
            }, url -> {
                if (url != null) {
                    doPreview(track);
                } else {
                    ui.showInfo("@musicSquare.noAudio");
                }
            });
            return;
        }
        doPreview(track);
    }

    private void doPreview(Track track) {
        if (track.audioUrl == null) return;

        ui.loadfrag.show(Core.bundle.get("musicSquare.downloading"));
        new Thread(() -> {
            try {
                String url = track.audioUrl;
                String ext = inferExt(url);

                HttpResponse res = blockingGet(url);
                if (res == null) throw new Exception("No response");

                byte[] data = res.getResult();
                Fi temp = tmpDirectory.child("preview_" + System.nanoTime() + "." + ext);
                temp.writeBytes(data);

                Music music = new Music(temp);
                music.setVolume(1f);
                music.play();

                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    previewFile = temp;
                    previewMusic = music;
                    currentPreviewUid = track.uid;
                });
            } catch (Exception e) {
                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    ui.showException(e);
                });
            }
        }, "MusicPreview").start();
    }

    private void stopPreview() {
        if (previewMusic != null) {
            previewMusic.stop();
            previewMusic.dispose();
            previewMusic = null;
        }
        if (previewFile != null) {
            previewFile.delete();
            previewFile = null;
        }
        currentPreviewUid = null;
    }

    private void downloadTrack(Track track) {
        BaseDialog catDialog = new BaseDialog("@musicSquare.selectCategory");
        catDialog.addCloseButton();
        catDialog.cont.table(Tex.button, t -> {
            t.defaults().size(220f, 60f);
            t.button("@importMusic.ambient", Styles.flatt, () -> {
                catDialog.hide();
                doDownload(track, "a");
            });
            t.row();
            t.button("@importMusic.dark", Styles.flatt, () -> {
                catDialog.hide();
                doDownload(track, "d");
            });
            t.row();
            t.button("@importMusic.boss", Styles.flatt, () -> {
                catDialog.hide();
                doDownload(track, "b");
            });
        });
        catDialog.show();
    }

    private void doDownload(Track track, String category) {
        if (!track.detailsLoaded || track.audioUrl == null) {
            showLoadingWhile(() -> {
                resource.fetchDetails(track);
                return track.audioUrl;
            }, url -> {
                if (url != null) {
                    downloadFile(track, category);
                } else {
                    ui.showInfo("@musicSquare.noAudio");
                }
            });
            return;
        }
        downloadFile(track, category);
    }

    private void downloadFile(Track track, String category) {
        ui.loadfrag.show(Core.bundle.get("musicSquare.downloading"));
        new Thread(() -> {
            try {
                String url = track.audioUrl;
                if (url == null) throw new Exception("No audio URL");

                String ext = inferExt(url);

                HttpResponse res = blockingGet(url);
                if (res == null) throw new Exception("No response");

                byte[] data = res.getResult();

                musicLoader.loadFolder();
                Fi targetDir;
                switch (category) {
                    case "d": targetDir = musicLoader.dark; break;
                    case "b": targetDir = musicLoader.boss; break;
                    default: targetDir = musicLoader.ambient;
                }

                String safeName = (track.artist + "-" + track.title)
                    .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_")
                    .replaceAll("_+", "_");
                if (safeName.startsWith("_")) safeName = safeName.substring(1);
                if (safeName.endsWith("_")) safeName = safeName.substring(0, safeName.length() - 1);
                if (safeName.isEmpty()) safeName = "music_" + System.nanoTime();

                String fileName = safeName + "__" + data.length + "." + ext;
                Fi target = targetDir.child(fileName);
                target.writeBytes(data);

                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    musicLoader.load();
                    ui.showInfo("@musicSquare.downloaded");
                });
            } catch (Exception e) {
                Core.app.post(() -> {
                    ui.loadfrag.hide();
                    ui.showException(e);
                });
            }
        }, "MusicDownload").start();
    }

    private static void showLoadingWhile(final Prov<Object> task, final Cons<Object> callback) {
        BaseDialog load = new BaseDialog("");
        load.cont.add(Core.bundle.get("loading"));
        load.show();
        new Thread(() -> {
            Object result = task.get();
            Core.app.post(() -> {
                load.hide();
                callback.get(result);
            });
        }, "FetchDetails").start();
    }

    private static String inferExt(String url) {
        if (url == null) return "mp3";
        String base = url.split("\\?")[0];
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            String ext = base.substring(dot + 1).toLowerCase();
            if (ext.equals("ogg") || ext.equals("mp3") || ext.equals("flac") || ext.equals("wav")) {
                return ext;
            }
        }
        return "mp3";
    }

    private static HttpResponse blockingGet(String url) {
        HttpResponse[] holder = new HttpResponse[1];
        Http.request(Http.HttpMethod.GET, url).timeout(30000).block(r -> holder[0] = r);
        return holder[0];
    }

    private interface Prov<T> {
        T get();
    }

    private interface Cons<T> {
        void get(T value);
    }
}

package MCS.musicSquare.sources;

import MCS.musicSquare.sources.musics.*;
import arc.*;
import arc.struct.Seq;
import arc.util.*;

public class allResources{
    public @Nullable Runnable onSearchBeginning, onSearchComplete;

    private final netEase netEase = new netEase();
    private final kuWo kuWo = new kuWo();
    private final qq qq = new qq();
    public Seq<musicBase> all = Seq.with(netEase, kuWo, qq);

    public Seq<musicBase.Track> allResults = new Seq<>();
    private String keyword;

    public void search(String kw){
        allResults.clear();
        this.keyword = kw;

        new Thread(() -> {
            Core.app.post(() -> {
                if(onSearchBeginning != null) onSearchBeginning.run();
            });
            for(var source : all){
                var results = source.search(kw);
                allResults.addAll(results);
            }
            Core.app.post(() -> {
                if(onSearchComplete != null) onSearchComplete.run();
            });
        }, "MusicSearch").start();
    }

    public void loadMore(int limit){ //TODO: 目前点击包是无事发生
        search(keyword);
    }
}

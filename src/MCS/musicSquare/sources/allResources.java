package MCS.musicSquare.sources;

import MCS.musicSquare.sources.musics.*;
import arc.*;
import arc.struct.Seq;
import arc.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class allResources{
    public @Nullable Runnable onSearchBeginning, onSearchComplete;

    private final netEase netEase = new netEase();
    //private final kuWo kuWo = new kuWo(); 关闭
    private final kuGou kuGou = new kuGou();
    private final qq qq = new qq();
    public final Seq<musicBase> all = Seq.with(netEase, kuGou, qq);

    private final ExecutorService searchExecutor = Threads.executor("MusicSearch", all.size);//用于并发查询
    private int searchId;

    public Seq<musicBase> enabled = Seq.with();
    public Seq<musicBase.Track> allResults = new Seq<>();

    String keyword;
    //int limit; TODO: 用于LOADMORE?

    public void search(String kw){
        allResults.clear();
        this.keyword = kw;

        int requestId = ++searchId;
        // 主线程触发加载状态，并检查序号避免过期搜索覆盖当前界面状态。
        Core.app.post(() -> {
            if(requestId == searchId && onSearchBeginning != null) onSearchBeginning.run();
        });

        Seq<musicBase> sources = enabled.copy();
        if(sources.isEmpty()){
            Core.app.post(() -> {
                if(requestId == searchId && onSearchComplete != null) onSearchComplete.run();
            });
            return;
        }

        musicBase.Track[][] resultsBySource = new musicBase.Track[sources.size][];//resource[平台id][此平台查询结果数]
        AtomicInteger remaining = new AtomicInteger(sources.size);

        for(int i = 0; i < sources.size; i++){
            final int sourceIndex = i;
            final musicBase source = sources.get(i);
            searchExecutor.submit(() -> {
                try{
                    Seq<musicBase.Track> results = source.search(kw);
                    resultsBySource[sourceIndex] = results == null ? new musicBase.Track[0] : results.toArray(musicBase.Track.class);
                    if(results != null) Log.info(source.name + " had found " + results.size + " result(s)");
                }catch(Throwable error){
                    Log.err(source.name + " failed to search：" + error.getMessage());
                    resultsBySource[sourceIndex] = new musicBase.Track[0];
                }finally{
                    if(remaining.decrementAndGet() == 0){
                        Core.app.post(() -> {
                            if(requestId != searchId) return; //避免旧搜索覆盖新搜索(BUT搜索按钮已经阻止了多次搜索?)
                            allResults.clear();
                            for(musicBase.Track[] sourceResults : resultsBySource){
                                if(sourceResults != null) allResults.addAll(sourceResults);
                            }
                            if(onSearchComplete != null) onSearchComplete.run();
                        });
                    }
                }
            });
        }
    }
}

package MCS.musicSquare.sources;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Jval;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static mindustry.Vars.*;
import static MCS.musicSquare.sources.ProviderParser.*;

public class allResources{
    public @Nullable Runnable onSearchBeginning, onSearchComplete;
    public final Seq<Provider> all = Seq.with();

    private final ExecutorService searchExecutor;//用于并发查询
    private int searchId;

    public Seq<Provider> enabled = Seq.with();
    public Seq<Track> allResults = new Seq<>();

    String keyword;
    //int limit; TODO: 用于LOADMORE?

    public allResources(){
        var json = Jval.read(mods.getMod("mcs").root.child("resources.json").readString("UTF-8")).asArray();
        for(var j : json){
            all.add(parse(j));
        }
        searchExecutor = Threads.executor("MusicSearch", all.size);
        for(var base : all){
            if(Core.settings.getBool("MCS-resource-" + base.name, true)) enabled.add(base);
        }
    }

    public void search(String kw, int limit){
        allResults.clear();
        this.keyword = kw;

        int requestId = ++searchId;
        // 主线程触发加载状态，并检查序号避免过期搜索覆盖当前界面状态。
        Core.app.post(() -> {
            if(requestId == searchId && onSearchBeginning != null) onSearchBeginning.run();
        });

        Seq<Provider> sources = enabled.copy();
        if(sources.isEmpty()){
            Core.app.post(() -> {
                if(requestId == searchId && onSearchComplete != null) onSearchComplete.run();
            });
            return;
        }

        Track[][] resultsBySource = new Track[sources.size][];//resource[平台id][此平台查询结果数]
        AtomicInteger remaining = new AtomicInteger(sources.size);

        for(int i = 0; i < sources.size; i++){
            final int sourceIndex = i;
            final Provider source = sources.get(i);
            searchExecutor.submit(() -> {
                try{
                    Seq<Track> results = source.search(kw, limit);
                    resultsBySource[sourceIndex] = results == null ? new Track[0] : results.toArray(Track.class);
                    if(results != null) Log.info(source.name + " had found " + results.size + " result(s)");
                }catch(Throwable error){
                    Log.err(source.name + " failed to search：" + error.getMessage());
                    resultsBySource[sourceIndex] = new Track[0];
                }finally{
                    if(remaining.decrementAndGet() == 0){
                        Core.app.post(() -> {
                            if(requestId != searchId) return; //避免旧搜索覆盖新搜索(BUT搜索按钮已经阻止了多次搜索?)
                            allResults.clear();
                            for(Track[] sourceResults : resultsBySource){
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

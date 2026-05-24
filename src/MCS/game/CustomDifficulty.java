package MCS.game;

import arc.*;

public enum CustomDifficulty{
    casual(0.5f, 0.5f, 2f),
    easy(1f, 0.75f, 1.5f),
    normal(1f, 1f, 1f),
    hard(1.25f, 1.5f, 0.8f),
    eradication(1.5f, 2f, 0.6f),
    custom(1f, 1f, 1f, true);

    public static final CustomDifficulty[] all = values();

    //TODO add more fields
    public float enemyHealthMultiplier, enemySpawnMultiplier, waveTimeMultiplier;
    public boolean isCustom;

    CustomDifficulty(float enemyHealthMultiplier, float enemySpawnMultiplier, float waveTimeMultiplier){
        this.enemySpawnMultiplier = enemySpawnMultiplier;
        this.waveTimeMultiplier = waveTimeMultiplier;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        isCustom = false;
    }

    CustomDifficulty(float enemyHealthMultiplier, float enemySpawnMultiplier, float waveTimeMultiplier, boolean isCustom){
        this.enemySpawnMultiplier = enemySpawnMultiplier;
        this.waveTimeMultiplier = waveTimeMultiplier;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.isCustom = isCustom;
    }

    public String info(){
        if(isCustom) return "[gray]" + Core.bundle.get("difficulty.custom");
        String res =
                (enemyHealthMultiplier == 1f ? "" : Core.bundle.format("difficulty.enemyHealthMultiplier", percentStat(enemyHealthMultiplier)) + "\n") +
                        (enemySpawnMultiplier == 1f ? "" : Core.bundle.format("difficulty.enemySpawnMultiplier", percentStat(enemySpawnMultiplier)) + "\n") +
                        (waveTimeMultiplier == 1f ? "" : Core.bundle.format("difficulty.waveTimeMultiplier", percentStatNeg(waveTimeMultiplier)) + "\n");

        return res.isEmpty() ? Core.bundle.get("difficulty.nomodifiers") : res;
    }

    public String localized(){
        return Core.bundle.get("difficulty." + name());
    }

    static String percentStat(float val){
        return ((int)(val * 100 - 100) > 0 ? "[negstat]+" : "[stat]") + (int)(val * 100 - 100) + "%[]";
    }

    static String percentStatNeg(float val){
        return ((int)(val * 100 - 100) > 0 ? "[stat]+" : "[negstat]") + (int)(val * 100 - 100) + "%[]";
    }
}

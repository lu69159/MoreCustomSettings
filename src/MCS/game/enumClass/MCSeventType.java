package MCS.game.enumClass;

import arc.audio.Music;
import arc.util.Nullable;

public class MCSeventType{
    public static class MusicBarChangeEvent {
        public boolean enabled;
        public MusicBarChangeEvent(boolean enabled){
            this.enabled = enabled;
        }
    }
    public static class CustomMusicChangeEvent{
        public boolean enabled;
        public CustomMusicChangeEvent(boolean enabled){
            this.enabled = enabled;
        }
    }
    public static class ContentManagerChangeEvent {
        public boolean enabled;
        public ContentManagerChangeEvent(boolean enabled){
            this.enabled = enabled;
        }
    }
    public static class MusicImportDialogShowEvent{
        public @Nullable Music from;
        public boolean isCopied;
        public MusicImportDialogShowEvent(){
            this(null, true);
        }
        public MusicImportDialogShowEvent(@Nullable Music from, boolean isCopied){
            this.from = from;
            this.isCopied = isCopied;
        }
    }
    public static class ImportMusicEvent{
        @Nullable public Music music;
        public String musicFi;
        public boolean isCopied;

        public ImportMusicEvent(Music music, String musicFi, boolean isCopied){
            this.music = music;
            this.musicFi = musicFi;
            this.isCopied = isCopied;
        }
    }
    public static class ImportNamedMusicEvent{
        @Nullable public Music music;
        public String inputName;
        public boolean isCopied;

        public ImportNamedMusicEvent(Music music, String inputName, boolean isCopied){
            this.music = music;
            this.inputName = inputName;
            this.isCopied = isCopied;
        }
    }
}

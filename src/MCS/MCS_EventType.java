package MCS;

import arc.audio.Music;
import arc.util.Nullable;

public class MCS_EventType {
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
        public boolean isImported;

        public MusicImportDialogShowEvent(){
            this(null, true, true);
        }
        public MusicImportDialogShowEvent(@Nullable Music from, boolean isImported, boolean isCopied){
            this.from = from;
            this.isImported = isImported;
            this.isCopied = isCopied;
        }
    }
    public static class ImportMusicEvent{
        public String name;

        public ImportMusicEvent(String name){
            this.name = name;
        }
    }
    public static class MoveMusicEvent {
        @Nullable public Music music;
        public String name;
        public boolean isCopied;

        public MoveMusicEvent(Music music, String name, boolean isCopied){
            this.music = music;
            this.name = name;
            this.isCopied = isCopied;
        }
    }

    public static class RebuildMusicListEvent{}
    public static class RebuildMusicBarEvent{}
}

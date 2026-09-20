package MCS.game.enumClass;

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
}

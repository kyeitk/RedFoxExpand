package redfoxexpand.reactive.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Definition-scoped property animation, distinct from Sprite texture-frame animation. */
public final class PropertyAnimation {
    private final String id;
    private final long durationMillis;
    private final boolean loop;
    private final List<PropertyTrack> tracks;

    public PropertyAnimation(String id, long durationMillis, boolean loop, List<PropertyTrack> tracks) {
        if (durationMillis <= 0L) throw new IllegalArgumentException("animation duration must be positive");
        this.id = id;
        this.durationMillis = durationMillis;
        this.loop = loop;
        this.tracks = Collections.unmodifiableList(new ArrayList<PropertyTrack>(tracks));
    }

    public String getId() { return id; }
    public long getDurationMillis() { return durationMillis; }
    public boolean isLoop() { return loop; }
    public List<PropertyTrack> getTracks() { return tracks; }
}


package redfoxexpand.reactive.animation;

import redfoxexpand.reactive.ReactiveLimits;
import redfoxexpand.reactive.binding.ReactiveProperty;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded per-screen property-animation instance controller. */
public final class AnimationController {
    private final Map<Key, Running> running = new LinkedHashMap<Key, Running>();

    public boolean play(String target, PropertyAnimation animation, long nowMillis, boolean restart) {
        prune(nowMillis);
        Key key = new Key(target, animation.getId());
        Running existing = running.get(key);
        if (existing != null && isRunning(existing, nowMillis) && !restart) return false;
        if (existing == null && running.size() >= ReactiveLimits.MAX_ACTIVE_ANIMATIONS_PER_SCREEN) {
            throw new IllegalStateException("active animation budget exceeded");
        }
        running.remove(key);
        running.put(key, new Running(animation, nowMillis));
        return true;
    }

    public void stop(String target, String animationId) {
        running.remove(new Key(target, animationId));
    }

    public AnimationProperties evaluate(String target, long nowMillis) {
        java.util.List<AnimationProperties.Contribution> contributions =
                new java.util.ArrayList<AnimationProperties.Contribution>();
        Iterator<Map.Entry<Key, Running>> iterator = running.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Running> entry = iterator.next();
            Running instance = entry.getValue();
            long elapsed = Math.max(0L, nowMillis - instance.startedAtMillis);
            PropertyAnimation animation = instance.animation;
            if (!animation.isLoop() && elapsed > animation.getDurationMillis()) {
                iterator.remove();
                continue;
            }
            if (!entry.getKey().target.equals(target)) continue;
            long position = animation.isLoop()
                    ? elapsed % animation.getDurationMillis()
                    : Math.min(elapsed, animation.getDurationMillis());
            for (PropertyTrack track : animation.getTracks()) {
                double value = track.sample(position);
                contributions.add(new AnimationProperties.Contribution(
                        track.getProperty(), track.getComposition(), value));
            }
        }
        return AnimationProperties.of(contributions);
    }

    public boolean isRunning(String target, String animationId, long nowMillis) {
        Running instance = running.get(new Key(target, animationId));
        return instance != null && isRunning(instance, nowMillis);
    }

    public int size() {
        return running.size();
    }

    public void clear() {
        running.clear();
    }

    private void prune(long nowMillis) {
        Iterator<Map.Entry<Key, Running>> iterator = running.entrySet().iterator();
        while (iterator.hasNext()) {
            Running value = iterator.next().getValue();
            if (!isRunning(value, nowMillis)) iterator.remove();
        }
    }

    private static boolean isRunning(Running value, long nowMillis) {
        return value.animation.isLoop()
                || Math.max(0L, nowMillis - value.startedAtMillis) <= value.animation.getDurationMillis();
    }

    private static final class Running {
        final PropertyAnimation animation;
        final long startedAtMillis;

        Running(PropertyAnimation animation, long startedAtMillis) {
            this.animation = animation;
            this.startedAtMillis = startedAtMillis;
        }
    }

    private static final class Key {
        final String target;
        final String animation;

        Key(String target, String animation) {
            this.target = target;
            this.animation = animation;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key key = (Key) other;
            return target.equals(key.target) && animation.equals(key.animation);
        }

        @Override
        public int hashCode() {
            return 31 * target.hashCode() + animation.hashCode();
        }
    }
}


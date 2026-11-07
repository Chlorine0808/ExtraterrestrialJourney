package chlorine.etjourney.content.craft;

/** Nothing stands between the device and the sky. */
public final class SkyCondition implements ICondition {

    @Override
    public boolean isMet(ConditionContext context) {
        return context.canSeeSky;
    }
}

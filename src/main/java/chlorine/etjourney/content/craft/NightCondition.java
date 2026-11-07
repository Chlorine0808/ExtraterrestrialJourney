package chlorine.etjourney.content.craft;

/** It is night by the world's clock, only where the sky turns from day to night. */
public final class NightCondition implements ICondition {

    @Override
    public boolean isMet(ConditionContext context) {
        return context.isNight();
    }
}
